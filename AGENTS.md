# AGENTS.md — EduTrust

Project: **EduTrust — Faithfulness-Aware Hybrid RAG for Educational Institutions**.
Read `PLAN.md` before doing anything. It defines three reviews. Only implement the review I name.

## Hard rules
1. Work on ONE review at a time. Do not add features from later reviews.
2. Small tasks only. After each task: build, run tests, tell me how to verify, then stop and wait.
3. Everything must run locally and free. No paid APIs, no cloud services, no API keys required.
4. Do not change the tech stack in PLAN.md without asking.
5. Every pipeline stage must be a separate class/service with its own test (so I can explain and evaluate each stage).
6. All thresholds, model names, top-K values go in `application.yml`, never hard-coded.
7. Log each pipeline stage's output (retrieved chunks, scores, verdicts) so I can show it in demos.
8. After finishing a task, explain in 5-8 lines what the code does, in simple language (I must explain it in viva).
9. Commit-ready code only: no dead code, no TODO placeholders that break the build.

## Stack
- Backend: Java 21, Spring Boot 3.x, Maven, Spring Security + JWT, Spring Data JPA
- RAG: LangChain4j, Apache PDFBox, BGE-small-en-v1.5 embeddings (in-process ONNX, 384 dims)
- LLM: Ollama (local)
- DB: PostgreSQL 16 + pgvector (Docker), PostgreSQL full-text search
- Reranker: Python FastAPI microservice with BAAI/bge-reranker-base
- Frontend: React (Vite) + Tailwind CSS + Axios
- Tests: JUnit 5, Spring Boot Test; Python: pytest
## Working style
- Final message at most 8 lines. Run tests yourself and fix failures. Do not paste long logs.
- Open only the files you need. No refactors outside the task.
- Only the Flyway migration named in the task may be added.