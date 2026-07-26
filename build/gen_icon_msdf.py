import json
import subprocess
from pathlib import Path

fonts = Path(r"C:\AstrumPasta\1.21.11\src\main\resources\assets\pasta\fonts")
exe = Path(r"C:\AstrumPasta\1.21.11\build\msdf-atlas-gen\1.4\msdf-atlas-gen.exe")

jobs = [
    ("clickgui-icons", "abcdefghijklmnopqrstuvwxyz"),
    ("notification-icons", "abcdefghijklmnopqrstuvwxyz"),
    ("watermark-icons", "!\"#$%&'()*+,-./01234567"),
]

for out, chars in jobs:
    charset = fonts / f"_charset_{out}.txt"
    charset.write_text('"' + chars + '"\n', encoding="utf-8")
    cmd = [
        str(exe),
        "-font", str(fonts / f"{out}.ttf"),
        "-type", "msdf",
        "-format", "png",
        "-imageout", str(fonts / f"{out}.png"),
        "-json", str(fonts / f"{out}.json"),
        "-size", "64",
        "-pxrange", "10",
        "-yorigin", "bottom",
        "-charset", str(charset),
    ]
    print("GEN", out)
    r = subprocess.run(cmd, cwd=fonts)
    print("exit", r.returncode)
    charset.unlink(missing_ok=True)
    if r.returncode == 0:
        j = json.loads((fonts / f"{out}.json").read_text(encoding="utf-8"))
        print(out, "glyphs", len(j["glyphs"]), "atlas", j["atlas"]["width"])
