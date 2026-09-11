# Sign the built APK with a platform/test key so it can replace the system
# Launcher3QuickStep on the device.
#
# Usage: .\sign_platform.ps1 [-KeyName testkey] [-Apk <path>] [-Out <path>]
#
# The FDE ROM is built with the AOSP `testkey` (userdebug default). Use
# -KeyName platform for ROMs built with the platform key.
#
# Requires the keys extracted into prebuilts/keys/ by the sync script.

param(
    [string]$KeyName = "testkey",
    [string]$Apk = "",
    [string]$Out = ""
)

$ErrorActionPreference = "Stop"
$moduleRoot = Split-Path -Parent $PSScriptRoot
if (-not $Apk) {
    $Apk = Join-Path $moduleRoot "app\build\outputs\apk\debug\app-debug.apk"
}
$keys = Join-Path $moduleRoot "prebuilts\keys"
$keyFile = Join-Path $keys "$KeyName.pk8"
$certFile = Join-Path $keys "$KeyName.x509.pem"
$sdk = "D:\huyang\Android\sdk"
$buildTools = Get-ChildItem "$sdk\build-tools" -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName "apksigner.bat") } |
    Sort-Object Name -Descending | Select-Object -First 1
$apksigner = Join-Path $buildTools.FullName "apksigner.bat"
$zipalign = Join-Path $buildTools.FullName "zipalign.exe"

if (-not $Out) {
    $Out = Join-Path (Split-Path -Parent $Apk) "app-$KeyName-signed.apk"
}

if (-not (Test-Path $keyFile)) {
    throw "$KeyName.pk8 not found in $keys. Run sync_from_rom.ps1 first."
}

$aligned = Join-Path ([System.IO.Path]::GetDirectoryName($Out)) "app-aligned-tmp.apk"
& $zipalign -f 4 $Apk $aligned
& $apksigner sign --key $keyFile --cert $certFile --out $Out $aligned
Remove-Item $aligned -ErrorAction SilentlyContinue

& $apksigner verify --print-certs $Out | Select-Object -First 6
Write-Host "signed APK: $Out"
