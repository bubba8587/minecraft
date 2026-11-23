# dte:A3
"""Contact sheet of poster camera shots: python3 tools/posters/sheet.py <out.png> <glob[,glob...]> [cols] (specs/posters.md)."""
import glob, sys
from pathlib import Path
from PIL import Image, ImageDraw

SHOTS = Path(__file__).resolve().parents[2] / "mods/postershots/build/run/posters/shots"


def sheet(out, pattern, cols=4, w=480):
	fs = [f for p in pattern.split(",") for f in sorted(glob.glob(str(SHOTS / p)))]
	h = w * 9 // 16
	rows = (len(fs) + cols - 1) // cols
	s = Image.new("RGB", (cols * w, rows * (h + 16)), "black")
	d = ImageDraw.Draw(s)
	for i, f in enumerate(fs):
		im = Image.open(f).convert("RGB").resize((w, h))
		x, y = (i % cols) * w, (i // cols) * (h + 16)
		s.paste(im, (x, y))
		d.text((x + 4, y + h + 2), Path(f).stem, fill="white")
	s.save(out)
	print(len(fs), "shots")


if __name__ == "__main__":
	sheet(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 4)
