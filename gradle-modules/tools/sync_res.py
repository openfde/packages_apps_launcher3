import os
import re
import shutil
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(REPO, "quickstep", "res")
DST = os.path.join(REPO, "gradle-modules", "generated", "quickstep-res")

FEATURE_FLAG = re.compile(r'\s+android:featureFlag="[^"]*"')
PRV_REF = "@androidprv:"


def sync() -> None:
    if os.path.isdir(DST):
        shutil.rmtree(DST)
    patched_files = 0
    copied = 0
    for root, _dirs, files in os.walk(SRC):
        rel = os.path.relpath(root, SRC)
        target_root = os.path.join(DST, rel) if rel != "." else DST
        os.makedirs(target_root, exist_ok=True)
        for name in files:
            src = os.path.join(root, name)
            dst = os.path.join(target_root, name)
            if name.endswith(".xml"):
                with open(src, "r", encoding="utf-8") as fh:
                    text = fh.read()
                new = text.replace(PRV_REF, "@*android:")
                new, n = FEATURE_FLAG.subn("", new)
                if new != text:
                    patched_files += 1
                with open(dst, "w", encoding="utf-8", newline="") as fh:
                    fh.write(new)
            else:
                shutil.copy2(src, dst)
            copied += 1
    print(f"copied {copied} files, patched {patched_files}")


if __name__ == "__main__":
    sync()
