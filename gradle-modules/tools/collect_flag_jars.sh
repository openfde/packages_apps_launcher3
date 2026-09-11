#!/bin/bash
# Collect the real aconfig flag jars that the launcher references at runtime.
#
# These classes live under com.android.internal.hidden_from_bootclasspath.* in the
# platform framework and are NOT present at runtime on the device (the release
# build inlines the constants via R8). Debug builds must package them.
#
# Note: android.app.appfunctions.flags.Flags is intentionally NOT collected,
# because the androidx.appfunctions AAR already ships that class.
set -u
cd /aosp4/hy_fde17
OUT=/tmp/flag_jars
rm -rf "$OUT"; mkdir -p "$OUT"

copy_mod() {
  local d="$1" j
  j=$(find "$d" -path '*android_common/javac/*.jar' 2>/dev/null | head -1)
  if [ -n "$j" ]; then
    cp "$j" "$OUT/"
    echo "collected $(basename "$j")"
  else
    echo "MISSING $d"
  fi
}

copy_mod out/soong/.intermediates/frameworks/base/android.companion.flags-aconfig-java
copy_mod out/soong/.intermediates/frameworks/base/android.multiuser.flags-aconfig-java
copy_mod out/soong/.intermediates/frameworks/base/android.os.flags-aconfig-java
copy_mod out/soong/.intermediates/frameworks/base/android.security.flags-aconfig-java-export

ls -la "$OUT"
