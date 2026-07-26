import json
import subprocess
from pathlib import Path

fonts = Path(r"C:\AstrumPasta\1.21.11\src\main\resources\assets\pasta\fonts")
exe = Path(r"C:\AstrumPasta\1.21.11\build\msdf-atlas-gen\1.4\msdf-atlas-gen.exe")
out = "watermark-icons"
charset = fonts / f"_charset_{out}.txt"
# Range of printable symbols used by the font
charset.write_text("['!', '7']\n", encoding="utf-8")
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
