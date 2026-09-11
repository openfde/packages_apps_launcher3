"""Merge the AOSP framework classes + framework-res resources into the SDK android.jar.

AGP/javac resolve android.* from the compileSdk android.jar only, so hidden platform
classes (e.g. android.widget.TextClock.ClockEventDelegate) and private framework
resources are invisible unless they are part of that jar. This script rebuilds
platforms/android-37.0/android.jar as:
    framework.jar classes  (authoritative platform classes, hidden APIs)
  + SDK android.jar classes (public extras not present in framework.jar)
  + framework-res.apk resources (resources.arsc + res/**, private resources)

The original android.jar is kept as android.jar.gradle-backup.
"""

import os
import shutil
import sys
import zipfile

SDK_JAR = r"D:\huyang\Android\sdk\platforms\android-37.0\android.jar"
FRAMEWORK_JAR = r"D:\code\packages_apps_launcher3\gradle-modules\prebuilts\platform\framework.jar"
FRAMEWORK_TURBINE = (
    r"D:\code\packages_apps_launcher3\gradle-modules\prebuilts\platform\framework-jarjar-turbine.jar"
)
FRAMEWORK_RES = r"D:\code\packages_apps_launcher3\gradle-modules\prebuilts\platform\framework-res.apk"
BACKUP = SDK_JAR + ".gradle-backup"


def main() -> None:
    if not os.path.exists(BACKUP):
        shutil.copy2(SDK_JAR, BACKUP)
        print("backup created:", BACKUP)
    tmp = SDK_JAR + ".tmp"
    seen = set()
    with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as out:
        # 1. relocated platform ABI jar (hidden aconfig classes at their real packages)
        with zipfile.ZipFile(FRAMEWORK_TURBINE) as tz:
            for item in tz.infolist():
                name = item.filename
                if name.startswith("META-INF/") or name.endswith("/") or name in seen:
                    continue
                seen.add(name)
                out.writestr(item, tz.read(name))
        # 2. full framework.jar classes not already present
        with zipfile.ZipFile(FRAMEWORK_JAR) as fz:
            for item in fz.infolist():
                name = item.filename
                if name.startswith("META-INF/") or name.endswith("/") or name in seen:
                    continue
                seen.add(name)
                out.writestr(item, fz.read(name))
        # 3. SDK android.jar classes not already present
        with zipfile.ZipFile(BACKUP) as sz:
            for item in sz.infolist():
                name = item.filename
                if name.startswith("META-INF/") or name.endswith("/"):
                    continue
                if name in ("resources.arsc", "AndroidManifest.xml") or name.startswith("res/"):
                    continue
                if name in seen:
                    continue
                seen.add(name)
                out.writestr(item, sz.read(name))
        # 4. framework-res resources
        with zipfile.ZipFile(FRAMEWORK_RES) as rz:
            for item in rz.infolist():
                name = item.filename
                if name.startswith("META-INF/") or name.endswith("/"):
                    continue
                if not (name == "resources.arsc" or name == "AndroidManifest.xml" or name.startswith("res/")):
                    continue
                if name in seen:
                    continue
                seen.add(name)
                out.writestr(item, rz.read(name))
    shutil.move(tmp, SDK_JAR)
    print("merged android.jar written:", os.path.getsize(SDK_JAR), "bytes")


if __name__ == "__main__":
    main()
