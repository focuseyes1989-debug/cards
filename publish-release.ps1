param([string]$NotesFile = "$PSScriptRoot\release-notes.md")
$ErrorActionPreference = 'Stop'
if(Test-Path "$PSScriptRoot\.local-tools.ps1") { . "$PSScriptRoot\.local-tools.ps1" }
$gh = if($env:CARD_GH) { $env:CARD_GH } else { 'gh' }
$repo = 'focuseyes1989-debug/cards'
Push-Location $PSScriptRoot
try {
    & $gh api "repos/$repo" --jq '.permissions.push' | Out-Null
    if($LASTEXITCODE -ne 0) { throw 'Log into GitHub with gh auth login first.' }
    & "$PSScriptRoot\build.ps1"
    $meta = Get-Content "$PSScriptRoot\app\build\outputs\apk\debug\output-metadata.json" -Raw | ConvertFrom-Json
    $version = $meta.elements[0].versionName
    $code = $meta.elements[0].versionCode
    $tag = "v$version"
    $apk = "$PSScriptRoot\dist\Invitation-Gallery.apk"
    $sdk = if($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
    $signer = Get-ChildItem "$sdk\build-tools" -Filter apksigner.bat -Recurse | Sort-Object FullName -Descending | Select-Object -First 1
    if(!$signer) { throw 'Android build-tools apksigner is required.' }
    $certs = & $signer.FullName verify --print-certs $apk
    if($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $expected = (Get-Content "$PSScriptRoot\release-signing.json" -Raw | ConvertFrom-Json).certificateSha256
    if(!($certs | Where-Object { $_ -match 'certificate SHA-256 digest:' -and $_.EndsWith($expected) })) { throw 'Signing key differs from installed app. Use the original keystore.' }
    $manifest = [ordered]@{schema=1;packageName='com.invitation.gallery';versionCode=[long]$code;versionName=$version;minSdk=26;apkUrl="https://github.com/$repo/releases/download/$tag/Invitation-Gallery.apk";sha256=(Get-FileHash -Algorithm SHA256 $apk).Hash.ToLowerInvariant();size=(Get-Item $apk).Length;notes=(Get-Content -LiteralPath $NotesFile -Raw).Trim()}
    [IO.File]::WriteAllText("$PSScriptRoot\dist\update.json",($manifest | ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
    $existing = & $gh release view $tag --repo $repo --json isDraft 2>$null
    if($LASTEXITCODE -eq 0) {
        if(!(($existing | ConvertFrom-Json).isDraft)) { throw 'This version is already published. Increase versionCode and versionName before releasing.' }
    } else {
        & $gh release create $tag --repo $repo --draft --target main --title "Invitation Gallery $tag" --notes-file $NotesFile
        if($LASTEXITCODE -ne 0) { throw 'Could not create draft release.' }
    }
    & $gh release upload $tag $apk "$PSScriptRoot\dist\update.json" --repo $repo --clobber
    if($LASTEXITCODE -ne 0) { throw 'Upload failed. Release remains a draft.' }
    & $gh release edit $tag --repo $repo --draft=false --latest
    if($LASTEXITCODE -ne 0) { throw 'Could not publish release.' }
    Write-Output "Published https://github.com/$repo/releases/tag/$tag"
} finally { Pop-Location }
