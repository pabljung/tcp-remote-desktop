param(
    [Parameter(Mandatory = $true)]
    [string] $ProjectDirectory
)

$ErrorActionPreference = "Stop"

$gitDirectory = git -C $ProjectDirectory rev-parse --absolute-git-dir
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$buildDirectory = Join-Path $gitDirectory "pre-commit-build"
New-Item -ItemType Directory -Force -Path $buildDirectory | Out-Null

$sourceDirectory = Join-Path $ProjectDirectory "src/main/java"
$sources = @(
    Get-ChildItem -LiteralPath $sourceDirectory -Recurse -File -Filter "*.java" |
        Select-Object -ExpandProperty FullName
)

if ($sources.Count -eq 0) {
    Write-Error "Nenhum arquivo Java foi encontrado em $sourceDirectory."
    exit 1
}

& javac --release 17 -encoding UTF-8 -d $buildDirectory $sources
exit $LASTEXITCODE
