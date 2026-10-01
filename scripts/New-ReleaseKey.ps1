# SPDX-License-Identifier: Apache-2.0
# Run once. Back up both files in release-secrets outside this computer.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$secrets = Join-Path $root 'release-secrets'
$key = Join-Path $secrets 'folderkit-release.p12'
$passwordFile = Join-Path $secrets 'keystore-password.txt'
if ((Test-Path -LiteralPath $key) -or (Test-Path -LiteralPath $passwordFile)) {
    throw 'A release key or password already exists. Refusing to replace it.'
}
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Android\Android Studio\jbr' }
$keytool = Join-Path $javaHome 'bin\keytool.exe'
if (!(Test-Path -LiteralPath $keytool)) { throw "keytool not found: $keytool" }
New-Item -ItemType Directory -Path $secrets -Force | Out-Null
$random = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($random) } finally { $rng.Dispose() }
$password = ([BitConverter]::ToString($random)).Replace('-', '')
$env:FOLDERKIT_RELEASE_PASSWORD = $password
try {
    & $keytool -genkeypair -v -storetype PKCS12 -keystore $key -alias folderkit `
        -keyalg RSA -keysize 4096 -validity 10000 `
        -dname 'CN=FolderKit, O=Ribnik Studio, C=ID' `
        -storepass:env FOLDERKIT_RELEASE_PASSWORD -keypass:env FOLDERKIT_RELEASE_PASSWORD
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed.' }
    [System.IO.File]::WriteAllText($passwordFile, $password, [System.Text.Encoding]::ASCII)
} finally {
    Remove-Item Env:FOLDERKIT_RELEASE_PASSWORD -ErrorAction SilentlyContinue
}
Write-Host "Release key created at $key"
Write-Host "Password file created at $passwordFile"
Write-Host 'Back up BOTH files offline. Losing either prevents updates to apps signed with this key.'
