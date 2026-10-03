$ErrorActionPreference = 'Stop'

$baseUrl = if ($env:EDUTRUST_BASE_URL) { $env:EDUTRUST_BASE_URL } else { 'http://localhost:8080' }
$loginBody = '{"email":"admin@college.edu","password":"admin123"}'
$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" -ContentType 'application/json' -Body $loginBody
$token = $login.token
$headers = @{ Authorization = "Bearer $token" }

$documents = @(
    @{ File = 'academic-regulations-2023.pdf'; Title = 'Academic Regulations 2023'; Department = 'All'; DocType = 'Regulations'; AcademicYear = '2023-24'; Version = 'v1'; DocDate = '2023-06-15'; Authority = 'Dean Academics' },
    @{ File = 'academic-regulations-2025.pdf'; Title = 'Academic Regulations 2025'; Department = 'All'; DocType = 'Regulations'; AcademicYear = '2025-26'; Version = 'v2'; DocDate = '2025-06-20'; Authority = 'Dean Academics' },
    @{ File = 'academic-regulations-2026.pdf'; Title = 'Academic Regulations 2026'; Department = 'All'; DocType = 'Regulations'; AcademicYear = '2026-27'; Version = 'v3'; DocDate = '2026-06-15'; Authority = 'Dean Academics' },
    @{ File = 'academic-calendar-2025-26.pdf'; Title = 'Academic Calendar 2025-26'; Department = 'All'; DocType = 'Academic Calendar'; AcademicYear = '2025-26'; Version = 'v1'; DocDate = '2025-07-10'; Authority = 'Dean Academics' },
    @{ File = 'fee-structure-2025-26.pdf'; Title = 'Fee Structure 2025-26'; Department = 'All'; DocType = 'Fee Structure'; AcademicYear = '2025-26'; Version = 'v1'; DocDate = '2025-07-01'; Authority = 'Accounts Section' },
    @{ File = 'cse-department-circular-2026.pdf'; Title = 'CSE Department Circular 2026'; Department = 'CSE'; DocType = 'Circular'; AcademicYear = '2025-26'; Version = 'v1'; DocDate = '2026-01-20'; Authority = 'Department Head' }
)

foreach ($document in $documents) {
    $path = Join-Path (Join-Path $PSScriptRoot '..') "dataset/documents/$($document.File)"
    & curl.exe -sS -X POST "$baseUrl/api/documents" `
        -H "Authorization: Bearer $token" `
        -F "file=@$path" `
        -F "title=$($document.Title)" `
        -F "department=$($document.Department)" `
        -F "docType=$($document.DocType)" `
        -F "academicYear=$($document.AcademicYear)" `
        -F "version=$($document.Version)" `
        -F "docDate=$($document.DocDate)" `
        -F "authority=$($document.Authority)"
    if ($LASTEXITCODE -ne 0) {
        throw "Upload failed for $($document.File)"
    }
}
