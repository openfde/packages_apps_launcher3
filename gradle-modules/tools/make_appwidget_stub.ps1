# Generate the android.appwidget.flags.Flags runtime stub used by debug builds.
#
# The platform class is hidden from the boot classpath and the release build
# inlines its values through R8; a debug APK needs a concrete class at runtime.
# Only the two accessors used by Launcher3 are provided.

$ErrorActionPreference = "Stop"
$tools = Split-Path -Parent $MyInvocation.MyCommand.Path
$moduleRoot = Split-Path -Parent $tools
$prebuilts = Join-Path $moduleRoot "prebuilts"
$outJar = Join-Path $prebuilts "jars\android-appwidget-flags-stub.jar"

$jdk = "D:\huyang\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
if (-not (Test-Path (Join-Path $jdk "bin\javac.exe"))) {
    throw "JDK 21 not found at $jdk"
}

$work = Join-Path $env:TEMP "launcher_appwidget_stub"
if (Test-Path $work) { Remove-Item -Recurse -Force $work }
New-Item -ItemType Directory -Force -Path "$work\src\android\appwidget\flags", "$work\classes" | Out-Null

Set-Content -LiteralPath "$work\src\android\appwidget\flags\Flags.java" -Value @"
package android.appwidget.flags;

/** Local runtime stub for the hidden platform aconfig flag accessors. */
public final class Flags {
    private Flags() {}

    public static boolean engagementMetrics() {
        return true;
    }

    public static boolean generatedPreviews() {
        return true;
    }
}
"@

& (Join-Path $jdk "bin\javac.exe") -d "$work\classes" "$work\src\android\appwidget\flags\Flags.java"
Push-Location "$work\classes"
& (Join-Path $jdk "bin\jar.exe") cf $outJar .
Pop-Location
Write-Host "wrote $outJar"
