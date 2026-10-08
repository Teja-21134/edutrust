# edutrust
## How to run

Start the local PostgreSQL 16 + pgvector database from the repository root:

```powershell
docker compose up -d
```

Run the backend from PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Verify the health endpoint in a second PowerShell window:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Expected response:

```text
status
------
UP
```

## Adapting EduTrust to another institution

1. Edit `backend/src/main/resources/institution.yml` with the institution's departments, document types, and messages.
2. Set `INSTITUTION_CONFIG` to an external YAML file when the configuration should live outside the build.
3. Replace the files in `dataset/documents` with the institution's documents.
4. Upload the documents with the existing upload script or admin API, supplying their metadata.
5. Run the evaluation scripts and review the retrieval and answer results.

Create the test database once before running backend integration tests:

```powershell
docker exec edutrust-postgres psql -U edutrust -d edutrust -c "CREATE DATABASE edutrust_test;"
```
