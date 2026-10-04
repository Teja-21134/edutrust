$ErrorActionPreference = 'Stop'

$baseUrl = if ($env:EDUTRUST_BASE_URL) { $env:EDUTRUST_BASE_URL } else { 'http://localhost:8080' }
$loginBody = '{"email":"admin@college.edu","password":"admin123"}'
$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.token)" }

$documents = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/documents" -Headers $headers
foreach ($document in $documents) {
    Invoke-RestMethod -Method Delete -Uri "$baseUrl/api/documents/$($document.id)" -Headers $headers
    Write-Host "Deleted $($document.id) - $($document.title)"
}

Write-Host "Document reset complete. Re-upload with scripts/upload-dataset.ps1."
