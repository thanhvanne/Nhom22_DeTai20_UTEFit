param([string]$EnvironmentFile = 'target/gd7-qa-env.ps1')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $EnvironmentFile)) { throw 'Set QA environment variables or supply the ignored QA environment file.' }
. (Resolve-Path -LiteralPath $EnvironmentFile)
$runtimeDirectory = Join-Path (Get-Location) 'target/qa-runtime'
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
$runtimeJar = Join-Path $runtimeDirectory ('utefit-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.jar')
Copy-Item -LiteralPath 'target/utefit-0.0.1-SNAPSHOT.jar' -Destination $runtimeJar
& java -jar $runtimeJar --spring.profiles.active=qa --spring.config.additional-location=src/main/resources/application-qa.properties
exit $LASTEXITCODE
