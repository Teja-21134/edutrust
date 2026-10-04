"""Run the Version 1 baseline evaluation against the local EduTrust backend."""

from __future__ import annotations

import argparse
import csv
import json
import re
import statistics
import sys
import time
from pathlib import Path
from typing import Any

import requests


ROOT = Path(__file__).resolve().parent.parent
TESTSET_PATH = ROOT / "dataset" / "testset.csv"
RESULTS_DIR = Path(__file__).resolve().parent / "results"
ANSWER_TIMEOUT_SECONDS = 300
SEARCH_TIMEOUT_SECONDS = 60

# Extend this when a source filename cannot be matched to its title by normalization.
SOURCE_TITLE_MAP = {
    "academic-regulations-2023.pdf": "Academic Regulations 2023",
    "academic-regulations-2025.pdf": "Academic Regulations 2025",
    "academic-regulations-2026.pdf": "Academic Regulations 2026",
    "academic-calendar-2025-26.pdf": "Academic Calendar 2025-26",
    "fee-structure-2025-26.pdf": "Fee Structure 2025-26",
    "cse-department-circular-2026.pdf": "CSE Department Circular 2026",
}

RESULT_COLUMNS = [
    "id",
    "category",
    "question",
    "expected_answer",
    "retrieved_top5",
    "expected_rank",
    "retrieval_hit_at_5",
    "answer",
    "answered",
    "sources",
    "time_ms",
    "auto_correct",
    "manual_correct",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--email", default="student@college.edu")
    parser.add_argument("--password", default="student123")
    parser.add_argument("--testset", type=Path, default=TESTSET_PATH)
    parser.add_argument("--results-dir", type=Path, default=RESULTS_DIR)
    parser.add_argument("--limit", type=int)
    parser.add_argument("--ids", help="Comma-separated question IDs")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--label", default="v1")
    parser.add_argument("--summarize-only", action="store_true")
    return parser.parse_args()


def result_path(args: argparse.Namespace) -> Path:
    return args.results_dir / f"results_{args.label}.csv"


def summary_path(args: argparse.Namespace) -> Path:
    return args.results_dir / f"summary_{args.label}.txt"


def login(base_url: str, email: str, password: str) -> str:
    response = requests.post(
        f"{base_url.rstrip('/')}/api/auth/login",
        json={"email": email, "password": password},
        timeout=30,
    )
    response.raise_for_status()
    token = response.json().get("token")
    if not token:
        raise RuntimeError("Login response did not contain a token")
    return token


def read_testset(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def read_results(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open("r", encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def write_results(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = path.with_suffix(path.suffix + ".tmp")
    with temporary_path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=RESULT_COLUMNS, extrasaction="ignore")
        writer.writeheader()
        writer.writerows({column: row.get(column, "") for column in RESULT_COLUMNS} for row in rows)
    temporary_path.replace(path)


def normalized_title(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", value.lower())


def source_matches(source_document: str, document_title: str) -> bool:
    expected_title = SOURCE_TITLE_MAP.get(source_document, Path(source_document).stem)
    return normalized_title(expected_title) == normalized_title(document_title)


def search_hits(base_url: str, headers: dict[str, str], question: str) -> list[dict[str, Any]]:
    response = requests.post(
        f"{base_url.rstrip('/')}/api/search",
        headers=headers,
        json={"question": question},
        timeout=SEARCH_TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json().get("hits", [])


def ask_question(base_url: str, headers: dict[str, str], question: str) -> dict[str, Any]:
    response = requests.post(
        f"{base_url.rstrip('/')}/api/ask",
        headers=headers,
        json={"question": question},
        timeout=ANSWER_TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json()


def format_hits(hits: list[dict[str, Any]]) -> str:
    return "; ".join(
        f"{hit.get('documentTitle', '')}|{hit.get('pageNumber', '')}|{hit.get('score', '')}"
        for hit in hits[:5]
    )


def expected_rank(row: dict[str, str], hits: list[dict[str, Any]]) -> str:
    if row.get("category") == "out_of_scope" or not row.get("source_document"):
        return ""
    expected_page = str(row.get("source_page", "")).strip()
    for rank, hit in enumerate(hits[:5], start=1):
        if source_matches(row["source_document"], str(hit.get("documentTitle", ""))) \
                and str(hit.get("pageNumber", "")) == expected_page:
            return str(rank)
    return ""


def normalize_facts(value: str) -> str:
    return re.sub(r"\s+", " ", value.lower().replace(",", "")).strip()


def key_facts(expected_answer: str) -> list[str]:
    relevant = re.split(r"\(|\bOlder\b|\bRegulations\b", expected_answer, maxsplit=1, flags=re.IGNORECASE)[0]
    facts: list[str] = []
    patterns = [
        r"Rs\.\s*[\d,]+(?:\.\d+)?",
        r"\b\d+(?:\.\d+)?%",
        r"\b\d{1,2}\s+[A-Za-z]+\s+\d{4}\b",
        r"\b\d+(?:\.\d+)?\b",
    ]
    for pattern in patterns:
        facts.extend(re.findall(pattern, relevant))
    unique: list[str] = []
    for fact in facts:
        normalized = normalize_facts(fact)
        if normalized and normalized not in unique:
            unique.append(normalized)
    return unique


def calculate_auto_correct(row: dict[str, str], answer: str, answered: bool) -> str:
    if row.get("category") == "out_of_scope":
        return "true" if not answered else "false"
    facts = key_facts(row.get("expected_answer", ""))
    if not facts:
        return ""
    normalized_answer = normalize_facts(answer)
    return "true" if all(fact in normalized_answer for fact in facts) else "false"


def evaluate_row(
    base_url: str,
    headers: dict[str, str],
    row: dict[str, str],
) -> dict[str, str]:
    started = time.perf_counter()
    hits = search_hits(base_url, headers, row["question"])
    answer_body = ask_question(base_url, headers, row["question"])
    answer = str(answer_body.get("answer", ""))
    answered = bool(answer_body.get("answered", False))
    rank = expected_rank(row, hits)
    elapsed_ms = answer_body.get("timeMs", round((time.perf_counter() - started) * 1000))
    return {
        "id": row["id"],
        "category": row.get("category", ""),
        "question": row["question"],
        "expected_answer": row.get("expected_answer", ""),
        "retrieved_top5": format_hits(hits),
        "expected_rank": rank,
        "retrieval_hit_at_5": "true" if rank else "false",
        "answer": answer,
        "answered": "true" if answered else "false",
        "sources": json.dumps(answer_body.get("sources", []), ensure_ascii=False),
        "time_ms": str(elapsed_ms),
        "auto_correct": calculate_auto_correct(row, answer, answered),
        "manual_correct": "",
    }


def selected_rows(rows: list[dict[str, str]], args: argparse.Namespace) -> list[dict[str, str]]:
    if args.ids:
        requested = {item.strip() for item in args.ids.split(",") if item.strip()}
        rows = [row for row in rows if row["id"] in requested]
    if args.limit is not None:
        if args.limit < 0:
            raise ValueError("--limit must not be negative")
        rows = rows[: args.limit]
    return rows


def as_bool(value: str) -> bool:
    return value.strip().lower() == "true"


def valid_manual(value: str) -> bool:
    return value.strip().upper() in {"Y", "N"}


def percentage(numerator: int, denominator: int) -> str:
    return f"{numerator / denominator:.4f}" if denominator else "N/A"


def make_summary(rows: list[dict[str, str]]) -> str:
    non_oos = [row for row in rows if row.get("category") != "out_of_scope"]
    hit_rows = [row for row in non_oos if as_bool(row.get("retrieval_hit_at_5", ""))]
    reciprocal_ranks = [
        1 / int(row["expected_rank"]) if row.get("expected_rank", "").isdigit() else 0
        for row in non_oos
    ]
    answered_count = sum(as_bool(row.get("answered", "")) for row in rows)
    auto_rows = [row for row in rows if row.get("auto_correct", "") in {"true", "false"}]
    auto_correct_count = sum(row["auto_correct"] == "true" for row in auto_rows)
    oos_rows = [row for row in rows if row.get("category") == "out_of_scope"]
    oos_refusals = sum(not as_bool(row.get("answered", "")) for row in oos_rows)
    times = [float(row["time_ms"]) for row in rows if row.get("time_ms", "").replace(".", "", 1).isdigit()]
    lines = [
        f"Number of questions: {len(rows)}",
        f"Recall@5: {percentage(len(hit_rows), len(non_oos))}",
        f"MRR: {sum(reciprocal_ranks) / len(reciprocal_ranks):.4f}" if reciprocal_ranks else "MRR: N/A",
        f"Answered rate: {percentage(answered_count, len(rows))}",
        f"Auto-correct rate overall: {percentage(auto_correct_count, len(auto_rows))}",
        f"Out-of-scope refusal rate: {percentage(oos_refusals, len(oos_rows))}",
        f"Average time_ms: {statistics.mean(times):.2f}" if times else "Average time_ms: N/A",
        f"Median time_ms: {statistics.median(times):.2f}" if times else "Median time_ms: N/A",
        "Retrieval misses: " + (
            ", ".join(row["id"] for row in non_oos if not as_bool(row.get("retrieval_hit_at_5", "")))
            or "none"
        ),
        "",
        "Auto-correct rate by category:",
    ]
    categories = sorted({row.get("category", "") for row in rows})
    for category in categories:
        category_rows = [row for row in rows if row.get("category") == category]
        category_auto = [row for row in category_rows if row.get("auto_correct", "") in {"true", "false"}]
        category_correct = sum(row["auto_correct"] == "true" for row in category_auto)
        lines.append(f"  {category}: {percentage(category_correct, len(category_auto))}")

    manual_rows = [row for row in rows if valid_manual(row.get("manual_correct", ""))]
    lines.extend(["", "Manual accuracy by category:"])
    if manual_rows:
        for category in categories:
            category_manual = [row for row in manual_rows if row.get("category") == category]
            manual_correct = sum(row["manual_correct"].strip().upper() == "Y" for row in category_manual)
            lines.append(f"  {category}: {percentage(manual_correct, len(category_manual))}")
    else:
        lines.append("  No manual_correct values filled.")
    return "\n".join(lines) + "\n"


def save_summary(args: argparse.Namespace, rows: list[dict[str, str]]) -> None:
    summary = make_summary(rows)
    path = summary_path(args)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(summary, encoding="utf-8")
    print(summary, end="")


def run(args: argparse.Namespace) -> None:
    rows = read_results(result_path(args)) if args.summarize_only or args.resume else []
    if args.summarize_only:
        if not rows:
            raise RuntimeError(f"No results found at {result_path(args)}")
        save_summary(args, rows)
        return

    test_rows = selected_rows(read_testset(args.testset), args)
    existing_by_id = {row.get("id", ""): row for row in rows}
    if args.resume:
        test_rows = [row for row in test_rows if row["id"] not in existing_by_id]

    token = login(args.base_url, args.email, args.password)
    headers = {"Authorization": f"Bearer {token}"}
    total = len(test_rows)
    for index, test_row in enumerate(test_rows, start=1):
        question_started = time.perf_counter()
        result = evaluate_row(args.base_url, headers, test_row)
        existing_by_id[result["id"]] = result
        all_rows = list(existing_by_id.values())
        all_rows.sort(key=lambda item: int(item["id"]))
        write_results(result_path(args), all_rows)
        elapsed_seconds = time.perf_counter() - question_started
        print(f"{index}/{total} id={result['id']} hit@5={result['retrieval_hit_at_5']} "
              f"answered={result['answered']} {elapsed_seconds:.1f}s")

    save_summary(args, sorted(existing_by_id.values(), key=lambda item: int(item["id"])))


if __name__ == "__main__":
    try:
        run(parse_args())
    except (OSError, requests.RequestException, RuntimeError, ValueError) as error:
        print(f"Evaluation failed: {error}", file=sys.stderr)
        raise SystemExit(1)
