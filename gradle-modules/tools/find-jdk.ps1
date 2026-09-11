# Resolve a JDK 21 installation for the helper scripts.
#
# Search order:
#   1. $env:LAUNCHER_JDK
#   2. $env:JAVA_HOME
#   3. Gradle toolchain-provisioned JDKs under $env:GRADLE_USER_HOME\jdks (or ~/.gradle/jdks)
#   4. "java" on PATH (its parent directory)
# Any JDK 21 distribution works (Temurin, Microsoft, Zulu, JBR, ...).

function Get-Jdk21 {
    $candidates = New-Object System.Collections.Generic.List[string]
    if ($env:LAUNCHER_JDK) { $candidates.Add($env:LAUNCHER_JDK) }
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }

    $gradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE ".gradle" }
    $jdksDir = Join-Path $gradleHome "jdks"
    if (Test-Path $jdksDir) {
        Get-ChildItem $jdksDir -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '21' } |
            ForEach-Object { $candidates.Add($_.FullName) }
    }

    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $candidates.Add((Split-Path -Parent (Split-Path -Parent $javaCmd.Source)))
    }

    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $javac = Join-Path $candidate "bin\javac.exe"
        if (-not (Test-Path $javac)) { continue }
        $version = (& $javac -version 2>&1) -join " "
        if ($version -match '^javac 21') { return $candidate }
    }

    throw @"
JDK 21 not found.
Install a JDK 21 and set LAUNCHER_JDK or JAVA_HOME, e.g.:
    `$env:LAUNCHER_JDK = "C:\Program Files\Eclipse Adoptium\jdk-21"
"@
}
