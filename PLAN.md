# EduTrust — Implementation Plan (3 Reviews)

**EduTrust = Faithfulness-Aware Hybrid RAG for Educational Institutions**
Runs 100% locally, 100% free tools. Demo is shown from the developer's laptop.

Pipeline (final, Review 3):
Query → Query Processing → Hybrid Retrieval (vector + keyword) → Metadata/Temporal Filtering → Reranking → Conflict/Authority Check → LLM (Ollama) → Faithfulness Verification → Citation → Final Answer OR Safe Fallback

Novelty = reliability-oriented pipeline + evaluation on institutional data (not a new foundation algorithm).

---

## 0. Software to install (all free)

### Install before Review 1
| Tool | Purpose | Verify with |
|---|---|---|
| JDK 21 (Temurin/OpenJDK) | Spring Boot | `java -version` |
| Maven | Build | `mvn -version` |
| IntelliJ IDEA Community | Java IDE | — |
| Node.js LTS | React | `node -v` |
| VS Code | React editing | — |
| Git + GitHub account | Version control | `git --version` |
| Docker Desktop (WSL2 on Windows) | PostgreSQL + pgvector | `docker --version` |
| Ollama | Local LLM runtime | `ollama --version` |
| Postman | API testing | — |
| Codex (already available) | AI coding | — |

### Install before Review 2
| Tool | Purpose | Verify with |
|---|---|---|
| Python 3.10+ | Reranker service + evaluation scripts | `python --version` |
| pip packages: `fastapi uvicorn sentence-transformers pandas matplotlib pytest` | Reranker, evaluation, graphs | `pip list` |
| DBeaver (optional) | Inspect PostgreSQL | — |

### Models
- LLM via Ollama. Pick by laptop RAM:
  - 8 GB RAM: a 1B–3B model (e.g. `ollama pull llama3.2:3b` or a small Qwen/Gemma)
  - 16 GB RAM: a 7B–8B quantized model is possible
  - Test with `ollama run <model>` before starting.
- Embeddings: BAAI/bge-small-en-v1.5 (bundled in-process via LangChain4j ONNX artifact; no separate install)
- Reranker: BAAI/bge-reranker-base (downloads automatically from Hugging Face on first run of the Python service)

### Libraries (added by Maven/npm inside the project, NOT installed manually)
- Maven: spring-boot-starter-web, -security, -data-jpa, validation, PostgreSQL driver, JWT library (jjwt), LangChain4j (core, ollama, pgvector, bge-small-en-v15 embeddings), Apache PDFBox, Lombok (optional), JUnit
- npm: react, vite, tailwindcss, axios, react-router-dom

### Docker image
- `pgvector/pgvector:pg16` (PostgreSQL 16 with pgvector)

---

## 1. Repository structure

```
edutrust/
  AGENTS.md
  PLAN.md
  docker-compose.yml          # postgres+pgvector (later: reranker)
  backend/                    # Spring Boot
  frontend/                   # React + Vite
  reranker/                   # Python FastAPI (Review 2)
  dataset/
    documents/                # synthetic + public PDFs
    testset.csv               # question, expected_answer, source_doc, page, category
  evaluation/                 # Python scripts (Review 2+)
  docs/                       # architecture diagrams, report notes
```

Suggested backend packages: `ingestion`, `retrieval`, `filtering`, `rerank`, `conflict`, `generation`, `verification`, `citation`, `pipeline`, `security`, `api`, `config`.

---

## 2. Dataset plan (fictional college: "XYZ Institute of Technology")

Prepare 10–20 PDFs (5–30 pages each). Mix synthetic and real public regulations.

| Document | Purpose |
|---|---|
| Academic Regulations 2023 | Old version (attendance 75%) |
| Academic Regulations 2025 | Conflict/version test (attendance 80%) |
| Academic Regulations 2026 | Latest version, temporal filtering |
| Exam rules and malpractice policy | Normal Q&A |
| Academic calendar 2025-26 | Dates, tables |
| Fee structure and scholarship circular | Numbers, tables |
| Hostel rules | Different topic |
| CSE department circular | Department override test (e.g. 70%) |
| Placement policy | Extra topic |
| Leave and condonation policy | Confuses attendance retrieval |

Deliberate test traps: version conflict, department override, similar topics (student vs faculty attendance), unanswerable questions, table lookups, unresolvable same-year conflict.

Test set: 50–100 questions with expected answer and source page. Categories: simple lookup 60%, temporal/version 15%, conflict 10%, out-of-scope 15%.

---

## REVIEW 1 — Foundation + Basic RAG (Version 1)

**Goal:** upload a PDF, ask a question, get an answer with source page. End-to-end, working.

### Scope (implement)
1. Project setup: Spring Boot, React (Vite + Tailwind), `docker-compose.yml` with pgvector.
2. Database schema: `documents` (id, title, department, doc_type, academic_year, doc_date, version, authority, file_name) and `chunks` (id, document_id, page_number, text, embedding vector(384), metadata).
3. Auth: JWT login, roles ADMIN and STUDENT. Only ADMIN can upload.
4. Ingestion pipeline: validate file type + metadata → PDFBox extraction (page-aware) → cleaning (headers/footers/whitespace) → chunking (~500 tokens, ~50 overlap, keep page number) → BGE-small embeddings → store in pgvector.
5. Basic query flow: question → embedding → vector search top 5 → prompt with evidence → Ollama → answer.
6. Prompt rule: answer only from provided evidence; say so if evidence is insufficient.
7. Simple citation: document title + page number from chunk metadata.
8. React UI: login page, admin upload page (with metadata form), chat page showing answer + source.
9. Baseline test set of 20–25 questions.

