# dte:A3
"""Approved poster strings, read from specs/copy-strings.md (the owner's exact wording)."""
import re
from functools import lru_cache
from pathlib import Path

SPEC = Path(__file__).resolve().parents[2] / "specs/copy-strings.md"
NOTE = re.compile(r"\s*\[(Requires [^\]]+)\]\s*")


@lru_cache(maxsize=None)
def _allowed():
	"""{mod section: [string, ...]} from each section's "Allowed" bullets."""
	out, mod, sub = {}, None, None
	for line in SPEC.read_text().splitlines():
		if line.startswith("## "):
			mod, sub = line[3:].strip(), None
		elif line.startswith("### "):
			sub = line[4:].strip()
		elif mod and sub == "Allowed" and line.startswith("- "):
			out.setdefault(mod, []).append(line[2:].strip())
	return out


def line(mod, key):
	"""The one allowed string of that mod containing key, split into (text, requirement note or None)."""
	hits = [s for s in _allowed()[mod] if key in s]
	if len(hits) != 1:
		raise SystemExit(f"copy: {len(hits)} allowed strings in '{mod}' match '{key}'")
	s = hits[0]
	m = NOTE.search(s)
	if not m:
		return s, None
	return (s[:m.start()] + " " + s[m.end():]).strip(), "[" + m.group(1) + "]"
