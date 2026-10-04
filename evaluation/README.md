# Version 1 baseline evaluation

This evaluator calls the local `/api/search` and `/api/ask` endpoints for every row in
`dataset/testset.csv`. It uses only Python 3.12 and the `requests` package.

## Windows setup

From the repository root in PowerShell:

```powershell
py -3.12 -m venv evaluation\.venv
evaluation\.venv\Scripts\Activate.ps1
python -m pip install -r evaluation\requirements.txt
```

Start PostgreSQL, the backend, and Ollama first. Then run the full baseline:

```powershell
python evaluation\run_eval.py --label v1
```

Useful options:

```text
--base-url URL       Backend URL (default: http://localhost:8080)
--email EMAIL        Login email (default: student@college.edu)
--password PASSWORD  Login password (default: student123)
--limit N            Evaluate only the first N selected questions
--ids 1,2,5          Evaluate only these question IDs
--resume             Skip IDs already present in the result CSV
--label NAME         Write results_<NAME>.csv and summary_<NAME>.txt
--summarize-only     Recompute the summary without calling the backend
```

For example:

```powershell
python evaluation\run_eval.py --ids 1,2,3 --label v1_small
python evaluation\run_eval.py --label v1 --resume
python evaluation\run_eval.py --label v1 --summarize-only
```

Results are written after every question to `evaluation/results/results_<label>.csv`.
The summary is printed and saved as `evaluation/results/summary_<label>.txt`.

## Metrics and heuristic

`Recall@5` counts a non-out-of-scope question as a retrieval hit when the expected
document and page occur in the first five vector-search hits. `MRR` averages the
reciprocal rank, using zero when the expected source is absent. The script also
reports answer rate, response time, refusal rate, and retrieval-miss IDs.

`auto_correct` is a rough guide only. For ordinary questions, it extracts percentages,
money amounts beginning with `Rs.`, dates, and numbers from the expected answer before
the first `(`, `Older`, or `Regulations`. Every extracted fact must occur in the answer
after comma and whitespace normalization. For out-of-scope questions, it is `true` only
when the system returns `answered=false`. Humans should fill `manual_correct` with `Y`
or `N`; `--summarize-only` reports manual accuracy by category when values are present.

Generated result CSVs and summaries are intentionally kept under version control for
the baseline report. Only `evaluation/.venv` is ignored.