### Not in Review 1
Hybrid search, filtering, reranking, conflict handling, faithfulness verification.

### Suggested Codex task order (one at a time)
1. Scaffold repo + docker-compose + Spring Boot skeleton + health endpoint.
2. DB schema/entities + pgvector setup.
3. JWT auth + roles.
4. PDF extraction + cleaning (with unit tests on a sample PDF).
5. Chunking (with tests).
6. Embedding service + storing chunks.
7. Vector search service.
8. Ollama generation service + prompt template.
9. `/api/ask` endpoint returning `{answer, sources[]}`.
10. React pages (login, upload, chat).
11. Script to run the 20–25 baseline questions and print results.

### Definition of done
- `docker compose up` starts the DB; backend + frontend start locally.
- Upload a PDF → chunks visible in DB.
- Ask "What is the minimum attendance?" → answer with document + page.
- Baseline results saved (`evaluation/results_v1.*`).

### Demo checklist
Live upload → ask → answer with source; show architecture diagram, dataset plan, baseline results.

---

## REVIEW 2 — Improved Retrieval (Version 2)

**Goal:** better retrieval, proven with numbers (Version 1 vs Version 2).

### Scope (implement)
1. Query processing: rule-based detection of topic, department, year (keywords/regex).
2. Keyword retrieval: PostgreSQL full-text search (tsvector + GIN index) on chunk text.
3. Hybrid retrieval: vector top 20 + keyword top 20 → merge with Reciprocal Rank Fusion → candidate set.
4. Metadata/temporal filtering: prefer latest academic year/version, match department and document type; configurable in `application.yml`.
5. Reranker: Python FastAPI service (`/rerank`) using BGE-reranker-base; Spring Boot calls it; top 20 → top 5.
6. Config flags to switch between Version 1 (vector only), Version 2 (hybrid + filter + rerank) so both can be evaluated.
7. Test set expanded to 50+ questions (add version/year and out-of-scope questions).
8. Evaluation scripts in Python: Recall@K, MRR, response time; output a comparison table and a graph (Version 1 vs Version 2).
9. UI: show retrieved chunks with scores and sources; admin document list with delete/update.

### Not in Review 2
Conflict handling, faithfulness verification, safe fallback.

### Suggested Codex task order
1. Query processing service (+ tests).
2. Full-text search + index.
3. Hybrid merge (RRF) (+ tests).
4. Temporal/metadata filter (+ tests with 2023/2025/2026 documents).
5. Reranker FastAPI service + Java client (+ pytest).
6. Pipeline mode flags in config.
7. Evaluation scripts (Recall@K, MRR, latency, table, graph).
8. UI updates.

### Definition of done
- Same question answered under both versions; Version 2 picks the newer regulation correctly.
- `evaluation/results_v2.*` plus a Version 1 vs 2 comparison table and graph.

### Demo checklist
Side-by-side Version 1 vs Version 2 for a version-sensitive question; evaluation table.

---

## REVIEW 3 — Trust Layer + Final System (Version 3 = EduTrust)

**Goal:** conflict handling, faithfulness verification, citations, safe fallback, full evaluation.

### Scope (implement)
1. Conflict detection (rule-based): after reranking, compare top evidence on the same topic; if values differ (e.g. 75% vs 80%), resolve using version → academic year → document date → source authority → department applicability. If unresolved, return: "The available documents contain conflicting information. Please verify with the concerned department."
2. Faithfulness verification: split the generated answer into claims; for each claim ask the LLM (judge prompt) whether the evidence supports it (SUPPORTED / UNSUPPORTED). Score = supported claims / total claims. Optional upgrade if time permits: small NLI model in the Python service.
3. Decision logic (thresholds in config): score ≥ threshold → accept; below → regenerate once (stricter prompt) or safe fallback: "I could not verify this information from the available institutional documents."
4. Citations only for verified claims: "Source: <document>, Page <n>".
5. Out-of-scope questions must trigger safe fallback.
6. Full evaluation of three versions: V1 Basic RAG, V2 Improved RAG, V3 EduTrust. Metrics: Recall@K, MRR, Answer Relevance, Groundedness, Faithfulness, Citation Correctness, Conflict Resolution Accuracy, Response Time.
7. Docker Compose for one-command startup (DB + reranker; backend/frontend optional).
8. Tests (JUnit + API), error handling, UI polish (show verdict, faithfulness score, citations, fallback messages).

### Suggested Codex task order
1. Conflict detection + authority resolution (+ tests using the 2023/2025/2026 and CSE circular documents).
2. Claim splitting + judge prompt + score calculation (+ tests with a known unsupported claim).
3. Decision logic + fallback messages.
4. Citation generator.
5. Wire full pipeline behind a `version=3` mode.
6. Extend evaluation scripts to 3 versions + conflict accuracy + faithfulness.
7. Docker Compose finalization, tests, UI polish.

### Definition of done
- Trap questions behave correctly: version conflict resolved, unresolved conflict flagged, out-of-scope gets fallback, unsupported claim caught.
- `evaluation/results_v3.*` and a final 3-version comparison table + graphs.

### Demo checklist
Conflict question, out-of-scope question, hallucination-prone question, final comparison table.

---

## April buffer
Final report, PPT, backup demo video (screen recording), bug fixes, README with run instructions. Optional: free cloud deployment (frontend on Vercel, DB on Supabase); the demo does not depend on it.

---

## How to use this with Codex
1. Put `AGENTS.md` and `PLAN.md` in the repo root.
2. Start each session with: "Read AGENTS.md and PLAN.md. We are on Review N. Do task K only."
3. After each task: run it, verify, `git commit`, then next task.
4. Before each review: explain every pipeline stage yourself (viva).
