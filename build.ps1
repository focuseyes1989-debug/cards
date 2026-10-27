$ErrorActionPreference = 'Stop'
if (Test-Path "$PSScriptRoot\.local-tools.ps1") { . "$PSScriptRoot\.local-tools.ps1" }
$gradle = Join-Path $PSScriptRoot 'gradlew.bat'
Push-Location $PSScriptRoot
try {
    & $gradle assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
    New-Item -ItemType Directory -Path "$PSScriptRoot\dist" -Force | Out-Null
    Copy-Item -LiteralPath "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk" -Destination "$PSScriptRoot\dist\Invitation-Gallery.apk"
} finally { Pop-Location }
