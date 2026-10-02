$ErrorActionPreference = "Stop"
$javaHomePath = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { $null }
if (-not $javaHomePath -or -not (Test-Path $javaHomePath)) {
    $javaExecutable = (Get-Command java -ErrorAction Stop).Source
    $env:JAVA_HOME = Split-Path (Split-Path $javaExecutable -Parent) -Parent
}
$version = "3.9.9"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$distribution = Join-Path $root ".mvn\wrapper\dists"
$mavenHome = Join-Path $distribution "apache-maven-$version"
$maven = Join-Path $mavenHome "bin\mvn.cmd"

if (-not (Test-Path $maven)) {
    New-Item -ItemType Directory -Force -Path $distribution | Out-Null
    $archive = Join-Path $distribution "apache-maven-$version-bin.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip"
    Write-Host "Downloading Maven $version..."
    Invoke-WebRequest -Uri $url -OutFile $archive
    Expand-Archive -Path $archive -DestinationPath $distribution -Force
    $extracted = Join-Path $distribution "apache-maven-$version"
    if (-not (Test-Path $maven)) { throw "Maven distribution was not extracted to $extracted" }
}

& $maven @args
exit $LASTEXITCODE
