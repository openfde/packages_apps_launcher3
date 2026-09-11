#!/bin/bash
# Build local prebuilt dependency set for the Launcher3 Gradle port.
# Run at AOSP top (/aosp4/hy_fde17). Output: launcher_gradle_stage/
set -u
TOP=/aosp4/hy_fde17
cd "$TOP"
OUT="$TOP/out"
MOD="$OUT/soong/.intermediates/packages/apps/Launcher3"
STAGE="$TOP/launcher_gradle_stage"
rm -rf "$STAGE"
mkdir -p "$STAGE/jars" "$STAGE/aars" "$STAGE/generated" "$STAGE/manifest" "$STAGE/platform" "$STAGE/keys"

echo "### versions"
{
  echo "--- dagger ---"
  find external/dagger2 -maxdepth 3 -iname '*version*' 2>/dev/null | head -5
  grep -m3 -iE 'version' external/dagger2/METADATA 2>/dev/null
  echo "--- protobuf ---"
  head -8 external/protobuf/version.json 2>/dev/null
  echo "--- kotlinc ---"
  cat external/kotlinc/build.txt 2>/dev/null | head -2
} | tee "$STAGE/versions.txt"

echo "### generated"
cp "$MOD/launcher-build-config/gen/BuildConfig.java" "$STAGE/generated/"
cp "$MOD/launcher-quickstep-processed-protolog-src/android_common/gen/launcher.quickstep.protolog.srcjar" "$STAGE/generated/protolog.srcjar"
cp "$MOD/launcher_log_protos_lite/android_common/gen/proto/proto0.srcjar" "$STAGE/generated/protos_launcher.srcjar"
cp "$MOD/launcher_quickstep_log_protos_lite/android_common/gen/proto/proto0.srcjar" "$STAGE/generated/protos_quickstep.srcjar"
cp "$MOD/aconfig/com_android_launcher3_flags_lib/android_common/turbine-apt/turbine-apt-sources.jar" "$STAGE/generated/aconfig_launcher_sources.jar"

echo "### launcher internal prebuilt jars"
cp "$MOD/aconfig/com_android_launcher3_flags_lib/android_common/repackaged-jarjar/javac/com_android_launcher3_flags_lib.jar" "$STAGE/jars/" 2>/dev/null \
  || cp "$MOD/aconfig/com_android_launcher3_flags_lib/android_common/javac/com_android_launcher3_flags_lib.jar" "$STAGE/jars/"
cp "$MOD/launcher_log_protos_lite/android_common/javac/launcher_log_protos_lite.jar" "$STAGE/jars/"
cp "$MOD/launcher_quickstep_log_protos_lite/android_common/javac/launcher_quickstep_log_protos_lite.jar" "$STAGE/jars/"
cp "$MOD/launcher-quickstep_protolog-groups/android_common/repackaged-jarjar/javac/launcher-quickstep_protolog-groups.jar" "$STAGE/jars/" 2>/dev/null \
  || cp "$MOD/launcher-quickstep_protolog-groups/android_common/javac/launcher-quickstep_protolog-groups.jar" "$STAGE/jars/"

echo "### manifest / platform / keys"
cp "$MOD/Launcher3QuickStep/android_common/manifest_merger/AndroidManifest.xml" "$STAGE/manifest/"
cp "$OUT/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/jarjar/framework.jar" "$STAGE/platform/"
TURBINE="$OUT/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/jarjar/turbine/framework.jar"
[ -f "$TURBINE" ] && cp "$TURBINE" "$STAGE/platform/framework-jarjar-turbine.jar"
FRES=$(find "$OUT" -maxdepth 12 -name 'framework-res.apk' -not -path '*od_sign*' 2>/dev/null | head -1)
[ -n "$FRES" ] && cp "$FRES" "$STAGE/platform/"
CFS=$(find "$OUT" -maxdepth 12 -name 'core-for-system-modules.jar' 2>/dev/null | head -1)
[ -n "$CFS" ] && cp "$CFS" "$STAGE/platform/"
cp build/make/target/product/security/platform.pk8 "$STAGE/keys/" 2>/dev/null || true
cp build/make/target/product/security/platform.x509.pem "$STAGE/keys/" 2>/dev/null || true

CP=/tmp/cp.txt
grep -oE '[^ ]+\.jar' "$MOD/Launcher3QuickStepLib/android_common/kotlinc/classpath.rsp" | sort -u > "$CP"

merge_jars() { # $1 mdir, $2 out
  local mdir="$1" out="$2" tmp found
  tmp=$(mktemp -d)
  found=0
  while IFS= read -r j; do
    if (cd "$tmp" && unzip -oq "/aosp4/hy_fde17/$j" 2>/dev/null); then found=1; fi
  done < <(find "$mdir" -name '*.jar' 2>/dev/null \
     | grep -v -E '/(busybox|kapt|ksp|kotlin_headers|hiddenapi|stubs|turbine|turbine-apt|anno|local-javac-header|local-combined)/' \
     | grep -v -E '(stubs\.jar|\.1\.jar$|\.0\.jar$|R\.jar|src\.jar|sources\.jar|\.rsp)$')
  if [ "$found" = 1 ]; then (cd "$tmp" && zip -qr "$out" .); fi
  rm -rf "$tmp"
}

find_srcdir() { # module path under tree -> source dir containing Android.bp
  local d="$1" i
  for i in 1 2 3 4; do
    if [ -f "$d/Android.bp" ]; then echo "$d"; return; fi
    d=$(dirname "$d")
    [ "$d" = "." ] && break
  done
  echo ""
}

