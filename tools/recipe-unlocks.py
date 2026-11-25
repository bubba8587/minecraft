#!/usr/bin/env python3
"""Recipe book unlock advancements for every recipe-book recipe in our mods and datapacks.

Writes data/<ns>/advancement/recipes/<category>/<recipe>.json next to each recipe, vanilla style: the recipe unlocks
when the player holds any of its ingredients (sticks ignored unless that is all there is). Recipe types the recipe
book never shows (brewing, our custom types) are skipped. Generated files carry no hand edits: rerun after changing
recipes. --check exits 1 when a file is missing or stale (for CI).

Usage: python3 tools/recipe-unlocks.py [--check]
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BOOK_TYPES = {
	"minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:crafting_transmute",
	"minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking",
	"minecraft:smithing_transform", "minecraft:stonecutting",
}
FILLER = {"minecraft:stick"}
INTERNAL = {"refmod", "refpack"}


def data_roots():
	for mod in sorted((ROOT / "mods").iterdir()):
		if mod.name not in INTERNAL and (mod / "src/main/resources/data").is_dir():
			yield mod / "src/main/resources/data"
	for pack in sorted((ROOT / "datapacks").iterdir()):
		if pack.name not in INTERNAL and (pack / "data").is_dir():
			yield pack / "data"


def norm(ref):
	return ref if ref.startswith("#") or ":" in ref else "minecraft:" + ref


def ingredients(recipe):
	"""Each ingredient as a list of item ids or tags."""
	t = recipe["type"]
	raw = []
	if t == "minecraft:crafting_shaped":
		used = set("".join(recipe["pattern"]))
		raw = [v for k, v in recipe["key"].items() if k in used]
	elif t == "minecraft:crafting_shapeless":
		raw = recipe["ingredients"]
	elif t == "minecraft:crafting_transmute":
		raw = [recipe["input"], recipe["material"]]
	elif t == "minecraft:smithing_transform":
		raw = [recipe["base"], recipe["addition"]]
	else:
		raw = [recipe["ingredient"]]
	out = []
	for r in raw:
		out.append([norm(x) for x in (r if isinstance(r, list) else [r])])
	return out


def criterion_items(alternatives):
	"""Item predicates for one ingredient: tags each alone, plain ids together."""
	ids = [a for a in alternatives if not a.startswith("#")]
	preds = [a for a in alternatives if a.startswith("#")]
	if ids:
		preds.append(ids[0] if len(ids) == 1 else ids)
	return preds


def advancement(recipe_id, recipe):
	ings = ingredients(recipe)
	kept = [i for i in ings if not set(i) <= FILLER] or ings
	criteria = {}
	for ing in kept:
		for pred in criterion_items(ing):
			first = pred if isinstance(pred, str) else pred[0]
			name = "has_" + first.lstrip("#").split(":", 1)[1].replace("/", "_")
			if name not in criteria:
				criteria[name] = {"conditions": {"items": [{"items": pred}]}, "trigger": "minecraft:inventory_changed"}
	criteria["has_the_recipe"] = {"conditions": {"recipes": recipe_id}, "trigger": "minecraft:recipe_unlocked"}
	return {
		"parent": "minecraft:recipes/root",
		"criteria": criteria,
		"requirements": [list(criteria)],
		"rewards": {"recipes": [recipe_id]},
	}


def main():
	check = "--check" in sys.argv
	stale = []
	written = 0
	for data in data_roots():
		for ns_dir in sorted(p for p in data.iterdir() if p.is_dir()):
			for f in sorted((ns_dir / "recipe").rglob("*.json")) if (ns_dir / "recipe").is_dir() else []:
				recipe = json.loads(f.read_text(encoding="utf-8"))
				if recipe.get("type") not in BOOK_TYPES:
					continue
				path = f.relative_to(ns_dir / "recipe").with_suffix("").as_posix()
				recipe_id = f"{ns_dir.name}:{path}"
				category = recipe.get("category", "misc")
				out = ns_dir / "advancement/recipes" / category / (path + ".json")
				text = json.dumps(advancement(recipe_id, recipe), indent="\t") + "\n"
				if out.exists() and out.read_text(encoding="utf-8") == text:
					continue
				if check:
					stale.append(out.relative_to(ROOT).as_posix())
					continue
				out.parent.mkdir(parents=True, exist_ok=True)
				out.write_text(text, encoding="utf-8", newline="\n")
				written += 1
	if check:
		for s in stale:
			print("stale or missing:", s)
		sys.exit(1 if stale else 0)
	print(f"wrote {written} recipe unlock advancement(s)")


if __name__ == "__main__":
	main()
