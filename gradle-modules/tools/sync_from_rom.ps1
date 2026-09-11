# Re-extract all ROM prebuilts and refresh the local Gradle build inputs.
#
# Connection settings are resolved in this order (first non-empty wins):
#   1. command line parameters
#   2. environment variables: ROM_HOST / ROM_USER / ROM_TOP / ROM_SSH_PORT / ROM_PASS
#   3. local, git-ignored config file gradle-modules/rom.properties:
#        rom.host=10.0.0.1
#        rom.user=builduser
#        rom.top=/aosp
#        rom.ssh_port=22
#
# Usage:
#   $env:ROM_PASS = "password"          # optional, when key auth is unavailable
#   .\sync_from_rom.ps1
#   .\sync_from_rom.ps1 -RomHost 10.0.0.1 -RomUser builduser -RomTop /aosp
#
# Steps: upload remote scripts -> run extraction on the ROM host -> download
# tarballs -> replace gradle-modules/prebuilts -> run local post-processing.

param(
    [string]$RomHost = $env:ROM_HOST,
    [string]$RomUser = $env:ROM_USER,
    [string]$RomTop  = $env:ROM_TOP,
    [string]$SshPort = $env:ROM_SSH_PORT
)

$ErrorActionPreference = "Stop"
$tools = Split-Path -Parent $MyInvocation.MyCommand.Path
$moduleRoot = Split-Path -Parent $tools
$prebuilts = Join-Path $moduleRoot "prebuilts"

$configFile = Join-Path $moduleRoot "rom.properties"
if (Test-Path $configFile) {
    $cfg = @{}
    Get-Content -LiteralPath $configFile | ForEach-Object {
        if ($_ -match '^\s*([^#=\s][^=]*?)\s*=\s*(.*?)\s*$') {
            $cfg[$Matches[1]] = $Matches[2]
        }
    }
    if (-not $RomHost) { $RomHost = $cfg['rom.host'] }
    if (-not $RomUser) { $RomUser = $cfg['rom.user'] }
    if (-not $RomTop)  { $RomTop  = $cfg['rom.top'] }
    if (-not $SshPort) { $SshPort = $cfg['rom.ssh_port'] }
}

if (-not $RomHost -or -not $RomUser -or -not $RomTop) {
    throw @"
Missing ROM connection settings.
Provide them via parameters, environment variables (ROM_HOST/ROM_USER/ROM_TOP),
or a local gradle-modules/rom.properties file, for example:

    rom.host=10.0.0.1
    rom.user=builduser
    rom.top=/aosp
"@
}
if (-not $SshPort) { $SshPort = "22" }

$sshArgs = @("-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=NUL", "-o", "ConnectTimeout=15", "-p", $SshPort)
$scpArgs = @("-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=NUL", "-o", "ConnectTimeout=15", "-P", $SshPort)

function New-AskPass {
    if (-not $env:ROM_PASS) { return }
    $askpass = Join-Path $env:TEMP "launcher_askpass.cmd"
    Set-Content -LiteralPath $askpass -Value "@echo off`r`necho $env:ROM_PASS" -Encoding ASCII
    $env:SSH_ASKPASS = $askpass
    $env:SSH_ASKPASS_REQUIRE = "force"
    $env:DISPLAY = "localhost:0"
}

function Invoke-RomSsh([string]$Command) {
    ssh @sshArgs "$RomUser@$RomHost" $Command
}

function Copy-FromRom([string]$Remote, [string]$Local) {
    scp @scpArgs "${RomUser}@${RomHost}:$Remote" $Local
}

New-AskPass

Write-Host "== uploading tools to $RomHost =="
scp @scpArgs `
    (Join-Path $tools "extract_prebuilts.sh") `
    (Join-Path $tools "build_extra_aars.sh") `
    (Join-Path $tools "collect_flag_jars.sh") `
    "${RomUser}@${RomHost}:/tmp/"

Write-Host "== extracting prebuilts on the ROM host (this takes a few minutes) =="
Invoke-RomSsh "cd $RomTop && bash /tmp/extract_prebuilts.sh && bash /tmp/build_extra_aars.sh && bash /tmp/collect_flag_jars.sh && tar -czf /tmp/launcher_stage.tgz launcher_gradle_stage && tar -czf /tmp/launcher_extra.tgz -C /tmp extra_aars && tar -czf /tmp/launcher_flags.tgz -C /tmp flag_jars && echo SYNC_DONE"

Write-Host "== downloading tarballs =="
Copy-FromRom "/tmp/launcher_stage.tgz" (Join-Path $moduleRoot "launcher_stage.tgz")
Copy-FromRom "/tmp/launcher_extra.tgz" (Join-Path $moduleRoot "launcher_extra.tgz")
Copy-FromRom "/tmp/launcher_flags.tgz" (Join-Path $moduleRoot "launcher_flags.tgz")

Write-Host "== replacing local prebuilts =="
if (Test-Path $prebuilts) { Remove-Item -Recurse -Force $prebuilts }
tar -xzf (Join-Path $moduleRoot "launcher_stage.tgz") -C $moduleRoot
Rename-Item (Join-Path $moduleRoot "launcher_gradle_stage") $prebuilts
tar -xzf (Join-Path $moduleRoot "launcher_extra.tgz") -C $moduleRoot
Get-ChildItem (Join-Path $moduleRoot "extra_aars") -Filter *.aar | ForEach-Object {
    Move-Item $_.FullName (Join-Path $prebuilts "aars") -Force
}
Remove-Item -Recurse -Force (Join-Path $moduleRoot "extra_aars")
tar -xzf (Join-Path $moduleRoot "launcher_flags.tgz") -C $moduleRoot
Get-ChildItem (Join-Path $moduleRoot "flag_jars") -Filter *.jar | ForEach-Object {
    Move-Item $_.FullName (Join-Path $prebuilts "jars") -Force
}
Remove-Item -Recurse -Force (Join-Path $moduleRoot "flag_jars")
Remove-Item (Join-Path $moduleRoot "launcher_stage.tgz"), (Join-Path $moduleRoot "launcher_extra.tgz"), (Join-Path $moduleRoot "launcher_flags.tgz")

Write-Host "== post-processing =="
python (Join-Path $tools "postprocess_prebuilts.py")
python (Join-Path $tools "sync_res.py")
python (Join-Path $tools "merge_android_jar.py")
& (Join-Path $tools "make_appwidget_stub.ps1")
& (Join-Path $tools "make_keystore.ps1")

Write-Host "== done. Run: .\gradlew.bat :app:assembleDebug --no-parallel =="