echo "### resource-bearing framework modules -> AAR"
RESLIST=/tmp/res_mods.txt
: > "$RESLIST"
while IFS= read -r j; do
  case "$j" in
    */frameworks/*)
      base=${j%%/android_common*}
      [ -f "$base/android_common/R.txt" ] && echo "$base" >> "$RESLIST"
      ;;
  esac
done < "$CP"
sort -u -o "$RESLIST" "$RESLIST"

while read -r mdir; do
  rel="${mdir#out/soong/.intermediates/}"
  if [ ! -s "$mdir/android_common/R.txt" ]; then
    key=$(echo "$rel" | tr '/' '_')
    merge_jars "$mdir" "$STAGE/jars/$key.jar"
    echo "JAR(empty-R) $rel"
    continue
  fi
  # R class package: search any R.jar, else manifest
  pkg=""
  rjar=$(find "$mdir" -name 'R.jar' 2>/dev/null | head -1)
  if [ -n "$rjar" ]; then
    pkg=$(unzip -l "$rjar" 2>/dev/null | grep -oE '[^ ]+/R\.class' | head -1 | sed 's#/R\.class##; s#/#.#g')
  fi
  srcdir=$(find_srcdir "$rel")
  if [ -z "$pkg" ] && [ -n "$srcdir" ] && [ -f "$srcdir/AndroidManifest.xml" ]; then
    pkg=$(grep -m1 -oE 'package="[^"]+"' "$srcdir/AndroidManifest.xml" 2>/dev/null | sed 's/package="//; s/"//')
  fi
  if [ -z "$pkg" ]; then
    mf=$(find "$mdir" -path '*manifest_fixer*' -name 'AndroidManifest.xml' 2>/dev/null | head -1)
    [ -n "$mf" ] && pkg=$(grep -m1 -oE 'package="[^"]+"' "$mf" 2>/dev/null | sed 's/package="//; s/"//')
  fi
  if [ -z "$pkg" ]; then
    key=$(echo "$rel" | tr '/' '_')
    merge_jars "$mdir" "$STAGE/jars/$key.jar"
    echo "JAR(no-pkg) $rel"
    continue
  fi
  tmp=$(mktemp -d)
  merge_jars "$mdir" "$tmp/classes.jar"
  printf '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="%s" />\n' "$pkg" > "$tmp/AndroidManifest.xml"
  cp "$mdir/android_common/R.txt" "$tmp/R.txt"
  resdirs=""
  [ -n "$srcdir" ] && [ -f "$srcdir/Android.bp" ] && \
    resdirs=$(awk '/resource_dirs:/{f=1} f{print} f&&/\]/{exit}' "$srcdir/Android.bp" | grep -oE '"[^"]+"' | tr -d '"')
  [ -z "$resdirs" ] && [ -n "$srcdir" ] && [ -d "$srcdir/res" ] && resdirs="res"
  mkdir -p "$tmp/res"
  for r in $resdirs; do
    if [ -d "$srcdir/$r" ]; then cp -r "$srcdir/$r/." "$tmp/res/"; fi
  done
  (cd "$tmp" && zip -qr "$STAGE/aars/systemui_${pkg}.aar" .)
  echo "AAR $pkg <- $rel (src: $srcdir, res: $resdirs)"
  rm -rf "$tmp"
done < "$RESLIST"

echo "### other classpath entries"
declare -A SEEN
while IFS= read -r j; do
  rel="${j#out/soong/.intermediates/}"
  case "$rel" in
    packages/apps/Launcher3/*) continue ;;
    frameworks/base/framework/*|frameworks/base/framework-minus-apex/*) continue ;;
    prebuilts/sdk/current/androidx/m2repository/*)
      moddir="${rel%%/android_common*}"
      vdir="$(dirname "$moddir")"
      ver="$(basename "$vdir")"
      art="$(basename "$(dirname "$vdir")")"
      aar="$vdir/$art-$ver.aar"
      key="aar_$(echo "$moddir" | tr '/' '_')"
      [ -n "${SEEN[$key]:-}" ] && continue; SEEN[$key]=1
      if [ -f "$aar" ]; then cp "$aar" "$STAGE/aars/androidx_$art-$ver.aar"; else cp "$j" "$STAGE/jars/androidx_$(echo "$moddir" | tr '/' '_').jar"; fi
      ;;
    prebuilts/sdk/current/extras/material-design-x/*)
      key="material"; [ -n "${SEEN[$key]:-}" ] && continue; SEEN[$key]=1
      cp prebuilts/sdk/current/extras/material-design-x/com/google/android/material/material/1.14.0-alpha99/material-1.14.0-alpha99.aar "$STAGE/aars/material-1.14.0-alpha99.aar"
      ;;
    *)
      mrel="${rel%%/android_common*}"
      if [ "$mrel" = "$rel" ]; then
        cp "$j" "$STAGE/jars/$(echo "$rel" | tr '/' '_')" 2>/dev/null || true
        continue
      fi
      key=$(echo "$mrel" | tr '/' '_')
      [ -n "${SEEN[$key]:-}" ] && continue; SEEN[$key]=1
      # resource-bearing modules are exported as AARs by the resource loop above
      if grep -qx "out/soong/.intermediates/$mrel" "$RESLIST"; then continue; fi
      case "$rel" in
        *.stubs.*) cp "$j" "$STAGE/jars/$key.jar" ;;
        *) merge_jars "out/soong/.intermediates/$mrel" "$STAGE/jars/$key.jar"
           [ -f "$STAGE/jars/$key.jar" ] || cp "$j" "$STAGE/jars/$key.jar" ;;
      esac
      ;;
  esac
done < "$CP"

echo "### summary"
echo "jars: $(ls "$STAGE/jars" | wc -l)"
echo "aars: $(ls "$STAGE/aars" | wc -l)"
du -sh "$STAGE"
ls "$STAGE/aars" | sort
