"""Apply deterministic fixes to the extracted prebuilts so Gradle/AGP can consume them.

Run after a fresh sync of `prebuilts/` from the ROM build.

Fixes:
1. AAR metadata: drop `forceCompileSdkPreview` / `minCompileSdkPreview` requirements.
   The AOSP prebuilt AARs were built against a preview SDK codename ("Baklava")
   while this project compiles with the stable compileSdk 37.
2. AAR resources: replace `@androidprv:` with `@*android:` and strip
   `android:featureFlag` attributes, because AGP's resource merger drops the
   private android namespace declaration and aapt2 cannot resolve the flag ids.
3. kotlinx-coroutines core jar: strip bundled Kotlin stdlib / annotations classes
   that duplicate org.jetbrains.kotlin:kotlin-stdlib.
4. launcher_log_protos_lite.jar: drop LauncherAtomExtensions* which are
   regenerated (superset) by launcher_quickstep_log_protos_lite.jar.
5. Remove jars that are superseded by AARs built by build_extra_aars.sh.
6. Remove merged jars of resource-bearing modules (they are shipped as AARs).
"""

import os
import re
import shutil
import sys
import zipfile

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PREBUILTS = os.path.join(REPO, "gradle-modules", "prebuilts")
AARS = os.path.join(PREBUILTS, "aars")
JARS = os.path.join(PREBUILTS, "jars")

PRV = "@androidprv:"
FEATURE_FLAG = re.compile(r'\s+(?:[A-Za-z0-9_]+:)?featureFlag="[^"]*"')
PREVIEW_KEYS = ("forceCompileSdkPreview=", "minCompileSdkPreview=")

# Resource-bearing modules that must be consumed as AARs (systemui_*.aar) instead
# of merged jars. The remote extract script skips these, but older stages may
# still contain them.
SUPERSEDED_JARS = [
    "external_lottie_lottie.jar",
    "android.app.appfunctions.exported-flags-aconfig-java.jar",
    "com.android.window.flags.window-aconfig-java.jar",
    "frameworks_libs_systemui_animationlib_animationlib.jar",
    "frameworks_base_packages_SystemUI_compose_scene_PlatformComposeSceneTransitionLayout.jar",
    "frameworks_libs_systemui_ace_src_com_android_personalcontext_ace_client_personalcontext_ace_client.jar",
    "frameworks_base_libs_WindowManager_Shell_shared_WindowManager-Shell-shared.jar",
    "frameworks_libs_systemui_iconloaderlib_iconloader_base.jar",
    "frameworks_base_packages_SystemUI_shared_SystemUISharedLib.jar",
    "frameworks_base_packages_SystemUI_shared_biometrics_BiometricsSharedLib.jar",
    "frameworks_base_packages_SystemUI_animation_PlatformAnimationLib.jar",
    "frameworks_base_packages_SettingsLib_SettingsTheme_SettingsLibSettingsTheme.jar",
]

COROUTINES_JAR = "external_kotlinx.coroutines_kotlinx_coroutines.jar"
PROTO_JAR = "launcher_log_protos_lite.jar"


def rewrite_zip(path, transform):
    with zipfile.ZipFile(path) as z:
        items = z.infolist()
        contents = {i.filename: z.read(i.filename) for i in items}
    changed = False
    for name in list(contents):
        new = transform(name, contents[name])
        if new is not None and new != contents[name]:
            contents[name] = new
            changed = True
    if not changed:
        return False
    tmp = path + ".tmp"
    with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zo:
        for i in items:
            zo.writestr(i, contents[i.filename])
    shutil.move(tmp, path)
    return True


def patch_aar(path):
    def transform(name, data):
        if name == "META-INF/com/android/build/gradle/aar-metadata.properties":
            text = data.decode("utf-8")
            new = "".join(
                line for line in text.splitlines(True) if not line.startswith(PREVIEW_KEYS)
            )
            return new.encode("utf-8") if new != text else None
        if name.startswith("res/") and name.endswith(".xml"):
            try:
                text = data.decode("utf-8")
            except UnicodeDecodeError:
                return None
            new = text.replace(PRV, "@*android:")
            new, _ = FEATURE_FLAG.subn("", new)
            return new.encode("utf-8") if new != text else None
        return None

    return rewrite_zip(path, transform)


def patch_jars():
    coroutines = os.path.join(JARS, COROUTINES_JAR)
    if os.path.exists(coroutines):
        def strip_kotlin(name, _data):
            if name.startswith(("kotlin/", "org/intellij/", "org/jetbrains/")):
                return b"__REMOVE__"
            return None

        # rewrite_zip cannot drop entries via transform, do it explicitly
        with zipfile.ZipFile(coroutines) as z:
            items = z.infolist()
            kept = [
                (i, z.read(i.filename))
                for i in items
                if not i.filename.startswith(("kotlin/", "org/intellij/", "org/jetbrains/"))
            ]
        tmp = coroutines + ".tmp"
        with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zo:
            for item, data in kept:
                zo.writestr(item, data)
        shutil.move(tmp, coroutines)
        print("stripped bundled stdlib from", COROUTINES_JAR)

    proto = os.path.join(JARS, PROTO_JAR)
    if os.path.exists(proto):
        with zipfile.ZipFile(proto) as z:
            items = z.infolist()
            kept = [
                (i, z.read(i.filename))
                for i in items
                if not i.filename.startswith(
                    "com/android/launcher3/logger/LauncherAtomExtensions"
                )
            ]
        tmp = proto + ".tmp"
        with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zo:
            for item, data in kept:
                zo.writestr(item, data)
        shutil.move(tmp, proto)
        print("stripped LauncherAtomExtensions from", PROTO_JAR)


def main():
    patched = 0
    for name in sorted(os.listdir(AARS)):
        if not name.endswith(".aar"):
            continue
        if patch_aar(os.path.join(AARS, name)):
            patched += 1
            print("patched AAR", name)
    print(f"AARs patched: {patched}")

    for name in SUPERSEDED_JARS:
        path = os.path.join(JARS, name)
        if os.path.exists(path):
            os.remove(path)
            print("removed superseded jar", name)

    patch_jars()
    print("postprocess done")


if __name__ == "__main__":
    main()
