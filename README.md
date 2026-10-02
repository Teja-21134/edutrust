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
