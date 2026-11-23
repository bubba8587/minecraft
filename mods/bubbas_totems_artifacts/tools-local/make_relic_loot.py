#!/usr/bin/env python3
# dte:C6
"""Writes the relic vault chest loot tables (specs/totems-artifacts.md, "Relic vaults"): one per vault kind, from the
numbers below, so the three stay in step. Infused Gems loot is added in code (relic/RelicLoot) only with that mod.

  python3 mods/bubbas_totems_artifacts/tools-local/make_relic_loot.py
"""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/bubbas_totems_artifacts/loot_table"
NS = "bubbas_totems_artifacts"

TOTEMS = {"totem_of_keeping": 3, "totem_of_gathering": 3, "totem_of_fortification": 3, "totem_of_growth": 3,
	"totem_of_seeking": 3, "totem_of_herding": 3, "totem_of_clear_skies": 3, "totem_of_warding": 2, "grave_talisman": 2,
	"totem_of_the_beginning": 1, "totem_of_the_end": 1}

# kind: treasure rolls, totem chance per chest, shred rolls, rare chance per chest
KINDS = {
	"relic_vault_grand": ((4, 7), 0.45, (2, 4), 0.40),
	"relic_vault_small": ((3, 5), 0.30, (1, 3), 0.15),
	"relic_vault_crypt": ((2, 4), 0.20, (1, 2), 0.0),
}


def item(name, weight, lo=None, hi=None, extra=None):
	e = {"type": "minecraft:item", "name": name, "weight": weight}
	mods = []
	if lo is not None:
		mods.append({"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
	if extra:
		mods.append(extra)
	if mods:
		e["modifier"] = mods if len(mods) > 1 else mods[0]
	return e


def enchanted(levels):
	return {"type": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": levels[0], "max": levels[1]},
		"options": "#minecraft:on_random_loot"}


def table(name, treasure, totem, shreds, rare):
	grand = name.endswith("grand")
	lv = (25, 39) if grand else (15, 30)
	pools = [
		{"rolls": {"type": "minecraft:uniform", "min": treasure[0], "max": treasure[1]}, "entries": [
			item("minecraft:gold_ingot", 12, 2, 7),
			item("minecraft:iron_ingot", 10, 3, 9),
			item("minecraft:emerald", 10, 2, 8),
			item("minecraft:diamond", 8 if grand else 5, 1, 4 if grand else 2),
			item("minecraft:lapis_lazuli", 6, 4, 12),
			item("minecraft:experience_bottle", 6, 2, 6),
			item("minecraft:golden_apple", 5, 1, 2),
			item("minecraft:name_tag", 3),
			item("minecraft:book", 6, extra=enchanted(lv)),
			item("minecraft:diamond_sword" if grand else "minecraft:iron_sword", 3, extra=enchanted(lv)),
			item("minecraft:diamond_chestplate" if grand else "minecraft:iron_chestplate", 3, extra=enchanted(lv)),
		]},
		{"rolls": {"type": "minecraft:uniform", "min": shreds[0], "max": shreds[1]}, "entries": [
			{"type": "minecraft:item", "name": f"{NS}:relic_map_shred", "modifier": {"type": f"{NS}:random_relic_shred"}}]},
		{"rolls": 1, "condition": {"type": "minecraft:random_chance", "chance": totem},
			"entries": [{"type": "minecraft:item", "name": f"{NS}:{t}", "weight": w} for t, w in TOTEMS.items()]},
	]
	if rare:
		pools.append({"rolls": 1, "condition": {"type": "minecraft:random_chance", "chance": rare}, "entries": [
			item("minecraft:enchanted_golden_apple", 2), item("minecraft:netherite_scrap", 3, 1, 2), item("minecraft:totem_of_undying", 2),
			item("minecraft:music_disc_pigstep", 1), item("minecraft:heart_of_the_sea", 1)]})
	return {"type": "minecraft:chest", "pools": pools, "random_sequence": f"{NS}:chests/{name}"}


SOCKET = {"type": "bubbas_infused_gems:socket_random_gems", "count": 1, "infusion": [2, 2, 3, 3], "tier": [1, 2, 2], "variant": "normal"}


def guard_gear():
	return {"fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": ["bubbas_infused_gems"]}],
		"type": "minecraft:empty", "pools": [
			{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:iron_chestplate", "modifier": SOCKET}]},
			{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:iron_sword", "modifier": dict(SOCKET, prefer_harmful=0.75)}]},
		]}


# kind: (weapon, weight) entries, enchanting levels, Infused Gems sockets (count, infusion, tier)
PRIME_WEAPONS = {
	"grand": ([("minecraft:netherite_sword", 3), ("minecraft:netherite_axe", 2)], (30, 39), (2, [3, 3, 4], [2, 3, 3])),
	"small": ([("minecraft:diamond_sword", 3), ("minecraft:diamond_axe", 2)], (20, 30), (1, [2, 3, 3], [1, 2, 2])),
	"crypt": ([("minecraft:diamond_sword", 1), ("minecraft:diamond_axe", 1)], (15, 25), (1, [2, 2, 3], [1, 2])),
}


def prime_weapon(kind, gems):
	weapons, levels, (count, infusion, tier) = PRIME_WEAPONS[kind]
	mods = [enchanted(levels)]
	if gems:
		mods.append({"type": "bubbas_infused_gems:socket_random_gems", "count": count, "gear": "weapon", "infusion": infusion, "tier": tier,
			"variant": "normal", "prefer_harmful": 0.75})
	entries = [{"type": "minecraft:item", "name": w, "weight": n, "modifier": mods if len(mods) > 1 else mods[0]} for w, n in weapons]
	table = {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": entries}]}
	if gems:
		table = {"fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": ["bubbas_infused_gems"]}], **table}
	return table


def main():
	for name, args in KINDS.items():
		path = OUT / "chests" / f"{name}.json"
		path.parent.mkdir(parents=True, exist_ok=True)
		path.write_text(json.dumps(table(name, *args), indent=2) + "\n")
	path = OUT / "relic_vault" / "guard_gear.json"
	path.parent.mkdir(parents=True, exist_ok=True)
	path.write_text(json.dumps(guard_gear(), indent=2) + "\n")
	for kind in PRIME_WEAPONS:
		for gems in (False, True):
			path = OUT / "relic_vault" / f"prime_weapon_{kind}{'_gems' if gems else ''}.json"
			path.write_text(json.dumps(prime_weapon(kind, gems), indent=2) + "\n")


if __name__ == "__main__":
	main()
