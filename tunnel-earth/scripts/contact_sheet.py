#!/usr/bin/env python3
"""Render review stills (with the safe-zone guides) and tile them into one contact sheet.

  python3 scripts/contact_sheet.py            # middle of every scene + key moments
  python3 scripts/contact_sheet.py 0 45 300   # specific frames
Output: review/frames/*.png (full size) and review/contact_sheet.png
"""
import json, os, shutil, subprocess, sys
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
T = json.load(open(os.path.join(ROOT, "src", "data", "timing.json"), encoding="utf-8"))
FPS = T["fps"]
OUT = os.path.join(ROOT, "review")
FR = os.path.join(OUT, "frames")


def default_frames():
    picks = []
    for s in T["scenes"]:
        a, n = s["startFrame"], s["durationFrames"]
        for q, name in ((0.25, "q1"), (0.5, "mid"), (0.75, "q3")):
            picks.append((a + int(n * q), f"S{s['id']} {name}"))
    sc = {s["id"]: s for s in T["scenes"]}
    extra = [
        (0, "frame 0"),
        (sc[1]["startFrame"] + 40, "S1 drilling"),
        (sc[3]["startFrame"] + sc[3]["durationFrames"] - 8, "S3 end"),
        (sc[6]["startFrame"] + sc[6]["durationFrames"] - 10, "S6 arrival"),
        (sc[7]["startFrame"] + 20, "S7 early"),
        (T["totalFrames"] - 1, "last frame"),
    ]
    picks += extra
    return sorted(set(picks))


def main():
    picks = [(int(x), f"f{x}") for x in sys.argv[1:]] or default_frames()
    frames = sorted({p[0] for p in picks})
    shutil.rmtree(FR, ignore_errors=True)
    os.makedirs(FR, exist_ok=True)
    subprocess.run(
        ["npx", "remotion", "render", "TunnelEarthGuides", os.path.join(FR, "f"),
         f"--frames={','.join(map(str, frames))}", "--image-format=png", "--log=error"],
        cwd=ROOT, check=True,
    )
    # Remotion writes <dir>/f/element-<frame>.png
    rendered = {}
    for dirpath, _, names in os.walk(FR):
        for name in names:
            digits = "".join(ch for ch in name if ch.isdigit())
            if digits and name.endswith(".png"):
                rendered[int(digits)] = os.path.join(dirpath, name)
    cols, per_page = 4, 8
    tw, th = 360, 640
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 22)
    except OSError:
        font = ImageFont.load_default()
    for old in os.listdir(OUT):
        if old.startswith("contact_sheet"):
            os.remove(os.path.join(OUT, old))
    pages = [picks[i:i + per_page] for i in range(0, len(picks), per_page)]
    for pi, page in enumerate(pages, 1):
        rows = (len(page) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * tw, rows * (th + 34)), (20, 20, 20))
        draw = ImageDraw.Draw(sheet)
        for i, (fr, label) in enumerate(page):
            path = rendered.get(fr)
            if not path:
                continue
            im = Image.open(path).convert("RGB")
            x, y = (i % cols) * tw, (i // cols) * (th + 34)
            sheet.paste(im.resize((tw, th), Image.LANCZOS), (x, y + 34))
            draw.text((x + 8, y + 5), f"{label}  #{fr}  {fr / FPS:.2f}s", fill=(255, 230, 120), font=font)
            im.save(os.path.join(FR, f"{fr:04d}_{label.replace(' ', '_')}.png"))
        out = os.path.join(OUT, f"contact_sheet_{pi}.png")
        sheet.save(out)
        print("contact sheet:", out, f"({len(page)} stills)")
    shutil.rmtree(os.path.join(FR, "f"), ignore_errors=True)


if __name__ == "__main__":
    main()
