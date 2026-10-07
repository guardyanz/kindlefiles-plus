# SPDX-License-Identifier: Apache-2.0
param([string]$UnsignedApk = '')
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$key = Join-Path $root 'release-secrets\folderkit-release.p12'
$passwordFile = Join-Path $root 'release-secrets\keystore-password.txt'
if (!(Test-Path -LiteralPath $key) -or !(Test-Path -LiteralPath $passwordFile)) {
    throw 'Missing release key. Run scripts/New-ReleaseKey.ps1 once, or restore both backed-up files.'
}
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Android\Android Studio\jbr' }
$env:JAVA_HOME = $javaHome
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$buildTools = Join-Path $sdk 'build-tools\35.0.0'
$zipalign = Join-Path $buildTools 'zipalign.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'
$aapt = Join-Path $buildTools 'aapt.exe'
foreach ($tool in @($zipalign, $apksigner, $aapt)) {
    if (!(Test-Path -LiteralPath $tool)) { throw "Android build tool not found: $tool" }
}
$gradleFile = Get-Content (Join-Path $root 'app\build.gradle.kts') -Raw
$version = [regex]::Match($gradleFile, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if (!$version) { throw 'Could not read versionName.' }
Push-Location $root
try {
    if ($UnsignedApk) {
        $unsigned = (Resolve-Path -LiteralPath $UnsignedApk).Path
    } else {
        & .\gradlew.bat :app:assembleRelease --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' }
        $unsigned = Join-Path $root 'app\build\outputs\apk\release\app-release-unsigned.apk'
    }
    if (!(Test-Path -LiteralPath $unsigned)) { throw "Unsigned APK not found: $unsigned" }
    $inputBadging = & $aapt dump badging $unsigned
    if ($LASTEXITCODE -ne 0 -or (($inputBadging -join "`n") -notmatch "package: name='io.github.guardyanz.kindlefiles'.*versionName='$([regex]::Escape($version))'")) {
        throw 'Unsigned APK package or version does not match this checkout.'
    }
    $dist = Join-Path $root 'dist'
    New-Item -ItemType Directory -Path $dist -Force | Out-Null
    $aligned = Join-Path $dist 'folderkit-aligned-unsigned.apk'
    $signed = Join-Path $dist "folderkit-$version.apk"
    if (Test-Path -LiteralPath $signed) { Remove-Item -LiteralPath $signed }
    & $zipalign -f -P 16 4 $unsigned $aligned
    if ($LASTEXITCODE -ne 0) { throw 'zipalign failed.' }
    & $apksigner sign --v2-signing-enabled true --v3-signing-enabled true `
        --ks $key --ks-type PKCS12 --ks-key-alias folderkit `
        --ks-pass "file:$passwordFile" --out $signed $aligned
    if ($LASTEXITCODE -ne 0) { throw 'APK signing failed.' }
    & $zipalign -c -P 16 4 $signed
    if ($LASTEXITCODE -ne 0) { throw 'Signed APK alignment failed.' }
    $verification = & $apksigner verify --verbose --print-certs $signed
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $verificationText = $verification -join "`n"
    if (($verificationText -notmatch 'Verified using v2 scheme.*: true') -and
        ($verificationText -notmatch 'Verified using v3 scheme.*: true')) {
        throw 'APK must verify with v2 or newer signing.'
    }
    Write-Host $verification
    $badging = & $aapt dump badging $signed
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect APK metadata.' }
    Write-Host ($badging | Select-String '^(package:|sdkVersion:|targetSdkVersion:|application-label:)')
    $permissions = & $aapt dump permissions $signed
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect APK permissions.' }
    if ($permissions -match 'android.permission.INTERNET|MANAGE_EXTERNAL_STORAGE') {
        throw 'Unexpected network or all-files permission in release APK.'
    }
    Write-Host $permissions
    $hash = Get-FileHash -Algorithm SHA256 -LiteralPath $signed
    Write-Host "Signed APK: $signed"
    Write-Host "SHA-256: $($hash.Hash)"
} finally {
    Pop-Location
}
