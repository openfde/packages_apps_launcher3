#!/bin/bash
set -u
cd /aosp4/hy_fde17
OUTDIR=/tmp/extra_aars
rm -rf "$OUTDIR"; mkdir -p "$OUTDIR"
JAR=prebuilts/jdk/jdk21/linux-x86/bin/jar
[ -x "$JAR" ] || JAR=prebuilts/jdk/jdk21/bin/jar

merge_jars() {
  local mdir="$1" out="$2" tmp found
  tmp=$(mktemp -d); found=0
  while IFS= read -r j; do
    (cd "$tmp" && unzip -oq "/aosp4/hy_fde17/$j" 2>/dev/null) && found=1
  done < <(find "$mdir" -name '*.jar' 2>/dev/null \
     | grep -v -E '/(busybox|kapt|ksp|kotlin_headers|hiddenapi|stubs|turbine|turbine-apt|anno|local-javac-header|local-combined)/' \
     | grep -v -E '(stubs\.jar|\.1\.jar$|\.0\.jar$|R\.jar|src\.jar|sources\.jar|\.rsp)$')
  if [ "$found" = 1 ]; then (cd "$tmp" && zip -qr "$out" .); fi
  rm -rf "$tmp"
}

find_srcdir() {
  local d="$1" i
  for i in 1 2 3 4; do
    [ -f "$d/Android.bp" ] && { echo "$d"; return; }
    d=$(dirname "$d"); [ "$d" = "." ] && break
  done
  echo ""
}

make_aar() { # $1 mdir(rel to tree) $2 srcdir $3 pkg $4 outname
  local mdir="$1" srcdir="$2" pkg="$3" outname="$4" tmp
  tmp=$(mktemp -d)
  merge_jars "out/soong/.intermediates/$mdir" "$tmp/classes.jar"
  if [ ! -f "$tmp/classes.jar" ]; then
    printf 'Manifest-Version: 1.0\n' > "$tmp/mf.txt"
    (cd "$tmp" && "$JAR" cfm classes.jar mf.txt >/dev/null 2>&1) || true
  fi
  cp "out/soong/.intermediates/$mdir/android_common/R.txt" "$tmp/R.txt" 2>/dev/null || true
  printf '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="%s" />\n' "$pkg" > "$tmp/AndroidManifest.xml"
  local resdirs=""
  [ -n "$srcdir" ] && [ -f "$srcdir/Android.bp" ] && \
    resdirs=$(awk '/resource_dirs:/{f=1} f{print} f&&/\]/{exit}' "$srcdir/Android.bp" | grep -oE '"[^"]+"' | tr -d '"')
  [ -z "$resdirs" ] && [ -n "$srcdir" ] && [ -d "$srcdir/res" ] && resdirs="res"
  mkdir -p "$tmp/res"
  for r in $resdirs; do
    [ -d "$srcdir/$r" ] && cp -r "$srcdir/$r/." "$tmp/res/"
  done
  (cd "$tmp" && zip -qr "$OUTDIR/$outname" .)
  echo "built $outname (res: $resdirs)"
  rm -rf "$tmp"
}

echo "=== dynamiccolors ==="
DC=out/soong/.intermediates/frameworks/libs/systemui/dynamiccolors/dynamiccolors
DCSRC=frameworks/libs/systemui/dynamiccolors
DCPKG=$(grep -m1 -oE 'package="[^"]+"' "$DCSRC/AndroidManifest.xml" | sed 's/package="//; s/"//')
echo "dynamiccolors pkg=$DCPKG"
TMP=$(mktemp -d)
printf 'Manifest-Version: 1.0\n' > "$TMP/mf.txt"
(cd "$TMP" && "$JAR" cfm classes.jar mf.txt >/dev/null 2>&1) || true
cp "$DC/android_common/R.txt" "$TMP/R.txt"
cp "$DCSRC/AndroidManifest.xml" "$TMP/AndroidManifest.xml"
mkdir -p "$TMP/res"; cp -r "$DCSRC/res/." "$TMP/res/"
(cd "$TMP" && zip -qr "$OUTDIR/dynamiccolors.aar" .)
echo "built dynamiccolors.aar"
rm -rf "$TMP"

echo "=== lottie ==="
LM=external/lottie/lottie
LSRC=$(find_srcdir "$LM")
echo "lottie srcdir=$LSRC"
LPKG=$(unzip -l "out/soong/.intermediates/$LM/android_common/busybox/R.jar" 2>/dev/null | grep -oE '[^ ]+/R\.class' | head -1 | sed 's#/R\.class##; s#/#.#g')
echo "lottie pkg=$LPKG"
make_aar "$LM" "$LSRC" "$LPKG" "lottie.aar"

echo "=== result ==="
ls -la "$OUTDIR"
