#!/usr/bin/env python3
# dte:C4
"""Bubba's Infused Gems balance model and table generator (specs/effect-gems.md, "Balance model").

  python tools/gem-balance.py           # run the checks, then write the tables
  python tools/gem-balance.py --check   # run the checks only, write nothing

Writes docs/research/effect-gems-balance.md and build/gem-balance.html (a page for the
owner to open in a browser). Every check failure is printed and exits non-zero, and
nothing is written.

Vanilla numbers are hard-coded below with the 26.3 source file they come from. When the
decompiled source exists (build/mcsrc/26.3/common, made by tools/mcsrc.py) the script
re-reads the potion list and the effect constants from it and fails if any differ, so a
new Minecraft release can't silently change the anchors. Standard library only.
"""
import html, itertools, math, random, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "build" / "mcsrc" / "26.3" / "common" / "net" / "minecraft"
OUT_MD = ROOT / "docs" / "research" / "effect-gems-balance.md"
OUT_HTML = ROOT / "build" / "gem-balance.html"

# ---------------------------------------------------------------- tunable model constants
CAP = 0.95            # best possible total, as a fraction of the strongest vanilla level (armor and weapons)
STACK_R = 0.85        # each further gem of the same effect counts this much of the one before
SLOTS = 3             # gem slots per armor piece (owner)
WEAPON_SLOTS = 2      # gem slots per weapon (owner)
PIECES = ["helmet", "chestplate", "leggings", "boots"]   # adjacency order (owner)
MIN_REL_STEP = 0.04   # every ladder step is at least +4% on the step below
MIN_ABS_DROP = 0.025  # from the best build, any one step down loses at least 0.025 of the scale
EARLY_SINGLE_FLOOR = 0.03       # the weakest possible single gem still does this much
EARLY_BUILD_RANGE = (0.10, 0.30)  # a typical early kit (armor or weapon) lands in this band

# Healing gem (owner): a non-lethal hit or fall gives back a fraction of the health it took.
HEAL_BEST = 0.25      # fraction of each hit returned by the best possible build
HEAL_EXP = 0.7        # heal = HEAL_BEST x (total / CAP)^HEAL_EXP: flatter than linear, so early gems still show
HEAL_EARLY_FLOOR = 0.05  # the early kit returns at least 5% of each hit
HEAL_MIN_DROP = 0.005    # one step down from the best loses at least half a percentage point

# Drawbacks (owner): a bad roll puts a mild harmful effect on the wearer (armor gems), or a mild
# beneficial effect on the target (weapon gems). Fixed size per drawback, whatever the gem or gear.
DRAWBACK_ONE = 0.10   # one drawback = 10% of the strongest vanilla level of that effect
DRAWBACK_CAP = 0.20   # any number of drawbacks of one effect saturate below this
DRAWBACK_HIT_S = 3    # weapon drawback: the target's buff lasts this long, refreshed by each hit

HARM_CHARGE_S = 10    # a Harming weapon gem charges this long between strikes
WEAPON_SPLIT = 0.5    # weapon strength s goes to potency as s^0.5 and duration as s^0.5 (product = s)

# ---------------------------------------------------------------- factor tables
# Base gems, weakest first, by how hard they are to get (owner). Strength only, no traits (owner).
BASE_GEMS = [
	("amethyst", "Amethyst shard", 0.80, "Overworld geodes, first trip underground"),
	("quartz", "Nether quartz", 0.84, "First Nether trip"),
	("ender_pearl", "Ender pearl", 0.88, "Endermen; farmable"),
	("diamond", "Diamond", 0.92, "Deep mining; common in chest loot"),
	("prismarine", "Prismarine shard (owner)", 0.96, "Ocean monuments, a guarded structure (minecraft:prismarine_shard)"),
	("nether_star", "Nether star", 1.00, "The Wither: the best base (owner)"),
]
BASE = {b: f for b, _, f, _ in BASE_GEMS}
BEST_BASE = "nether_star"

# Armor materials (ArmorMaterials.java, 26.3: durability multiplier and chest defense quoted
# for orientation). MAIN_LADDER is the progression the checks hold strictly increasing.
MATERIALS = {
	"leather":   (0.75, "durability x5, chest 3, enchantability 15"),
	"copper":    (0.79, "durability x11, chest 4, enchantability 8 (26.x copper armor)"),
	"chainmail": (0.83, "durability x15, chest 5, enchantability 12"),
	"gold":      (0.83, "durability x7, chest 5, enchantability 25 (side branch: open question)"),
	"iron":      (0.87, "durability x15, chest 6, enchantability 9"),
	"turtle":    (0.87, "helmet only; durability x25, defense 2 like iron's helmet; never part of a run"),
	"diamond":   (0.93, "durability x33, chest 8, toughness 2"),
	"netherite": (1.00, "durability x37, chest 8, toughness 3, knockback resistance 0.1"),
}
MAIN_LADDER = ["leather", "copper", "chainmail", "iron", "diamond", "netherite"]
HELMET_ONLY = {"turtle"}
# Matching run: a piece's factor from the length of the unbroken same-material run it sits in.
RUN = {1: 0.85, 2: 0.90, 3: 0.95, 4: 1.00}

# Weapon material (proposal: yes, a factor, for consistency with armor). ToolMaterial.java, 26.3.
# Swords, axes and spears come in every material; the others have none and get a fixed factor.
WEAPON_MATERIALS = {
	"wood":      (0.75, "durability 59, enchantability 15"),
	"stone":     (0.79, "durability 131, enchantability 5"),
	"copper":    (0.83, "durability 190, enchantability 13 (26.x copper tools)"),
	"gold":      (0.83, "durability 32, enchantability 22 (side branch, like gold armor)"),
	"iron":      (0.87, "durability 250, enchantability 14"),
	"diamond":   (0.93, "durability 1561, enchantability 10"),
	"netherite": (1.00, "durability 2031, enchantability 15"),
}
WEAPON_LADDER = ["wood", "stone", "copper", "iron", "diamond", "netherite"]
WEAPON_FIXED = {  # no material: diamond-level, since they can't be upgraded to netherite (open question)
	"bow":      (0.93, "no material; ranged, keeps its distance"),
	"crossbow": (0.93, "no material; ranged"),
	"trident":  (0.93, "no material; drowned drop, rare"),
	"mace":     (0.93, "no material; heavy core from ominous vaults"),
}
WEAPON_ALL = {**{m: f for m, (f, _) in WEAPON_MATERIALS.items()}, **{w: f for w, (f, _) in WEAPON_FIXED.items()}}

# Enchant tier outcome grades (owner's words: weak / medium / strongest).
GRADES = {"weak": 0.50, "medium": 0.75, "strongest": 1.00}
FORM_MAX_TIER = {"normal": 1, "splash": 2, "lingering": 3}   # owner
# (label, intended-effect grade or None, [(other kind, grade)], proposed odds)
TIERS = {
	1: [("Weak drawback alone", None, [("drawback", "weak")], 0.50),
		("Weak intended effect", "weak", [], 0.50)],
	2: [("Medium intended alone", "medium", [], 0.50),
		("Medium intended + weak other", "medium", [("other", "weak")], 0.50)],
	3: [("Strongest alone", "strongest", [], 0.40),
		("Strongest + weak other", "strongest", [("other", "weak")], 0.30),
		("Strongest + medium other", "strongest", [("other", "medium")], 0.15),
		("Strongest + weak drawback", "strongest", [("drawback", "weak")], 0.15)],
}

def variant_factors(variants):
	"""Potion variant factor by rank among the variants the effect really has.
	Strong (glowstone, higher level) is the big step; long (redstone) a small one."""
	has = [v for v in ("normal", "long", "strong") if v in variants]
	if has == ["normal", "long", "strong"]:
		return {"normal": 0.80, "long": 0.85, "strong": 1.00}
	if has == ["normal", "long"]:
		return {"normal": 0.90, "long": 1.00}
	if has == ["normal", "strong"]:
		return {"normal": 0.80, "strong": 1.00}
	if has == ["normal"]:
		return {"normal": 1.00}
	raise ValueError(f"unexpected variant set {has}")

# ---------------------------------------------------------------- vanilla facts (26.3)
T = 20.0  # ticks per second
TIPPED_ARROW_SCALE = 0.125  # Items.TIPPED_ARROW potion_duration_scale

def jump_apex(v0, g=0.08, drag=0.98):
	"""Apex of a jump: LivingEntity.travelInAir moves, then subtracts gravity, then multiplies by 0.98."""
	y = best = 0.0
	v = v0
	for _ in range(200):
		y += v
		best = max(best, y)
		v = (v - g) * drag
		if v < 0:
			break
	return best

def pct(x, sign=True):
	return f"{'+' if sign and x >= 0 else ''}{x * 100:.1f}%"

def heal_fraction(s):
	return HEAL_BEST * (max(s, 0.0) / CAP) ** HEAL_EXP

def drawback_total(n):
	"""n drawbacks of one effect: the first is DRAWBACK_ONE, the rest saturate toward DRAWBACK_CAP."""
	return DRAWBACK_CAP * (1 - (1 - DRAWBACK_ONE / DRAWBACK_CAP) ** n)

# Each effect: name, potion (Potions.java id), variants {variant: (ticks, amplifier)}, role,
# anchor (what 1.0 is), value numeric (monotonic), show text, impl, src.
# Every gem goes into armor (its effects reach the wearer) or a weapon (they reach what it hits), owner.
# role: main = a beneficial gem (anchor, value and show describe it worn); healing = the beneficial
#       Healing gem (own worn curve); harmful = a harmful gem (anchor, unit and unit_fmt describe one hit);
#       negative = drawback-only (Bad Luck).
# kind (the hit-applied curve): scaled = potency and duration both scale; onoff = the vanilla effect,
#       duration scales; instant = a charged strike. Beneficial effects are scaled unless said otherwise.
# source: ("potion",) brewed from its potion; ("item", text) an item in the bottle slot; ("found", [loot], text) loot only.
# level_one: where vanilla's weakest level sits on the effect's scale (drawbacks stay below it).
E = {}
def effect(key, **kw):
	E[key] = kw

effect("speed", name="Speed", potion="swiftness", role="main", level_one=0.5,
	variants={"normal": (3600, 0), "long": (9600, 0), "strong": (1800, 1)},
	anchor="Speed II: movement speed +40% (0.2 per level, add_multiplied_total)",
	value=lambda s: 0.4 * s, show=lambda s: f"{pct(0.4 * s)} speed",
	impl="Attribute modifier on movement_speed, +0.4 x s, add_multiplied_total (exact).",
	src="MobEffects.SPEED")
effect("strength", name="Strength", potion="strength", role="main",
	variants={"normal": (3600, 0), "long": (9600, 0), "strong": (1800, 1)},
	anchor="Strength II: attack damage +6 (3 per level, add_value)",
	value=lambda s: 6 * s, show=lambda s: f"+{6 * s:.2f} attack damage",
	impl="Attribute modifier on attack_damage, +6 x s, add_value (exact).",
	src="MobEffects.STRENGTH")
effect("jump_boost", name="Jump Boost", potion="leaping", role="main", level_one=0.5,
	variants={"normal": (3600, 0), "long": (9600, 0), "strong": (1800, 1)},
	anchor="Jump Boost II: jump velocity +0.2 (0.1 per level) and safe fall distance +2 (1 per level)",
	value=lambda s: jump_apex(0.42 + 0.2 * s),
	show=lambda s: f"jump {jump_apex(0.42 + 0.2 * s):.2f} blocks (vanilla {jump_apex(0.42):.2f}), safe fall {3 + 2 * s:.1f}",
	impl="Attribute modifiers: jump_strength +0.2 x s and safe_fall_distance +2 x s. (Vanilla adds the jump boost after the block jump factor; the attribute is scaled by it, so it is a little weaker on honey.)",
	src="MobEffects.JUMP_BOOST, LivingEntity.getJumpBoostPower, Attributes.JUMP_STRENGTH 0.42")
effect("regeneration", name="Regeneration", potion="regeneration", role="main", level_one=0.5,
	variants={"normal": (900, 0), "long": (1800, 0), "strong": (450, 1)},
	anchor="Regeneration II: 1 HP every 25 ticks (interval 50 >> level) = 0.8 HP/s",
	value=lambda s: 0.8 * s, show=lambda s: f"{0.8 * s:.2f} HP/s (1 heart per {2 / (0.8 * s):.1f} s)",
	impl="Tick handler: heal 0.8 x s / 20 HP per tick while hurt (LivingEntity.heal takes floats). Not a MobEffectInstance, because vanilla levels are whole numbers.",
	src="RegenerationMobEffect")
effect("resistance", name="Resistance (from Turtle Master)", potion="turtle_master", role="main", level_one=0.25,
	variants={"normal": (400, 2), "long": (800, 2), "strong": (400, 3)},
	anchor="Resistance IV (strong Turtle Master): damage taken -80% ((level x 5) / 25)",
	value=lambda s: 0.8 * s, show=lambda s: f"damage taken {pct(-0.8 * s)}",
	impl="Damage hook (mixin at LivingEntity.getDamageAfterMagicAbsorb, beside vanilla's resistance), x (1 - 0.8 x s), skipping bypasses_resistance damage.",
	src="LivingEntity.getDamageAfterMagicAbsorb, Potions.STRONG_TURTLE_MASTER")
effect("fire_resistance", name="Fire Resistance", potion="fire_resistance", role="main",
	variants={"normal": (3600, 0), "long": (9600, 0)},
	anchor="Fire Resistance: immune to fire-tagged damage (on/off)",
	value=lambda s: s, show=lambda s: f"fire damage {pct(-s)} (lava {4 * (1 - s):.2f}/hit), burn time {pct(-s)}",
	impl="On/off made partial: fire-tagged damage (is_fire) x (1 - s) through the damage hook, and burning_time attribute -s (add_multiplied_total) so fire also goes out sooner.",
	src="LivingEntity (is_fire + FIRE_RESISTANCE), Entity lava 4.0, Attributes.BURNING_TIME")
effect("water_breathing", name="Water Breathing", potion="water_breathing", role="main",
	variants={"normal": (3600, 0), "long": (9600, 0)},
	anchor="Water Breathing: air never runs out (on/off); vanilla air is 300 ticks (15 s)",
	value=lambda s: 15 / (1 - s) if s < 1 else math.inf,
	show=lambda s: f"air lasts {15 / (1 - s):.0f} s (vanilla 15 s)",
	impl="On/off made partial with the vanilla oxygen_bonus attribute (what Respiration uses): bonus = s / (1 - s), so each air tick is kept with chance s. Pure attribute, exact.",
	src="LivingEntity.decreaseAirSupply, Entity.getMaxAirSupply 300")
effect("night_vision", name="Night Vision", potion="night_vision", role="main",
	variants={"normal": (3600, 0), "long": (9600, 0)},
	anchor="Night Vision: lightmap night-vision intensity 1.0 (on/off; vanilla fades through 0.4-1.0 when ending)",
	value=lambda s: s, show=lambda s: f"brightness {s * 100:.0f}% of night vision",
	impl="On/off made partial on the client: set the lightmap's nightVisionEffectIntensity to s (mixin at LightmapRenderStateExtractor, where vanilla writes GameRenderer.nightVisionScale). Vanilla already renders fractional values during its end-fade.",
	src="GameRenderer.nightVisionScale, LightmapRenderStateExtractor")
effect("invisibility", name="Invisibility", potion="invisibility", role="main",
	variants={"normal": (3600, 0), "long": (9600, 0)},
	anchor="Invisibility with no armor: mobs see you at 7% range (0.7 x 10% minimum armor cover). In full armor vanilla gives only 70%.",
	value=lambda s: 0.93 * s,
	show=lambda s: f"mobs notice you at {100 * (1 - 0.93 * s):.0f}% range, locator bar range {pct(-s)}",
	impl="Stealth, not a see-through body: the vanilla mob_visibility item component on each gemmed piece (pieces multiply, so each gets total^(1/n)), plus waypoint_transmit_range -s. Data only.",
	src="LivingEntity.getVisibilityPercent, item/component/MobVisibility, MobEffects.INVISIBILITY")
effect("slow_falling", name="Slow Falling", potion="slow_falling", role="main",
	variants={"normal": (1800, 0), "long": (4800, 0)},
	anchor="Slow Falling: falling gravity capped at 0.01 (from 0.08), no fall damage",
	value=lambda s: s,
	show=lambda s: f"fall speed max {49 * (0.08 - 0.07 * s) * T:.1f} b/s (vanilla 78.4), fall damage {pct(-s)}",
	impl="On/off made partial: while falling, gravity 0.08 - 0.07 x s (mixin at LivingEntity.getEffectiveGravity, where vanilla checks slow falling) and fall_damage_multiplier -s.",
	src="LivingEntity.getEffectiveGravity, Attributes.GRAVITY 0.08")
effect("luck", name="Luck", potion="luck", role="main",
	variants={"normal": (6000, 0)}, source=("found", ["common", "rare", "ominous"],
		"fishing treasure and shipwrecks (common), buried treasure (rare); the Luck potion has no survival brewing recipe"),
	anchor="Luck I: luck +1 (only level)",
	value=lambda s: s, show=lambda s: f"+{s:.2f} luck",
	impl="Attribute modifier on luck, +s (exact).",
	src="MobEffects.LUCK")
effect("instant_health", name="Healing", potion="healing", role="healing", kind="instant",
	variants={"normal": (1, 0), "strong": (1, 1)},
	unit=8.0, unit_fmt=lambda x: f"heals the target {x:.1f} HP",
	hit_anchor_text="Strongest tipped arrow (strong Healing): Healing II, 8 HP at once (4 << level)",
	anchor="Its own scale: 1.0 on the gem scale returns 25% of each hit (the best build, 0.95, returns exactly 25%)",
	value=heal_fraction,
	show=lambda s: f"{heal_fraction(s) * 100:.1f}% of each non-lethal hit back (6 HP hit: +{6 * heal_fraction(s):.2f} HP)",
	impl=("After the wearer takes damage from an attack (an entity source) or a fall and survives it, heal that "
		"damage instance's health loss x the gem's fraction, at once. A killing blow still kills (the heal runs "
		"after the hit, only if the wearer is alive). Mixin after LivingEntity.actuallyHurt. Works the same on mobs."),
	src="LivingEntity.actuallyHurt, DamageTypeTags.IS_FALL, HealOrHarmMobEffect")
# Harmful gems: their anchor is what the strongest tipped arrow applies (the hit-applied curve).
effect("poison", name="Poison", potion="poison", role="harmful", kind="scaled", level_one=0.48,
	variants={"normal": (900, 0), "long": (1800, 0), "strong": (432, 1)},
	unit=25 / 12 * 0.8, unit_fmt=lambda x: f"{x:.2f} HP/s poison",
	anchor="Strongest tipped arrow (strong Poison): Poison II for 2.7 s, 1 HP every 12 ticks (25 >> level) = 1.67 HP/s, never below 1 HP",
	impl="Gem Poison mob effect (vanilla's icon and rules; potency in 1% steps): never below 1 HP, undead immune, milk clears it.",
	src="PoisonMobEffect, Potions.STRONG_POISON")
effect("slowness", name="Slowness", potion="slowness", role="harmful", kind="scaled", level_one=0.25,
	variants={"normal": (1800, 0), "long": (4800, 0), "strong": (400, 3)},
	unit=0.6, unit_fmt=lambda x: f"{pct(-x)} speed",
	anchor="Strongest tipped arrow (strong Slowness): Slowness IV for 2.5 s, movement speed -60% (-0.15 per level)",
	wearer=lambda s: f"{pct(-0.6 * s)} speed",
	impl="Gem Slowness mob effect: movement_speed -0.6 x potency (add_multiplied_total). As a drawback: the same modifier, permanent while worn.",
	src="MobEffects.SLOWNESS, Potions.STRONG_SLOWNESS")
effect("weakness", name="Weakness", potion="weakness", role="harmful", kind="scaled", level_one=1.0,
	variants={"normal": (1800, 0), "long": (4800, 0)},
	unit=4.0, unit_fmt=lambda x: f"-{x:.2f} attack damage",
	anchor="Strongest tipped arrow (long Weakness): Weakness I for 30 s, attack damage -4 (only level)",
	wearer=lambda s: f"-{4 * s:.2f} attack damage",
	impl="Gem Weakness mob effect: attack_damage -4 x potency. As a drawback: the same modifier, permanent while worn.",
	src="MobEffects.WEAKNESS, Potions.LONG_WEAKNESS")
effect("instant_damage", name="Harming", potion="harming", role="harmful", kind="instant",
	variants={"normal": (1, 0), "strong": (1, 1)},
	unit=12.0, unit_fmt=lambda x: f"+{x:.1f} magic damage",
	anchor="Strongest tipped arrow (strong Harming): Harming II, 12 magic damage at once (6 << level)",
	impl=f"Charged strike: the first hit or arrow after a {HARM_CHARGE_S} s charge deals +12 x s magic damage (HealOrHarmMobEffect.applyInstantaneousEffect). Undead are healed instead, as in vanilla.",
	src="HealOrHarmMobEffect")
for key, name, extra in [
		("wind_charged", "Wind Charged", "a wind burst when the target dies"),
		("weaving", "Weaving", "cobwebs where the target dies"),
		("oozing", "Oozing", "2 slimes when the target dies"),
		("infested", "Infested", "1-2 silverfish when the target is hurt (vanilla 10% per hit)")]:
	effect(key, name=name, potion=key, role="harmful", kind="onoff", variants={"normal": (3600, 0)},
		unit=None, unit_fmt=None,
		anchor=f"Tipped arrow of {name}: {name} I for 22.5 s: {extra} (only level)",
		impl="The vanilla effect itself (it has one level), for a duration scaled by s.",
		src=f"{''.join(p.title() for p in key.split('_'))}MobEffect, Potions.{key.upper()}")
# Armor gems with no brewable potion: an item that carries the effect in vanilla goes in the bottle slot
# (26.3's bottle slot takes any item a brewing recipe names as input), or the gem is found-only.
effect("haste", name="Haste", potion=None, role="main", variants={"normal": None},
	source=("found", ["common", "rare", "ominous"], "mineshaft chests (common), stronghold and ancient city chests (rare); only beacons give Haste in vanilla"),
	anchor="Haste II (beacon): mining speed x1.4 (+0.2 per level), attack speed +20%",
	value=lambda s: 0.4 * s, show=lambda s: f"mining {pct(0.4 * s)}, attack speed {pct(0.2 * s)}",
	impl="Attribute modifiers: block_break_speed +0.4 x s and attack_speed +0.2 x s (add_multiplied_total).",
	src="Player.getDestroySpeed, MobEffects.HASTE, BeaconBlockEntity")
effect("absorption", name="Absorption", potion=None, role="main", variants={"normal": None, "strong": None},
	source=("item", "golden apple (Absorption I: normal) or enchanted golden apple (Absorption IV: strong)"),
	anchor="Absorption IV (enchanted golden apple): 16 absorption HP (4 x (level + 1))",
	value=lambda s: 16 * s, show=lambda s: f"{16 * s:.1f} HP shield, refills 30 s after it was last hit",
	impl="max_absorption +16 x s, refilled to full 30 s after it was last hit.",
	src="AbsorptionMobEffect, Consumables.GOLDEN_APPLE / ENCHANTED_GOLDEN_APPLE")
effect("health_boost", name="Health Boost", potion=None, role="main", variants={"normal": None},
	source=("found", ["rare", "ominous"], "end city and ancient city chests (rare); no vanilla item or source gives Health Boost"),
	anchor="Health Boost II: max health +8 (4 per level). No vanilla source gives it, so the anchor is a proposal.",
	value=lambda s: 8 * s, show=lambda s: f"+{8 * s:.1f} max health",
	impl="Attribute modifier on max_health, +8 x s (exact).",
	src="MobEffects.HEALTH_BOOST")
effect("dolphins_grace", name="Dolphin's Grace", potion=None, role="main", variants={"normal": None},
	source=("item", "nautilus shell (drowned, fishing treasure; dolphins give the effect)"),
	anchor="Dolphin's Grace: water drag 0.96 instead of 0.8 (0.9 sprinting) (on/off)",
	value=lambda s: 0.8 + 0.16 * s, show=lambda s: f"water drag {0.8 + 0.16 * s:.3f} (vanilla 0.800)",
	impl="On/off made partial: water slow-down 0.8 + 0.16 x s (mixin where travelInWater sets 0.96).",
	src="LivingEntity.travelInWater, DOLPHINS_GRACE_WATER_DRAG, Dolphin")
effect("saturation", name="Saturation", potion=None, role="main", variants={"normal": None},
	source=("item", "suspicious stew of dandelion, golden dandelion or blue orchid (matched by its stew effect: Fabric API's components ingredient)"),
	anchor="Saturation: hunger never drops (on/off; vanilla refills food and saturation every tick)",
	value=lambda s: s, show=lambda s: f"hunger drains {pct(-s)} slower",
	impl="On/off made partial: food exhaustion x (1 - s) (mixin at FoodData.addExhaustion). Food still drains, just slower, so eating stays part of the game.",
	src="SaturationMobEffect, FoodData.addExhaustion, Blocks.DANDELION / BLUE_ORCHID")
effect("conduit_power", name="Conduit Power", potion=None, role="main", variants={"normal": None},
	source=("item", "heart of the sea (buried treasure; the conduit's core)"),
	anchor="Conduit Power (only level): in water or rain, air never runs out, mining underwater at full speed, clear underwater sight",
	value=lambda s: s, show=lambda s: f"in water: air lasts {15 / (1 - s):.0f} s, underwater mining {pct(s)} of the way to full speed, underwater sight {s * 100:.0f}%",
	impl="On/off made partial, only while in water or rain: oxygen_bonus s / (1 - s), submerged_mining_speed raised by s of the way to 1.0, underwater fog s of the way to the conduit view.",
	src="ConduitBlockEntity, MobEffectUtil.hasWaterBreathing, Attributes.SUBMERGED_MINING_SPEED")
effect("hero_of_the_village", name="Hero of the Village", potion=None, role="main", variants={"normal": None},
	source=("found", ["common", "rare", "ominous"], "hero-of-the-village gifts (common), pillager outpost chests (rare); only winning a raid gives it in vanilla"),
	anchor="Hero of the Village V (a level-5 raid won): villager prices cut by 0.3 + 0.0625 x 4 = 55% of the base cost",
	value=lambda s: 0.55 * s, show=lambda s: f"villager trades {0.55 * s * 100:.0f}% cheaper",
	impl="Price hook beside vanilla's: the hero modifier 0.55 x s (mixin at Villager.updateSpecialPrices). Villagers' gifts stay vanilla-only (the effect itself).",
	src="Villager.updateSpecialPrices, Raid")
# More weapon gems (harmful effects with no potion): the anchor is the strongest vanilla source.
# A source that is a hit (arrow, bullet, mob attack) applies its full duration; any other source (food,
# area pulse, spell) is scaled like a tipped arrow (1/8), so the 1.0 is always "what one hit would apply".
def hit_anchor(ticks, amp, is_hit):
	return (ticks if is_hit else max(math.floor(ticks * TIPPED_ARROW_SCALE), 1), amp)
effect("wither", name="Wither", potion=None, role="harmful", kind="scaled", level_one=1.0, variants={"normal": None},
	source=("item", "wither rose (dropped where the Wither kills a mob)"),
	hit=hit_anchor(200, 0, True), unit=0.5, unit_fmt=lambda x: f"{x:.2f} HP/s wither",
	anchor="Wither skeleton's hit: Wither I for 10 s, 1 HP every 40 ticks = 0.5 HP/s (can kill, unlike poison)",
	impl="Gem Wither mob effect (vanilla's icon; potency in 1% steps); it can kill, as vanilla's does.",
	src="WitherSkeleton.doHurtTarget, WitherMobEffect")
effect("mining_fatigue", name="Mining Fatigue", potion=None, role="harmful", kind="scaled", level_one=1 / 3, variants={"normal": None},
	source=("found", ["ominous"], "elder guardian drops (a rare extra drop); vanilla's only source is the elder guardian"),
	hit=hit_anchor(6000, 2, False), unit=1.0, unit_fmt=lambda p: f"mining x{0.027 ** p:.2f}, attack speed {pct(-0.3 * p)}",
	anchor="Elder guardian: Mining Fatigue III (mining x0.027, attack speed -30%), at arrow scale 37.5 s",
	wearer=lambda s: f"mining x{0.027 ** s:.2f}, attack speed {pct(-0.3 * s)}",
	impl="Gem Mining Fatigue mob effect: block_break_speed x 0.027^potency and attack_speed -0.3 x potency. As a drawback: the same, permanent while worn.",
	src="ElderGuardian, Player.getDestroySpeed")
effect("hunger", name="Hunger", potion=None, role="harmful", kind="scaled", level_one=1 / 3, variants={"normal": None},
	source=("item", "rotten flesh (Hunger I, 80% chance in vanilla)"),
	hit=hit_anchor(300, 2, False), unit=0.3, unit_fmt=lambda x: f"exhaustion +{x / 20:.4f}/tick",
	anchor="Pufferfish: Hunger III (exhaustion +0.015 per tick), at arrow scale 1.8 s",
	wearer=lambda s: f"extra hunger: 1 food point per {4 / (0.3 * s):.0f} s",
	impl="Gem Hunger mob effect: causeFoodExhaustion(0.015 x potency) per tick (players only; mobs have no food). As a drawback: the same, permanent while worn.",
	src="HungerMobEffect, Consumables.PUFFERFISH / ROTTEN_FLESH")
for key, name, src_item, ticks, is_hit, how, src in [
		("glowing", "Glowing", "glowstone dust", 200, True, "Spectral arrow hit: Glowing for 10 s", "SpectralArrow"),
		("levitation", "Levitation", "shulker shell", 200, True, "Shulker bullet hit: Levitation I for 10 s", "ShulkerBullet"),
		("blindness", "Blindness", "suspicious stew of azure bluet or open eyeblossom (Fabric API's components ingredient)", 400, False,
			"Illusioner's spell: Blindness for 20 s, at arrow scale 2.5 s", "Illusioner, Blocks.AZURE_BLUET"),
		("nausea", "Nausea", "pufferfish", 300, False, "Pufferfish: Nausea for 15 s, at arrow scale 1.8 s", "Consumables.PUFFERFISH"),
		("darkness", "Darkness", "echo shard (ancient cities)", 260, False, "Warden's pulse: Darkness for 13 s, at arrow scale 1.6 s", "Warden")]:
	effect(key, name=name, potion=None, role="harmful", kind="onoff", variants={"normal": None},
		source=("item", src_item), hit=hit_anchor(ticks, 0, is_hit), unit=None, unit_fmt=None,
		anchor=how + " (on/off effect: the strength goes into duration)",
		impl="The vanilla effect itself, for a duration scaled by s. Mostly felt by players (PvP, and gemmed mob gear hitting players).",
		src=src)
# Drawback-only (armor bad rolls, on the wearer). Slowness, weakness, mining fatigue and hunger above double as drawbacks.
effect("unluck", name="Bad Luck", potion=None, role="negative", variants={}, level_one=1.0,
	anchor="Bad Luck I: luck -1",
	value=lambda s: s, show=lambda s: f"-{s:.2f} luck",
	impl="Attribute modifier on luck, -s.", src="MobEffects.UNLUCK")

for k, e in E.items():
	e.setdefault("source", ("potion",) if e["potion"] else None)
	e["good"] = e["role"] in ("main", "healing")
	if e["role"] == "main":
		e.setdefault("kind", "scaled")

# Beneficial gems on a weapon (owner: the slot decides who its effects reach; a regeneration gem on a sword
# heals the target). Their hit 1.0 is the strongest tipped arrow of the effect at its worn 1.0 level. With
# no potion, the strongest vanilla source at tipped-arrow scale (Absorption: the enchanted golden apple),
# or, with none, a standard 3:00 potion's tipped arrow (proposal: 22.5 s).
DEFAULT_HIT_TICKS = math.floor(3600 * TIPPED_ARROW_SCALE)
E["absorption"]["hit"] = (math.floor(2400 * TIPPED_ARROW_SCALE), 3)

def arrow_anchor(key):
	"""(ticks, amplifier) of the hit 1.0: the strongest tipped arrow (highest level; ties: longest), or the hit anchor."""
	e = E[key]
	if "hit" in e:
		return e["hit"]
	if not e["potion"]:
		return DEFAULT_HIT_TICKS, 0
	v = max(e["variants"].values(), key=lambda tv: (tv[1], tv[0]))
	ticks, amp = v
	return max(math.floor(ticks * TIPPED_ARROW_SCALE), 1), amp

def weapon_hit(key, s):
	"""(potency fraction of the anchor, duration in seconds) one hit applies at weapon strength s."""
	e = E[key]
	ticks, _ = arrow_anchor(key)
	if e["kind"] == "scaled":
		return s ** WEAPON_SPLIT, s ** (1 - WEAPON_SPLIT) * ticks / T
	if e["kind"] == "onoff":
		return 1.0, s * ticks / T
	return s, 0.0  # instant

def potency_text(key, x):
	"""The effect at x of its 1.0 level, with no duration (what a drawback or a worn harmful gem does)."""
	e = E[key]
	if e["role"] in ("main", "negative"):
		return e["show"](x)
	if "wearer" in e:
		return e["wearer"](x)
	return e["unit_fmt"](x * e["unit"])

def hit_show(key, s):
	"""In a weapon: what one hit (or fired arrow) gives the target at weapon strength s."""
	e = E[key]
	p, d = weapon_hit(key, s)
	if e["kind"] == "scaled":
		return f"{potency_text(key, p)} for {d:.1f} s"
	if e["kind"] == "onoff":
		return f"{e['name']} for {d:.1f} s"
	return f"{e['unit_fmt'](p * e['unit'])} every {HARM_CHARGE_S} s"

def hit_anchor_text(key):
	e = E[key]
	if "hit_anchor_text" in e:
		return e["hit_anchor_text"]
	if not e["good"]:
		return e["anchor"]
	ticks, _ = arrow_anchor(key)
	level = e["anchor"].split(":")[0]
	if key == "absorption":
		return f"{level} for {ticks / T:.1f} s (the enchanted golden apple at tipped-arrow scale)"
	if not e["potion"]:
		return f"{level} for {ticks / T:.1f} s (no vanilla timed source: a 3:00 potion's tipped arrow, proposal)"
	return f"Strongest tipped arrow: {level} for {ticks / T:.1f} s"

# Harmful gems worn (pointless, but allowed: a poison gem in your armor poisons you, owner).
DEATH_TRIGGER = {"wind_charged": "a wind burst", "weaving": "cobwebs", "oozing": "2 slimes"}
PULSED = {"glowing", "levitation", "blindness", "nausea", "darkness"}

def worn_show(key, s):
	"""In armor: what the wearer gets, all the time, at worn strength s."""
	e = E[key]
	if e["good"]:
		return e["show"](s)
	if key == "instant_damage":  # the mirror of the Healing gem, on the same curve
		return f"each non-lethal hit taken deals {heal_fraction(s) * 100:.1f}% more"
	if key in DEATH_TRIGGER:
		return f"{DEATH_TRIGGER[key]} where the wearer dies, {s:.0%} chance"
	if key == "infested":
		return f"{10 * s:.1f}% chance of silverfish each time the wearer is hurt"
	if key in PULSED:
		return f"{e['name']} on the wearer {10 * s:.1f} s of every 10 s"
	return potency_text(key, s) + (" (never below 1 HP)" if key == "poison" else "") + ", all the time"

def worn_anchor_text(key):
	e = E[key]
	if e["good"]:
		return e["anchor"]
	if key == "instant_damage":
		return "Its own scale, the Healing gem's mirror: the best build adds 25% to each hit taken"
	if key in DEATH_TRIGGER or key == "infested":
		return f"{e['name']}: the vanilla trigger every time (on/off; the strength is its chance)"
	if key in PULSED:
		return f"{e['name']}: on all the time (on/off; the strength is the share of time it's on)"
	return f"The hit anchor's level ({potency_text(key, 1.0)}), on the wearer all the time"

def worn_value(key, s):
	e = E[key]
	if e["good"]:
		return e["value"](s)
	return heal_fraction(s) if key == "instant_damage" else s

# Found-only gems: rolled like an enchant at the loot source's tiers, with a base gem from its list.
# Their potion factor is 1.0 (no variants), so only ominous loot (nether star, tier 3) gives a perfect gem.
LOOT = {
	"common":  ("Common loot (mineshafts, dungeons, shipwrecks, fishing treasure, hero gifts)", ["amethyst", "quartz", "ender_pearl"], [1, 2]),
	"rare":    ("Rare loot (strongholds, ancient cities, end cities, buried treasure, outposts)", ["diamond", "prismarine"], [2, 3]),
	"ominous": ("Ominous vaults in trial chambers (and the elder guardian for Mining Fatigue)", ["prismarine", "nether_star"], [3]),
}
# Effects that are truly not gems (every 26.3 effect is either a gem, drawback-only, or here).
NOT_GEMS = {
	"bad_omen": "a one-shot trigger, used up when a raid starts; worn permanently it would chain raids without end, and it has no partial form",
	"raid_omen": "the raid's own countdown, not something a player carries",
	"trial_omen": "a one-shot trigger that turns trial spawners ominous; permanent, every trial chamber would always be ominous",
	"breath_of_the_nautilus": "the 26.x nautilus mount's water breathing for its rider; the Water Breathing gem already covers it",
}
DRAWBACK_ANCHOR = {  # a drawback is a constant level, so its 1.0 is the strongest vanilla level itself
	"slowness": "Slowness IV (strong Slowness potion): movement speed -60%",
	"weakness": "Weakness I: attack damage -4 (only level)",
	"mining_fatigue": "Mining Fatigue III (elder guardian): mining x0.027, attack speed -30%",
	"hunger": "Hunger III (pufferfish): exhaustion +0.015 per tick",
	"unluck": "Bad Luck I: luck -1",
}
DRAWBACK_ONLY = {"unluck": "Bad Luck only ever matters for loot and fishing, too little to be worth a gem slot, so it stays a drawback"}

GEM_EFFECTS = [k for k, v in E.items() if v["role"] in ("main", "healing", "harmful")]
GOOD_GEMS = [k for k in GEM_EFFECTS if E[k]["good"]]
BAD_GEMS = [k for k in GEM_EFFECTS if not E[k]["good"]]

# ---------------------------------------------------------------- rolls (owner: one roll table per gem)
# Direct opposites (owner's list first, then added): never a gem's drawback, since a direct inverse just
# cancels out. (beneficial, harmful, why). The table is symmetric: each side excludes the other.
INVERSE_PAIRS = [
	("speed", "slowness", "movement speed up / down (owner)"),
	("strength", "weakness", "attack damage up / down (owner)"),
	("haste", "mining_fatigue", "mining and attack speed up / down (owner)"),
	("regeneration", "poison", "health over time back / away (owner)"),
	("regeneration", "wither", "health over time back / away (owner)"),
	("luck", "unluck", "luck +1 / -1 (owner)"),
	("instant_health", "instant_damage", "instant heal / instant damage (owner)"),
	("saturation", "hunger", "food kept / food drained (owner)"),
	("night_vision", "blindness", "sight in the dark / no sight (owner)"),
	("night_vision", "darkness", "sight in the dark / pulsing dark (owner)"),
	("invisibility", "glowing", "hidden / outlined through walls (owner)"),
	("slow_falling", "levitation", "slower fall / forced rise (owner)"),
	("conduit_power", "mining_fatigue", "added: Conduit Power's underwater mining is the conduit's Haste, and Mining Fatigue is the ocean monument's direct answer to it"),
]
OWNER_PAIRS = {(a, b) for a, b, why in INVERSE_PAIRS if why.endswith("(owner)")}
NO_OPPOSITE = {  # checked and left out of the table, with the reason
	"jump_boost": "no vanilla effect lowers jumps; Slow Falling / Levitation is a different axis (falling, not jumping)",
	"resistance": "no vanilla effect raises damage taken; Weakness lowers damage dealt, which is Strength's opposite",
	"health_boost": "no vanilla effect lowers max health; Wither drains health over time, which is Regeneration's opposite",
	"absorption": "no vanilla effect takes absorption away; same reasoning as Health Boost",
	"fire_resistance": "no vanilla effect makes fire hurt more",
	"water_breathing": "no vanilla effect drains air faster",
	"dolphins_grace": "no vanilla effect slows swimming alone; Slowness is Speed's opposite",
	"hero_of_the_village": "its opposite is Bad Omen, which is not a gem or a drawback",
}
OPPOSITE = {}
for a, b, _ in INVERSE_PAIRS:
	OPPOSITE.setdefault(a, set()).add(b)
	OPPOSITE.setdefault(b, set()).add(a)

# Drawback candidates, by the main effect's kind. A beneficial gem's drawback is a mild harmful effect, a
# harmful gem's a mild beneficial one; the gem's slot decides who gets it, like its main effect (armor:
# the wearer, while worn; weapon: the target, for a few seconds refreshed by each hit).
HARMFUL_DRAWBACKS = ["slowness", "weakness", "mining_fatigue", "hunger", "unluck"]
BENEFICIAL_DRAWBACKS = ["speed", "regeneration", "resistance", "jump_boost"]
NEVER_DRAWBACK = {
	"poison": "a permanent damage-over-time on the wearer cancels natural healing and nags at every moment; it can't kill but pins the wearer near 1 HP",
	"wither": "the same as poison, and it can kill",
	"instant_damage": "instant, no mild form",
	"blindness": "ruins the screen, not just the gem", "darkness": "ruins the screen, not just the gem",
	"nausea": "ruins the screen, not just the gem", "levitation": "dangerous, not mild",
	"glowing": "harmless alone, a beacon in PvP", "wind_charged": "only felt at death", "weaving": "only felt at death",
	"oozing": "only felt at death", "infested": "spawns mobs, not mild",
	"absorption": "changes the fight too much", "health_boost": "changes the fight too much",
	"strength": "changes the fight too much", "invisibility": "the target vanishes",
	"fire_resistance": "would cancel Fire Aspect",
}
# Main effect only, never an "other" roll.
OTHER_EXCLUDED = {"instant_health": "its own per-hit curve: main effect only", "instant_damage": "a charged strike: main effect only",
	"hero_of_the_village": "a permanent villager discount stays a rare found gem: main effect only"}

def other_pool(k):
	return [o for o in GEM_EFFECTS if E[o]["good"] == E[k]["good"] and o != k and o not in OTHER_EXCLUDED]

def drawback_pool(k):
	return [d for d in (HARMFUL_DRAWBACKS if E[k]["good"] else BENEFICIAL_DRAWBACKS) if d != k and d not in OPPOSITE.get(k, ())]

EXAMPLES = [("speed", "strength", "hunger"), ("poison", "slowness", "resistance"), ("night_vision", "water_breathing", "mining_fatigue")]

def source_text(k):
	src = E[k]["source"]
	if src[0] == "potion":
		return f"Craftable: {E[k]['potion'].replace('_', ' ')} potion (normal, splash or lingering) + base gem"
	if src[0] == "item":
		return f"Craftable: {src[1]} in the bottle slot + base gem; gunpowder then dragon's breath raise its form"
	return f"Found only: {src[2]}; plus ominous vaults"

# ---------------------------------------------------------------- source verification
SRC_EXPECT_EFFECTS = [
	r'"speed".*?MOVEMENT_SPEED.*?0\.2F, AttributeModifier\.Operation\.ADD_MULTIPLIED_TOTAL',
	r'"slowness".*?MOVEMENT_SPEED.*?-0\.15F',
	r'"haste".*?ATTACK_SPEED.*?0\.1F',
	r'"mining_fatigue".*?ATTACK_SPEED.*?-0\.1F',
	r'"strength".*?ATTACK_DAMAGE.*?3\.0, AttributeModifier\.Operation\.ADD_VALUE',
	r'"jump_boost".*?SAFE_FALL_DISTANCE.*?1\.0, AttributeModifier\.Operation\.ADD_VALUE',
	r'"weakness".*?ATTACK_DAMAGE.*?-4\.0',
	r'"health_boost".*?MAX_HEALTH.*?4\.0',
	r'"absorption".*?MAX_ABSORPTION.*?4\.0',
	r'"luck".*?LUCK.*?1\.0',
	r'"infested", new InfestedMobEffect\(MobEffectCategory\.HARMFUL, \d+, 0\.1F',
]
SRC_EXPECT_FILES = {
	"world/effect/RegenerationMobEffect.java": [r"50 >> amplification", r"heal\(1\.0F\)"],
	"world/effect/PoisonMobEffect.java": [r"25 >> amplification", r"getHealth\(\) > 1\.0F"],
	"world/effect/HealOrHarmMobEffect.java": [r"4 << amplification", r"6 << amplification"],
	"world/effect/AbsorptionMobEffect.java": [r"4 \* \(1 \+ amplifier\)"],
	"world/effect/HungerMobEffect.java": [r"0\.005F \* \(amplification \+ 1\)"],
	"world/effect/MobEffectInstance.java": [r"Math\.max\(Mth\.floor\(duration \* scale\), 1\)"],
	"world/entity/LivingEntity.java": [r"getAmplifier\(\) \+ 1\) \* 5", r"0\.1F \* \(this\.getEffect\(MobEffects\.JUMP_BOOST\)",
		r"Math\.min\(this\.getGravity\(\), 0\.01\)", r"visibilityPercent \*= 0\.7 \* coverPercentage", r"slowDown = 0\.96F",
		r"oxygenBonus \+ 1\.0", r"void actuallyHurt\("],
	"world/entity/player/Player.java": [r"\+ 1\) \* 0\.2F", r"Math\.pow\(0\.3, amplifier \+ 1\)"],
	"world/entity/Entity.java": [r"lava\(\), 4\.0F", r"getMaxAirSupply\(\) \{\s*return 300;"],
	"world/entity/projectile/arrow/Arrow.java": [r"forEachEffect\(effect -> mob\.addEffect\(effect, effectSource\), durationScale\)",
		r"new ItemStack\(Items\.ARROW\)"],
	"world/entity/ai/attributes/Attributes.java": [r'"attribute\.name\.jump_strength", 0\.42F', r'"attribute\.name\.gravity", 0\.08'],
	"world/item/component/Consumables.java": [r"HUNGER, 300, 2", r"ABSORPTION, 2400, 3"],
	"world/item/equipment/ArmorMaterials.java": [r"ArmorMaterial COPPER", r"ArmorMaterial TURTLE_SCUTE", r"ArmorMaterial NETHERITE"],
	"world/item/ToolMaterial.java": [rf"ToolMaterial {m.upper()} = " for m in WEAPON_MATERIALS],
	"world/item/Items.java": [r"POTION_DURATION_SCALE, 0\.125F", r"Item PRISMARINE_SHARD", r"Item NAUTILUS_SHELL", r"Item HEART_OF_THE_SEA",
		r"Item ECHO_SHARD", r"Item SHULKER_SHELL", r"Item SPECTRAL_ARROW", r"Item GLOWSTONE_DUST", r"Item ENCHANTED_GOLDEN_APPLE", r"Item SUSPICIOUS_STEW", r"Item NETHERITE_SPEAR", r"Item WOODEN_SPEAR", r"Item MACE",
		r"Item TRIDENT", r"Item CROSSBOW"],
	"world/entity/monster/ElderGuardian.java": [r"MINING_FATIGUE, 6000, 2"],
	"world/inventory/BrewingStandMenu.java": [r"PotionIngredient\.isPotionInput\(itemStack, this\.recipeAccess\)"],
	"world/item/crafting/PotionIngredient.java": [r"RecipePropertySet\.BREWING_INPUTS"],
	"world/entity/monster/skeleton/WitherSkeleton.java": [r"MobEffects\.WITHER, 200\)"],
	"world/effect/WitherMobEffect.java": [r"40 >> amplification"],
	"world/entity/projectile/arrow/SpectralArrow.java": [r"int duration = 200", r"MobEffects\.GLOWING"],
	"world/entity/projectile/ShulkerBullet.java": [r"MobEffects\.LEVITATION, 200\)"],
	"world/entity/monster/illager/Illusioner.java": [r"MobEffects\.BLINDNESS, 400\)"],
	"world/entity/monster/warden/Warden.java": [r"MobEffects\.DARKNESS, 260"],
	"world/entity/npc/villager/Villager.java": [r"0\.3F \+ 0\.0625F \* heroOfTheVillage\.getAmplifier\(\)"],
	"world/level/block/Blocks.java": [r"FlowerBlock\(MobEffects\.SATURATION", r"FlowerBlock\(MobEffects\.BLINDNESS, 11\.0F", r"WitherRoseBlock\(MobEffects\.WITHER"],
	"world/item/component/Consumables.java#2": [r"NAUSEA, 300, 0", r"ABSORPTION, 2400, 0", r"HUNGER, 600, 0\), 0\.8F"],
}

def verify_source(errors):
	if not SRC.exists():
		return "skipped (no build/mcsrc/26.3; run tools/mcsrc.py)"
	potions = (SRC / "world/item/alchemy/Potions.java").read_text(encoding="utf-8")
	potions = re.sub(r"\s+", " ", potions)
	n = 0
	for key, e in E.items():
		if not e["potion"]:
			continue
		for var, (ticks, amp) in e["variants"].items():
			amp_s = f", {amp}" if amp else ""
			pat = rf'new Potion\("{e["potion"]}",[^;]*?MobEffectInstance\(MobEffects\.{key.upper()}, {ticks}{amp_s}\)'
			if key == "resistance":
				pat = rf'new Potion\("turtle_master",[^;]*?MobEffects\.RESISTANCE, {ticks}{amp_s}\)'
			if not re.search(pat, potions):
				errors.append(f"source: potion {e['potion']} {var} ({ticks} ticks, amplifier {amp}) not found in Potions.java")
			n += 1
	for missing in ("wither", "haste", "absorption", "saturation", "glowing", "levitation"):
		if re.search(rf'new Potion\("{missing}"', potions):
			errors.append(f"source: a {missing} potion now exists; make that gem brewable from it")
		n += 1
	brew = ROOT / "build" / "mcsrc" / "26.3" / "vanilla" / "data" / "minecraft" / "recipe" / "brewing"
	if brew.exists():
		if any('"minecraft:luck"' in f.read_text(encoding="utf-8") for f in brew.glob("*.json")):
			errors.append("source: a survival Luck brewing recipe now exists; Luck gems can become craftable")
		n += 1
	effects = re.sub(r"\s+", " ", (SRC / "world/effect/MobEffects.java").read_text(encoding="utf-8"))
	for pat in SRC_EXPECT_EFFECTS:
		if not re.search(r"register\( ?" + pat, effects):
			errors.append(f"source: MobEffects.java no longer matches {pat}")
		n += 1
	for rel, pats in SRC_EXPECT_FILES.items():
		p = SRC / rel.split("#")[0]
		text = p.read_text(encoding="utf-8") if p.exists() else ""
		for pat in pats:
			if not re.search(pat, text):
				errors.append(f"source: {rel} no longer matches {pat}")
			n += 1
	return f"{n} facts matched against build/mcsrc/26.3"

# ---------------------------------------------------------------- the model
W = sum(STACK_R ** j for j in range(SLOTS * len(PIECES)))
WW = sum(STACK_R ** j for j in range(WEAPON_SLOTS))

def runs(layout):
	"""Run length for each piece: the unbroken same-material stretch it sits in (None breaks it)."""
	out = [0] * len(layout)
	i = 0
	while i < len(layout):
		j = i
		while j + 1 < len(layout) and layout[i] is not None and layout[j + 1] == layout[i]:
			j += 1
		for k in range(i, j + 1):
			out[k] = (j - i + 1) if layout[k] is not None else 0
		i = j + 1
	return out

def piece_factors(layout):
	r = runs(layout)
	return [MATERIALS[m][0] * RUN[r[i]] if m else 0.0 for i, m in enumerate(layout)]

def stack(contribs, norm=W):
	"""Total strength of one effect from every gem contribution to it (each <= 1)."""
	c = sorted(contribs, reverse=True)
	return CAP * sum(x * STACK_R ** j for j, x in enumerate(c)) / norm

def wstack(contribs):
	return stack(contribs, WW)

def gem_quality(effect_key, variant, base, grade):
	return variant_factors(E[effect_key]["variants"])[variant] * BASE[base] * GRADES[grade]

def build_total(layout, gems_per_piece, quality):
	pf = piece_factors(layout)
	return stack([quality * pf[i] for i in range(4) for _ in range(gems_per_piece[i])])

def weapon_total(weapon, gems, quality):
	return wstack([quality * WEAPON_ALL[weapon]] * gems)

def top_variant(key):
	vf = variant_factors(E[key]["variants"])
	return max(vf, key=vf.get)

# ---------------------------------------------------------------- checks
def ladder_ok(name, values, errors):
	for (a_name, a), (b_name, b) in zip(values, values[1:]):
		if not (b >= a * (1 + MIN_REL_STEP) - 1e-12):
			errors.append(f"{name}: {a_name} {a} -> {b_name} {b} is not a visible step (+{MIN_REL_STEP:.0%} needed)")

def check_monotonic(name, f, errors, n=100):
	vals = [f(i / n * CAP) for i in range(1, n + 1)]
	if not all(b > a for a, b in zip(vals, vals[1:])):
		errors.append(f"{name}: not strictly increasing in strength")

def run_checks():
	errors, notes = [], []
	notes.append("vanilla facts: " + verify_source(errors))
	# 1. Every ladder strictly up with a visible step.
	ladder_ok("base gems", [(b, f) for b, _, f, _ in BASE_GEMS], errors)
	ladder_ok("armor", [(m, MATERIALS[m][0]) for m in MAIN_LADDER], errors)
	ladder_ok("weapon material", [(m, WEAPON_MATERIALS[m][0]) for m in WEAPON_LADDER], errors)
	ladder_ok("run", [(k, v) for k, v in sorted(RUN.items())], errors)
	ladder_ok("grades", list(GRADES.items()), errors)
	for k in GEM_EFFECTS:
		vf = variant_factors(E[k]["variants"])
		ladder_ok(f"{k} variants", [(v, vf[v]) for v in ("normal", "long", "strong") if v in vf], errors)
	if [b for b, _, _, _ in BASE_GEMS] != ["amethyst", "quartz", "ender_pearl", "diamond", "prismarine", "nether_star"]:
		errors.append("base gem order must be amethyst, quartz, ender pearl, diamond, prismarine, nether star (owner)")
	if max(BASE.values()) != BASE[BEST_BASE] or sum(1 for f in BASE.values() if f >= 1.0) != 1:
		errors.append("nether star must be the only best base gem (owner)")
	if max(MATERIALS.values())[0] != MATERIALS["netherite"][0] or sum(1 for v in MATERIALS.values() if v[0] >= 1.0) != 1:
		errors.append("netherite must be the only best armor material")
	if sum(1 for f in WEAPON_ALL.values() if f >= 1.0) != 1 or WEAPON_ALL["netherite"] != 1.0:
		errors.append("netherite must be the only best weapon")
	for t, outs in TIERS.items():
		if abs(sum(o[3] for o in outs) - 1) > 1e-9:
			errors.append(f"tier {t} outcome odds don't add up to 100%")
	if [o[1] for o in TIERS[2]] != ["medium", "medium"] or TIERS[2][0][2]:
		errors.append("tier 2 must be: medium alone, or medium + weak other (owner)")
	notes.append("every ladder step (base gem, armor, weapon material, run, grade, potion variant) is at least +4% on the one below; nether star and netherite are the only bests; each tier's odds add up to 100%")

	# 2. Gem quality: 1.0 only for strongest potion + lingering + tier 3 + nether star, for every gem effect.
	for k in GEM_EFFECTS:
		hits = []
		if E[k]["source"][0] == "found":
			for src in E[k]["source"][1]:
				_, bases, tiers = LOOT[src]
				for tier in tiers:
					for label, grade, _, _ in TIERS[tier]:
						if grade is None:
							continue
						for b in bases:
							if gem_quality(k, "normal", b, grade) >= 1 - 1e-12:
								hits.append((src, tier, b))
			if sorted(set(hits)) != [("ominous", 3, BEST_BASE)]:
				errors.append(f"{k}: found-only gem perfect from {sorted(set(hits))}, expected only an ominous nether star tier 3")
			continue
		for var in E[k]["variants"]:
			for form, maxtier in FORM_MAX_TIER.items():
				for tier in range(1, maxtier + 1):
					for label, grade, _, _ in TIERS[tier]:
						if grade is None:
							continue
						for b in BASE:
							q = gem_quality(k, var, b, grade)
							if q > 1 + 1e-12:
								errors.append(f"{k}: gem quality above 1")
							if q >= 1 - 1e-12:
								hits.append((var, form, tier, b))
		want = [(top_variant(k), "lingering", 3, BEST_BASE)]
		if sorted(set(hits)) != want:
			errors.append(f"{k}: best gem reachable by {sorted(set(hits))}, expected only {want}")
	notes.append("a gem's own strength reaches 1.0 only from the strongest potion or source item + lingering form + tier 3 + nether star, for every craftable armor and weapon effect; a found-only gem is perfect only from ominous loot with a nether star base and tier 3")

	# 2b. Every 26.3 status effect is classified: a gem (craftable or found), drawback-only, or not a gem.
	if SRC.exists():
		mob = re.sub(r"\s+", " ", (SRC / "world/effect/MobEffects.java").read_text(encoding="utf-8"))
		ids = set(re.findall(r'Holder<MobEffect> [A-Z_]+ = register\( ?"([a-z_]+)"', mob))
		known = set(GEM_EFFECTS) | set(NOT_GEMS) | set(DRAWBACK_ONLY)
		if ids != known:
			errors.append(f"status effects not classified: {sorted(ids - known)}; classified but not in 26.3: {sorted(known - ids)}")
		n_ids = len(ids)
	else:
		n_ids = len(set(GEM_EFFECTS) | set(NOT_GEMS) | set(DRAWBACK_ONLY))
	for k in GEM_EFFECTS:
		src = E[k]["source"]
		if src[0] == "found" and not set(src[1]) <= set(LOOT):
			errors.append(f"{k}: unknown loot source {src[1]}")
		if src[0] == "potion" and not E[k]["potion"]:
			errors.append(f"{k}: potion source without a potion")
	crafted = sum(1 for k in GEM_EFFECTS if E[k]["source"][0] != "found")
	found = sum(1 for k in GEM_EFFECTS if E[k]["source"][0] == "found")
	notes.append(f"all {n_ids} status effects in 26.3 are classified: {crafted} craftable gems, {found} found-only gems, {len(DRAWBACK_ONLY)} drawback-only, {len(NOT_GEMS)} not gems")

	# 3. Every armor layout x gem count: the stack reaches CAP only with full netherite, 12 gems.
	head_opts = [None] + list(MATERIALS)
	body_opts = [None] + [m for m in MATERIALS if m not in HELMET_ONLY]
	best_layouts, second = [], 0.0
	count = 0
	for layout in itertools.product(head_opts, body_opts, body_opts, body_opts):
		pf = piece_factors(list(layout))
		ranges = [range(SLOTS + 1) if m else range(1) for m in layout]
		for gems in itertools.product(*ranges):
			count += 1
			t = stack([pf[i] for i in range(4) for _ in range(gems[i])])
			if t >= CAP - 1e-12:
				best_layouts.append((layout, gems))
			else:
				second = max(second, t)
			if t >= 1.0:
				errors.append(f"layout {layout} {gems} reaches {t} >= 1.0")
	if best_layouts != [(("netherite",) * 4, (3, 3, 3, 3))]:
		errors.append(f"the armor cap is reached by {best_layouts}, expected only full netherite with 12 gems")
	if CAP - second < MIN_ABS_DROP:
		errors.append(f"second-best armor setup {second:.3f} is too close to the best {CAP}")
	notes.append(f"{count:,} armor layouts x gem counts enumerated: only full contiguous netherite with 12 gems reaches {CAP}; next best {second:.3f}")

	# 3w. Every weapon x every pair of gem qualities: the cap only with netherite + 2 perfect gems.
	qualities = sorted({gem_quality(k, v, b, g) for k in GEM_EFFECTS for v in E[k]["variants"] for b in BASE for g in GRADES})
	wbest, wsecond, wcount = set(), 0.0, 0
	for weapon in WEAPON_ALL:
		for n in range(WEAPON_SLOTS + 1):
			for qs in itertools.combinations_with_replacement(qualities, n):
				wcount += 1
				t = wstack([q * WEAPON_ALL[weapon] for q in qs])
				if t >= 1.0:
					errors.append(f"weapon {weapon} {qs} reaches {t} >= 1.0")
				if t >= CAP - 1e-12:
					wbest.add((weapon, qs))
				elif all(q == 1.0 for q in qs):  # like armor: the gap is measured over gear and gem count
					wsecond = max(wsecond, t)
	if wbest != {("netherite", (1.0, 1.0))}:
		errors.append(f"the weapon cap is reached by {sorted(wbest)}, expected only netherite with 2 perfect gems")
	if CAP - wsecond < MIN_ABS_DROP:
		errors.append(f"second-best weapon {wsecond:.3f} is too close to the best {CAP}")
	notes.append(f"{wcount:,} weapons x gem combinations enumerated: only a netherite weapon with 2 perfect gems reaches {CAP}; next best weapon or gem count with perfect gems {wsecond:.3f}")

	# 4. One step down from the best build loses a visible amount, for every gem in either slot.
	for k in GEM_EFFECTS:
		for label, t in downgrade_rows(k):
			if CAP - t < MIN_ABS_DROP:
				errors.append(f"{k}: '{label}' only drops {CAP - t:.3f} from the best (need {MIN_ABS_DROP})")
	for k in GEM_EFFECTS:
		for label, t in weapon_downgrade_rows(k):
			if CAP - t < MIN_ABS_DROP:
				errors.append(f"{k} weapon: '{label}' only drops {CAP - t:.3f} from the best (need {MIN_ABS_DROP})")
	notes.append(f"from the best armor build and the best weapon build, every single step down (potion, base, tier, material, run, one gem fewer, a material-less weapon) loses at least {MIN_ABS_DROP}")

	# 5. Early game is small but noticeable.
	worst = min(stack([gem_quality(k, "normal", "amethyst", "weak") * MATERIALS["leather"][0] * RUN[1]]) for k in GEM_EFFECTS)
	if worst < EARLY_SINGLE_FLOOR:
		errors.append(f"weakest single armor gem {worst:.3f} is below the floor {EARLY_SINGLE_FLOOR}")
	early = [build_total(["iron"] * 4, [1, 1, 1, 1], gem_quality(k, "normal", "quartz", "weak")) for k in GEM_EFFECTS]
	if not all(EARLY_BUILD_RANGE[0] <= x <= EARLY_BUILD_RANGE[1] for x in early):
		errors.append(f"early armor kit range {min(early):.3f}-{max(early):.3f} outside {EARLY_BUILD_RANGE}")
	wworst = min(weapon_total("wood", 1, gem_quality(k, "normal", "amethyst", "weak")) for k in GEM_EFFECTS)
	if wworst < EARLY_SINGLE_FLOOR:
		errors.append(f"weakest weapon gem {wworst:.3f} is below the floor")
	wearly = [weapon_total("iron", 1, gem_quality(k, "normal", "quartz", "weak")) for k in GEM_EFFECTS]
	if not all(EARLY_BUILD_RANGE[0] <= x <= EARLY_BUILD_RANGE[1] for x in wearly):
		errors.append(f"early weapon range {min(wearly):.3f}-{max(wearly):.3f} outside {EARLY_BUILD_RANGE}")
	notes.append(f"weakest single armor gem (normal potion, amethyst, tier 1, lone leather piece) = {worst:.3f}; early armor kit (iron set, 4 quartz tier-1 gems) = {min(early):.3f}-{max(early):.3f}; "
		f"weakest weapon gem (wooden sword, amethyst, tier 1) = {wworst:.3f}; early weapon (iron sword, 1 quartz tier-1 gem) = {min(wearly):.3f}-{max(wearly):.3f}")

	# 6. Stacking: never beyond cap x the strongest gem; adding a gem never lowers it; weak gems lose to strong.
	rng = random.Random(26_3)
	for _ in range(20000):
		n = rng.randint(1, 12)
		cs = [rng.choice(qualities) * rng.choice(list(MATERIALS.values()))[0] * rng.choice(list(RUN.values())) for _ in range(n)]
		t = stack(cs)
		if t > CAP * max(cs) + 1e-12 or t >= 1.0:
			errors.append(f"stack {cs} = {t} beats cap x strongest gem")
			break
		if n < 12 and stack(cs + [rng.choice(qualities)]) < t - 1e-12:
			errors.append(f"adding a gem to {cs} lowered the total")
			break
	for _ in range(5000):
		cs = [rng.choice(qualities) * rng.choice(list(WEAPON_ALL.values())) for _ in range(rng.randint(1, 2))]
		t = wstack(cs)
		if t > CAP * max(cs) + 1e-12 or t >= 1.0:
			errors.append(f"weapon stack {cs} = {t} beats cap x strongest gem")
			break
		if len(cs) == 1 and wstack(cs + [rng.choice(qualities)]) < t - 1e-12:
			errors.append(f"adding a weapon gem to {cs} lowered the total")
			break
	for m in MAIN_LADDER:
		weak12 = build_total([m] * 4, [3, 3, 3, 3], gem_quality("speed", "strong", BEST_BASE, "weak"))
		strong4 = build_total([m] * 4, [1, 1, 1, 1], gem_quality("speed", "strong", BEST_BASE, "strongest"))
		if not weak12 < strong4:
			errors.append(f"{m}: 12 tier-1 gems ({weak12:.3f}) beat 4 tier-3 gems ({strong4:.3f})")
	for m in WEAPON_ALL:
		weak2 = weapon_total(m, 2, gem_quality("poison", "strong", BEST_BASE, "weak"))
		strong1 = weapon_total(m, 1, gem_quality("poison", "strong", BEST_BASE, "strongest"))
		if not weak2 < strong1:
			errors.append(f"{m}: 2 tier-1 weapon gems ({weak2:.3f}) beat 1 tier-3 gem ({strong1:.3f})")
	notes.append(f"stacking: 25,000 random armor and weapon stacks never pass {CAP} x their strongest gem; adding a gem never lowers a total; 12 tier-1 armor gems lose to 4 tier-3 in every set, and 2 tier-1 weapon gems lose to 1 tier-3 on every weapon")

	# 7. Worn (armor) mappings move one way and never reach the vanilla anchor, for every gem.
	for k in GEM_EFFECTS:
		f = lambda s, k=k: worn_value(k, s)
		check_monotonic(k, f, errors)
		at_one = f(0.99999) if k == "water_breathing" else f(1.0)
		if k not in ("instant_health", "instant_damage") and not f(CAP) < at_one:
			errors.append(f"{k}: the worn cap reaches the vanilla anchor")
		for c in (0.05, CAP):
			worn_show(k, c), hit_show(k, c)  # every gem renders in both slots
	notes.append(f"every one of the {len(GEM_EFFECTS)} gems has a worn (armor) and a hit-applied (weapon) form; every worn mapping rises with strength and stays short of the vanilla level at the cap")

	# 8. Healing curve: modest at best, visible early, a net loss always, flat but still a ladder.
	h_best = heal_fraction(CAP)
	if abs(h_best - HEAL_BEST) > 1e-9 or not h_best < 0.5:
		errors.append(f"healing at the best build is {h_best:.3f}, expected {HEAL_BEST} and well under half")
	if not all(heal_fraction(x) < 1 for x in (CAP, 1.0)):
		errors.append("healing returns a whole hit or more")
	h_early = heal_fraction(build_total(["iron"] * 4, [1] * 4, gem_quality("instant_health", "normal", "quartz", "weak")))
	if h_early < HEAL_EARLY_FLOOR:
		errors.append(f"early healing kit returns {h_early:.3f}, below {HEAL_EARLY_FLOOR}")
	for label, t in downgrade_rows("instant_health"):
		if h_best - heal_fraction(t) < HEAL_MIN_DROP:
			errors.append(f"healing: '{label}' only drops {h_best - heal_fraction(t):.4f}")
	if not h_best / heal_fraction(CAP * 0.2) < 1 / 0.2:
		errors.append("healing curve is not flatter than linear")
	notes.append(f"healing: best build returns {h_best:.0%} of each non-lethal hit (never a whole hit, so taking damage is always a net loss); early kit {h_early:.1%}; every step down from the best loses at least {HEAL_MIN_DROP * 100:.1f} points; the curve is flatter than linear")

	# 9. Drawbacks: mild. One drawback is DRAWBACK_ONE; more saturate; always below vanilla's weakest level.
	tots = [drawback_total(n) for n in range(1, SLOTS * len(PIECES) + 1)]
	if abs(tots[0] - DRAWBACK_ONE) > 1e-12 or not all(b > a for a, b in zip(tots, tots[1:])) or not tots[-1] < DRAWBACK_CAP:
		errors.append("drawback stacking must start at DRAWBACK_ONE, rise, and stay below DRAWBACK_CAP")
	for k in HARMFUL_DRAWBACKS + BENEFICIAL_DRAWBACKS:
		if not DRAWBACK_CAP < E[k]["level_one"]:
			errors.append(f"{k}: drawbacks can reach vanilla level I ({E[k]['level_one']:.2f} on its scale)")
	weakest_benefit = min(gem_quality(k, "normal", "amethyst", "strongest") * MATERIALS["leather"][0] * RUN[1] for k in GEM_EFFECTS)
	if not DRAWBACK_ONE < weakest_benefit:
		errors.append(f"a drawback ({DRAWBACK_ONE}) outweighs the weakest tier-3 gem's own share ({weakest_benefit:.3f})")
	for k in NEVER_DRAWBACK:
		if k in HARMFUL_DRAWBACKS + BENEFICIAL_DRAWBACKS:
			errors.append(f"{k} must never be a drawback ({NEVER_DRAWBACK[k]})")
	notes.append(f"drawbacks: one is {DRAWBACK_ONE:.0%} of the strongest vanilla level, any number of one effect stays under {DRAWBACK_CAP:.0%}, below vanilla's level I for every drawback effect; poison, wither, harming and the rest of the never-drawback list never roll")

	# 9b. Rolls (owner): one table per gem; other = same kind, never itself; drawback = the other kind, never its direct opposite.
	for a, b, _ in INVERSE_PAIRS:
		if a not in E or b not in E or not E[a]["good"] or E[b]["good"]:
			errors.append(f"inverse pair {a}/{b} must be a beneficial and a harmful effect")
	need = {("speed", "slowness"), ("strength", "weakness"), ("haste", "mining_fatigue"), ("regeneration", "poison"), ("regeneration", "wither"),
		("luck", "unluck"), ("instant_health", "instant_damage"), ("saturation", "hunger"), ("night_vision", "blindness"),
		("night_vision", "darkness"), ("invisibility", "glowing"), ("slow_falling", "levitation")}
	if not need <= OWNER_PAIRS:
		errors.append(f"owner's inverse pairs missing: {sorted(need - OWNER_PAIRS)}")
	for k in set(NO_OPPOSITE) & set(OPPOSITE):
		errors.append(f"{k} is both in the inverse table and listed as having no opposite")
	sizes = {}
	for k in GEM_EFFECTS:
		op, dp = other_pool(k), drawback_pool(k)
		if not op:
			errors.append(f"{k}: empty 'other' roll table")
		if not dp:
			errors.append(f"{k}: empty drawback roll table")
		for o in op:
			if o == k or E[o]["good"] != E[k]["good"] or o not in GEM_EFFECTS:
				errors.append(f"{k}: 'other' roll {o} is the main effect itself, not a gem, or of the wrong kind")
		for d in dp:
			if d in OPPOSITE.get(k, ()):
				errors.append(f"{k}: drawback pool holds its direct opposite {d}")
			if E[d]["good"] == E[k]["good"]:
				errors.append(f"{k}: drawback {d} is the same kind as the main effect")
			if d in NEVER_DRAWBACK:
				errors.append(f"{k}: drawback {d} is on the never-drawback list")
		sizes[k] = (len(op), len(dp))
	for k, o, d in EXAMPLES:
		if o not in other_pool(k) or d not in drawback_pool(k):
			errors.append(f"example {k}: {o} / {d} not in its roll tables")
	notes.append(f"rolls: each of the {len(GEM_EFFECTS)} gems has one roll table; 'other' pools {min(a for a, _ in sizes.values())}-{max(a for a, _ in sizes.values())} effects, all the main effect's kind and never itself; "
		f"drawback pools {min(b for _, b in sizes.values())}-{max(b for _, b in sizes.values())} effects, all the opposite kind, none on the never-drawback list, and none a direct opposite ({len(INVERSE_PAIRS)} inverse pairs, the owner's 12 included)")

	# 10. Weapons: per hit, potency and duration both rise; nothing reaches the strongest tipped arrow.
	for k in GEM_EFFECTS:
		p_prev = d_prev = -1
		for i in range(1, 101):
			p, d = weapon_hit(k, i / 100 * CAP)
			kind = E[k]["kind"]
			if (kind != "onoff" and not p > p_prev) or (kind != "instant" and not d > d_prev):
				errors.append(f"{k}: weapon potency/duration not rising")
				break
			p_prev, d_prev = p, d
		p, d = weapon_hit(k, CAP)
		ticks, _ = arrow_anchor(k)
		if not (p * max(d, 1e-9) < 1.0 * max(ticks / T if E[k]["kind"] != "instant" else 1e-9, 1e-9) and (p < 1.0 or d < ticks / T)):
			errors.append(f"{k}: the best weapon gem matches the strongest tipped arrow")
		if E[k]["kind"] == "scaled" and not p < 1.0:
			errors.append(f"{k}: the best weapon gem's potency reaches the strongest arrow's level, so 'the stronger wins' can't favour the real arrow")
	notes.append("weapons: per hit, potency and duration both rise with strength, for every gem; at the best build every effect stays under the strongest vanilla tipped arrow, so a real strong tipped arrow always wins the 'stronger applies' comparison and a gem arrow beats the weaker ones")
	return errors, notes

# ---------------------------------------------------------------- builds for the tables
def downgrade_rows(k):
	tv = top_variant(k)
	vf = variant_factors(E[k]["variants"])
	best_q = gem_quality(k, tv, BEST_BASE, "strongest")
	rows = []
	lower = [v for v in vf if vf[v] < vf[tv]]
	if lower:
		v2 = max(lower, key=vf.get)
		rows.append((f"{v2} potion instead of {tv}", build_total(["netherite"] * 4, [3] * 4, gem_quality(k, v2, BEST_BASE, "strongest"))))
	rows += [
		("prismarine base instead of nether star", build_total(["netherite"] * 4, [3] * 4, gem_quality(k, tv, "prismarine", "strongest"))),
		("splash potion (tier 2 at best: medium)", build_total(["netherite"] * 4, [3] * 4, gem_quality(k, tv, BEST_BASE, "medium"))),
		("full diamond armor", build_total(["diamond"] * 4, [3] * 4, best_q)),
		("diamond boots break the run", build_total(["netherite"] * 3 + ["diamond"], [3] * 4, best_q)),
		("one gem fewer (11)", build_total(["netherite"] * 4, [3, 3, 3, 2], best_q)),
	]
	return rows

def weapon_downgrade_rows(k):
	tv = top_variant(k)
	vf = variant_factors(E[k]["variants"])
	best_q = gem_quality(k, tv, BEST_BASE, "strongest")
	rows = []
	lower = [v for v in vf if vf[v] < vf[tv]]
	if lower:
		v2 = max(lower, key=vf.get)
		rows.append((f"{v2} potion instead of {tv}", weapon_total("netherite", 2, gem_quality(k, v2, BEST_BASE, "strongest"))))
	rows += [
		("prismarine base instead of nether star", weapon_total("netherite", 2, gem_quality(k, tv, "prismarine", "strongest"))),
		("splash potion (tier 2 at best: medium)", weapon_total("netherite", 2, gem_quality(k, tv, BEST_BASE, "medium"))),
		("diamond weapon", weapon_total("diamond", 2, best_q)),
		("bow, crossbow, trident or mace", weapon_total("bow", 2, best_q)),
		("one gem (the other slot empty or another effect)", weapon_total("netherite", 1, best_q)),
	]
	return rows

BUILDS = [
	("Starter", "1 gem on a lone leather chestplate; normal potion, amethyst, tier 1 weak",
		[None, "leather", None, None], [0, 1, 0, 0], "normal", "amethyst", "weak"),
	("Early", "Full iron set, 1 gem per piece (4); normal potion, quartz, tier 1 weak",
		["iron"] * 4, [1] * 4, "normal", "quartz", "weak"),
	("Mid", "Full diamond set, 2 gems per piece (8); splash of the strong potion, ender pearl, tier 2 medium",
		["diamond"] * 4, [2] * 4, "top", "ender_pearl", "medium"),
	("Late", "Full diamond set, 12 gems; lingering strong potion, diamond, tier 3",
		["diamond"] * 4, [3] * 4, "top", "diamond", "strongest"),
	("Best", "Full netherite set, 12 gems; lingering strong potion, nether star, tier 3",
		["netherite"] * 4, [3] * 4, "top", BEST_BASE, "strongest"),
]
WEAPON_BUILDS = [
	("Starter", "Wooden sword, 1 gem; normal potion, amethyst, tier 1 weak", "wood", 1, "normal", "amethyst", "weak"),
	("Early", "Iron sword, 1 gem; normal potion, quartz, tier 1 weak", "iron", 1, "normal", "quartz", "weak"),
	("Mid", "Diamond sword, 2 gems; splash of the strong potion, ender pearl, tier 2 medium", "diamond", 2, "top", "ender_pearl", "medium"),
	("Late", "Diamond sword, 2 gems; lingering strong potion, diamond, tier 3", "diamond", 2, "top", "diamond", "strongest"),
	("Bow", "Bow, 2 gems; lingering strong potion, nether star, tier 3 (the best ranged)", "bow", 2, "top", BEST_BASE, "strongest"),
	("Best", "Netherite sword, axe or spear, 2 gems; lingering strong potion, nether star, tier 3", "netherite", 2, "top", BEST_BASE, "strongest"),
]
LADDER_EFFECTS = ["speed", "strength", "regeneration", "fire_resistance", "instant_health"]
WEAPON_LADDER_EFFECTS = ["poison", "slowness", "weakness", "instant_damage"]

def build_value(k, b):
	_, _, layout, gems, var, base, grade = b
	if var == "top":
		var = top_variant(k)
	return build_total(layout, gems, gem_quality(k, var, base, grade))

def weapon_build_value(k, b):
	_, _, weapon, gems, var, base, grade = b
	if var == "top":
		var = top_variant(k)
	return weapon_total(weapon, gems, gem_quality(k, var, base, grade))

# ---------------------------------------------------------------- document
def fmt(x, d=2):
	return f"{x:.{d}f}"

def document(notes):
	S = []  # ("h1"|"h2"|"h3"|"p"|"ul"|"table", ...)
	S.append(("h1", "Bubba's Infused Gems: balance model"))
	S.append(("p", "Generated by `tools/gem-balance.py` from the owner's design in `specs/effect-gems.md`. Do not edit by hand: change the script and rerun it. Vanilla numbers come from the decompiled Minecraft 26.3 source and the script re-checks them against it."))
	S.append(("h2", "The formula in plain words"))
	S.append(("ul", [
		"Every effect is measured on one scale: **1.0 = the strongest vanilla level of that effect** (Speed II, Strength II, Regeneration II, Resistance IV from strong Turtle Master...). For on/off effects 1.0 is the full effect. Weapon effects: 1.0 = what the strongest vanilla tipped arrow of that effect applies. Healing has its own, flatter scale.",
		"**One gem's strength** = potion factor x base gem factor x enchant grade x gear factor. On armor the gear factor is armor material x matching run; on a weapon it is the weapon's material. Each is at most 1.0.",
		f"**Armor stacking:** a piece holds {SLOTS} gems, so 12 in all. The gems of one effect are lined up strongest first; the first counts fully, each next one {STACK_R:.0%} of the one before. The sum is scaled so that 12 perfect gems give exactly **{CAP}**.",
		f"**Weapon stacking:** a weapon holds {WEAPON_SLOTS} gems, same rule, scaled so 2 perfect gems give exactly **{CAP}**. One perfect gem gives {CAP / WW:.3f}: two gems of one effect, or one gem each of two effects.",
		f"**The cap is {CAP}**: 5% short of vanilla's best. Only the single best build reaches it: **lingering potion + nether star + tier 3 + full contiguous netherite** (armor), or **+ a netherite weapon with both slots** (weapons).",
		"**Potion form doesn't multiply**: it sets the highest enchant tier (normal 1, splash 2, lingering 3), and the tier sets the grade. So lingering is needed for the strongest grade.",
		"**Base gems change strength only** (owner): no traits. Order, by how hard they are to get: amethyst, quartz, ender pearl, diamond, prismarine, nether star.",
		"A gem's \"other\" effect from the enchant roll uses the same formula with its own grade and joins that effect's stack. A **drawback** roll is a fixed, mild size, whatever the gem or gear (below).",
	]))
	S.append(("p", f"Total for an effect, in symbols: `total = {CAP} x sum(gem_i x {STACK_R}^(rank_i)) / sum({STACK_R}^0..{STACK_R}^(slots-1))`, with `gem = potion x base x grade x gear`."))

	S.append(("h2", "Factor tables"))
	S.append(("h3", "Base gem (strength only)"))
	S.append(("table", ["Base gem", "Factor", "Why here"], [[n, fmt(f), w] for _, n, f, w in BASE_GEMS]))
	S.append(("h3", "Potion variant (by what that effect really has in 26.3)"))
	S.append(("table", ["Variants that exist", "normal", "long", "strong"], [
		["normal + long + strong (Swiftness, Strength, Leaping, Regeneration, Poison, Slowness, Turtle Master)", "0.80", "0.85", "1.00"],
		["normal + long (Fire Resistance, Night Vision, Invisibility, Water Breathing, Slow Falling, Weakness)", "0.90", "1.00", "-"],
		["normal + strong (Healing, Harming)", "0.80", "-", "1.00"],
		["one only (Luck, Wind Charged, Weaving, Oozing, Infested), and every item-sourced or found-only gem but Absorption", "1.00", "-", "-"],
		["Absorption: golden apple (normal) or enchanted golden apple (strong)", "0.80", "-", "1.00"],
	]))
	S.append(("h3", "Potion form -> highest enchant tier (owner)"))
	S.append(("table", ["Form", "Highest tier", "Best grade reachable", "Item-sourced gem (proposal)"], [
		["Normal", "1", "weak", "as brewed from the item"], ["Splash", "2", "medium", "brewed again with gunpowder"],
		["Lingering", "3", "strongest", "then again with dragon's breath"]]))
	S.append(("p", "An item has no splash or lingering form, so an Infused Gem brewed from an item starts at the normal form and is raised the way potions are: put it back in the bottle slot with gunpowder (splash form), then dragon's breath (lingering form). So lingering-level effort is still what opens tier 3, for every craftable gem."))
	S.append(("h3", "Enchant tier outcomes"))
	S.append(("table", ["Tier", "Outcome", "Intended effect", "Extra effect", "Proposed odds"],
		[[str(t), lab, (f"{g} ({GRADES[g]:.2f})" if g else "-"),
			", ".join(f"{kind} {gr}" + (f" ({GRADES[gr]:.2f})" if kind == "other" else f" ({DRAWBACK_ONE:.2f}, fixed)") for kind, gr in ex) or "-", f"{p:.0%}"]
			for t, outs in TIERS.items() for lab, g, ex, p in outs]))
	S.append(("p", "Armor \"other\" rolls from: " + ", ".join(E[k]["name"] for k in ARMOR_OTHER) + ". Weapon \"other\" rolls (a second harmful effect on hit) from: " + ", ".join(E[k]["name"] for k in WEAPON_OTHER) + "."))
	S.append(("h3", "Armor material"))
	S.append(("table", ["Material", "Factor", "Vanilla 26.3 (ArmorMaterials)"],
		[[m, fmt(f), why] for m, (f, why) in sorted(MATERIALS.items(), key=lambda x: (x[1][0], x[0]))]))
	S.append(("h3", "Matching run (helmet - chestplate - leggings - boots)"))
	S.append(("table", ["Run length of the piece", "Factor", "Example"], [
		["1", fmt(RUN[1]), "a lone piece, or helmet + boots of the same material (not touching)"],
		["2", fmt(RUN[2]), "helmet + chestplate"], ["3", fmt(RUN[3]), "helmet + chestplate + leggings"],
		["4", fmt(RUN[4]), "the full set"]]))
	hb = build_total(["netherite", None, None, "netherite"], [3, 0, 0, 3], 1.0)
	hc = build_total(["netherite", "netherite", None, None], [3, 3, 0, 0], 1.0)
	S.append(("p", f"With 6 perfect gems: netherite helmet + boots give {hb:.3f}, netherite helmet + chestplate give {hc:.3f}. Touching pieces win, as the owner asked."))
	S.append(("h3", "Weapon material (proposal: a factor, like armor)"))
	S.append(("p", "Yes for consistency: the same gem is stronger in a better weapon, exactly as in better armor, so a netherite weapon is part of the single best build. Swords, axes and spears come in every material. Bows, crossbows, tridents and maces have none, so they get a fixed diamond-level factor: they can't be upgraded, and at diamond-level the best build stays unique to netherite."))
	S.append(("table", ["Weapon", "Factor", "Vanilla 26.3"],
		[[m, fmt(f), why] for m, (f, why) in sorted({**WEAPON_MATERIALS, **WEAPON_FIXED}.items(), key=lambda x: (x[1][0], x[0]))]))
	S.append(("h3", "Stacking weight per gem (strongest first)"))
	S.append(("table", ["Gem #"] + [str(i + 1) for i in range(12)],
		[["Armor: share of the cap"] + [f"{STACK_R ** i / W:.3f}" for i in range(12)],
		 ["Armor: running total (perfect gems)"] + [f"{CAP * sum(STACK_R ** j for j in range(i + 1)) / W:.3f}" for i in range(12)],
		 ["Weapon: running total (perfect gems)"] + [f"{CAP * sum(STACK_R ** j for j in range(i + 1)) / WW:.3f}" if i < WEAPON_SLOTS else "-" for i in range(12)]]))

	S.append(("h2", "Every status effect: craftable, found-only, or not a gem"))
	S.append(("p", "Every one of the 40 status effects in 26.3 (the script reads them from the source and fails if one is missing). Beneficial effects are armor gems; harmful ones are weapon gems. An effect with a potion is brewed from it. One without a potion is brewed from a vanilla item that already carries it: in 26.3 the brewing stand's bottle slot takes any item that a brewing recipe names as its input (BrewingStandMenu.PotionSlot asks the recipes, PotionIngredient.isPotionInput), so this is data, no menu change. Stews are told apart by their stew effect with Fabric API's components ingredient. An effect with no sensible item is a **found-only** gem: loot and mob gear, never brewed."))
	S.append(("table", ["Effect", "Goes on", "How it's had"],
		[[E[k]["name"], "armor" if E[k]["role"] in ("main", "healing") else "weapon", source_text(k)] for k in GEM_EFFECTS]
		+ [[E[k]["name"], "-", "Drawback only: " + why] for k, why in DRAWBACK_ONLY.items()]
		+ [[k.replace("_", " ").title(), "-", "Not a gem: " + why] for k, why in NOT_GEMS.items()]))
	S.append(("h3", "Found-only gems: what they come with"))
	S.append(("p", "A found gem is made as if brewed and enchanted: its potion factor is 1.0 (one form of the effect), its base gem is picked from the loot source's list and its tier rolled from the source's tiers with the normal tier odds. Same strength scale as crafted gems; only ominous loot can give a perfect one."))
	S.append(("table", ["Loot source", "Base gems", "Tiers", "Strength range (gem quality)"],
		[[name, ", ".join(bases), ", ".join(map(str, tiers)),
			f"{min(BASE[b] for b in bases) * min(GRADES[g] for t in tiers for _, g, _, _ in TIERS[t] if g):.2f} - {max(BASE[b] for b in bases) * max(GRADES[g] for t in tiers for _, g, _, _ in TIERS[t] if g):.2f} (or a drawback-only roll at tier 1)" if 1 in tiers else
			f"{min(BASE[b] for b in bases) * min(GRADES[g] for t in tiers for _, g, _, _ in TIERS[t] if g):.2f} - {max(BASE[b] for b in bases) * max(GRADES[g] for t in tiers for _, g, _, _ in TIERS[t] if g):.2f}"]
			for name, bases, tiers in LOOT.values()]))

	S.append(("h2", "Armor gems: what the scale means in game"))
	cols = [0.05, 0.10, 0.25, 0.50, 0.75, CAP]
	keys = [k for k in E if E[k]["role"] == "main"]
	S.append(("table", ["Effect", "1.0 means (vanilla)"] + [f"s = {c:.2f}" for c in cols],
		[[E[k]["name"], E[k]["anchor"]] + [E[k]["show"](c) for c in cols] for k in keys]))

	S.append(("h2", "Healing gem"))
	S.append(("p", "When the wearer takes damage from an attack or a fall and survives it, they instantly get back a fraction of the health that hit took. A killing blow still kills. Its own curve: `heal = " + f"{HEAL_BEST} x (total / {CAP})^{HEAL_EXP}" + "`, so the best build returns exactly 25% and early gems are flattened upward to stay noticeable."))
	S.append(("p", "**Why 25% at the very best:** it works like 25% less damage from survivable hits, between Resistance I (20%) and Resistance II (40%), and far below the Resistance gem's own best (76%) or full Protection IV armor (64%). It never stops a one-shot, never heals outside a fight, and is always a net loss (you can't farm it with fall damage). A Healing II potion gives 8 HP at once; at 25% the wearer must take 32 HP of hits, about one and a half health bars, to get that much back. So it is a comfort in long fights, not a second health bar."))
	S.append(("table", ["Build", "Gem total", "Share of each hit returned", "A 6 HP zombie hit", "A 20 HP fall (survived)"],
		[[b[0], fmt(build_value("instant_health", b), 3), f"{heal_fraction(build_value('instant_health', b)) * 100:.1f}%",
			f"+{6 * heal_fraction(build_value('instant_health', b)):.2f} HP", f"+{20 * heal_fraction(build_value('instant_health', b)):.1f} HP"] for b in BUILDS]))

	S.append(("h2", "Drawbacks"))
	S.append(("p", f"A drawback roll (tier 1's weak drawback alone, or tier 3's strongest + weak drawback) is a fixed size: **{DRAWBACK_ONE:.0%} of the strongest vanilla level** of that effect, whatever the potion, base gem or gear (a better gem shouldn't hurt more). More drawbacks of the same effect add less and less and never pass {DRAWBACK_CAP:.0%}, which is below vanilla's weakest level of every drawback effect. So they sting, but never as much as a real potion of it."))
	dcols = [1, 2, 3, 12]
	S.append(("h3", "Armor drawbacks (on the wearer, while worn)"))
	S.append(("table", ["Effect", "1.0 means (vanilla)"] + [f"{n} drawback{'s' if n > 1 else ''} ({drawback_total(n):.3f})" for n in dcols],
		[[E[k]["name"], DRAWBACK_ANCHOR[k]] + [(E[k]["wearer"] if "wearer" in E[k] else E[k]["show"])(drawback_total(n)) for n in dcols] for k in ARMOR_DRAWBACKS]))
	S.append(("h3", f"Weapon drawbacks (the inverse: a mild buff on the target, {DRAWBACK_HIT_S} s, refreshed by each hit)"))
	S.append(("table", ["Effect on the target", "1.0 means (vanilla)"] + [f"{n} drawback{'s' if n > 1 else ''} ({drawback_total(n):.3f})" for n in (1, 2)],
		[[E[k]["name"], E[k]["anchor"]] + [E[k]["show"](drawback_total(n)) for n in (1, 2)] for k in WEAPON_DRAWBACKS]))
	S.append(("p", DRAWBACK_EXCLUDED))

	S.append(("h2", "Weapon gems"))
	S.append(("p", f"Harmful gems go on weapons ({WEAPON_SLOTS} slots). Swords, axes, maces, tridents and spears apply the effect to what they hit; bows and crossbows turn each fired arrow into its tipped form carrying the gem's effect (picked up again, it's a plain arrow). Each hit **refreshes** the effect, it never stacks. A real tipped arrow fired from a gemmed bow: the stronger effect applies (higher strength wins, then longer duration), never both."))
	S.append(("p", f"**Per hit:** the weapon's strength `s` is shared equally between how hard and how long: potency = s^{WEAPON_SPLIT} of the strongest arrow's level, duration = s^{1 - WEAPON_SPLIT} of its duration, so potency x duration = s. On/off effects (Wind Charged, Weaving, Oozing, Infested, Glowing, Levitation, Blindness, Nausea, Darkness) put it all into duration. Effects with no tipped arrow are anchored on their strongest vanilla source: a hit source (wither skeleton, spectral arrow, shulker bullet) at its full duration, any other source (food, a mob's pulse or spell) at tipped-arrow scale, 1/8. Harming is a charged strike: the first hit or arrow after a {HARM_CHARGE_S} s charge deals s x 12 magic damage, so a bow can't fire unlimited harming arrows."))
	S.append(("table", ["Effect", "1.0 means (strongest tipped arrow, or strongest vanilla hit)"] + [f"s = {c:.2f}" for c in cols],
		[[E[k]["name"], E[k]["anchor"]] + [E[k]["show"](c) for c in cols] for k in WEAPON_GEMS]))

	S.append(("h3", "How each effect is made partial"))
	S.append(("table", ["Effect", "Implementation", "Source (26.3)"], [[E[k]["name"], E[k]["impl"], E[k]["src"]] for k in E]))
	S.append(("p", "Armor gem effects are attribute modifiers, data components or small tick and damage hooks, not potion effect instances: vanilla levels are whole numbers and their icons and timers would claim a full level. Weapon effects (and weapon drawbacks on the target) are gem mob effects whose amplifier counts strength in 1% steps, with vanilla's icon and timer; a gem arrow carries one as its potion contents with a duration scale of 1. A gem and a drunk potion of the same effect don't add up: the stronger applies (open question)."))

	S.append(("h2", "Progression ladders"))
	S.append(("h3", "Armor"))
	S.append(("table", ["Build", "What it is"] + [E[k]["name"] for k in LADDER_EFFECTS],
		[[b[0], b[1]] + [f"{build_value(k, b):.3f}: {E[k]['show'](build_value(k, b))}" for k in LADDER_EFFECTS] for b in BUILDS]))
	S.append(("h3", "One step down from the best armor (Speed)"))
	S.append(("table", ["Change from the best build", "Total", "In game"],
		[["(best)", fmt(CAP, 3), E["speed"]["show"](CAP)]] + [[lab, fmt(t, 3), E["speed"]["show"](t)] for lab, t in downgrade_rows("speed")]))
	S.append(("h3", "Weapons"))
	S.append(("table", ["Build", "What it is"] + [E[k]["name"] for k in WEAPON_LADDER_EFFECTS],
		[[b[0], b[1]] + [f"{weapon_build_value(k, b):.3f}: {E[k]['show'](weapon_build_value(k, b))}" for k in WEAPON_LADDER_EFFECTS] for b in WEAPON_BUILDS]))
	S.append(("h3", "One step down from the best weapon (Poison)"))
	S.append(("table", ["Change from the best build", "Total", "In game"],
		[["(best)", fmt(CAP, 3), E["poison"]["show"](CAP)]] + [[lab, fmt(t, 3), E["poison"]["show"](t)] for lab, t in weapon_downgrade_rows("poison")]))

	S.append(("h2", "Checks (all passed)"))
	S.append(("ul", notes))

	S.append(("h2", "Open questions for the owner"))
	S.append(("ol", [
		f"Cap at {CAP} (5% short of the strongest vanilla level)? yes / no (0.90 would make the gap easier to feel).",
		"Gold armor and gold weapons: (a) chainmail-level factor 0.83 (model), (b) a magic bonus up to diamond-level 0.93, echoing gold's high enchantability.",
		"Turtle helmet counts as its own material (iron-level, always a run of 1)? yes / no.",
		"Resistance's 1.0: (a) Resistance IV from strong Turtle Master, 80% less damage (the rule as written; gems up to 76%), (b) Resistance II, the beacon level, 40% (gems up to 38%).",
		"Turtle Master gems also carry Turtle Master's Slowness, on the wearer? yes / no.",
		f"Healing gem returns at most {HEAL_BEST:.0%} of each survivable hit (early iron kit about {heal_fraction(build_value('instant_health', BUILDS[1])) * 100:.0f}%)? yes / no.",
		f"Drawback size: one drawback is {DRAWBACK_ONE:.0%} of the strongest vanilla level, many of one effect stay under {DRAWBACK_CAP:.0%}, the same whatever the gem or gear? yes / no.",
		"Poison and wither never roll as armor drawbacks? yes / no (the alternative is a non-lethal poison that stops at half health).",
		"Weapon drawbacks give the target speed, regeneration or resistance (no others)? yes / no.",
		"Weapon material is a strength factor, like armor (wood ... netherite)? yes / no.",
		"Bows, crossbows, tridents and maces (no material): (a) diamond-level 0.93, so only netherite weapons reach the cap (model), (b) netherite-level 1.00.",
		"Weapon strength per hit: (a) shared between potency and duration (model), (b) all into potency, always the strongest arrow's duration.",
		f"Harming weapon gems strike once per {HARM_CHARGE_S} s charge (not every hit)? yes / no.",
		"The weapon's 1.0 is the strongest vanilla tipped arrow (Poison II 2.7 s, Slowness IV 2.5 s), not the drunk potion (Poison II 21.6 s)? yes / no.",
		"Wither's 1.0: (a) the wither skeleton's hit, Wither I for 10 s (model: a weapon matches a weapon), (b) the Wither boss's skull, Wither II for 10 s.",
		"Item-sourced Infused Gems reach splash and lingering form by brewing them again with gunpowder, then dragon's breath? yes / no.",
		"Wither gems from the wither rose? yes / no (the alternative is found-only, from Wither-fight loot).",
		"Saturation gems: (a) from suspicious stew of dandelion, golden dandelion or blue orchid (model), (b) from a golden dandelion itself, (c) found-only.",
		"Blindness gems from suspicious stew of azure bluet or open eyeblossom, and Nausea gems from pufferfish? yes / no.",
		"Darkness gems from echo shards (ancient cities)? yes / no (the alternative is found-only, in ancient cities).",
		"Conduit Power gems from a heart of the sea, and Dolphin's Grace gems from a nautilus shell? yes / no.",
		"Haste, Health Boost, Luck and Hero of the Village are found-only, in the places listed? yes / no.",
		"Hero of the Village gems: villager trades up to 52% cheaper at the best build, while worn? yes / no (a permanent discount is strong; the alternative is not a gem).",
		"Mining Fatigue gems come only from elder guardians (a rare extra drop) and ominous vaults? yes / no.",
		"Bad Omen, Trial Omen, Raid Omen and Breath of the Nautilus are not gems? yes / no.",
		"A gem and a drunk potion of the same effect: the stronger applies, they don't add? yes / no.",
		"Undead follow vanilla's inversion (a healing gem hurts a zombie wearer, harming heals an undead target; undead ignore poison and regeneration)? yes / no.",
		"Invisibility gems give stealth only (mobs notice you later, hidden on the locator bar), never a see-through body? yes / no.",
		"Enchant odds as proposed (tier 1: 50/50; tier 2: medium alone 50%, + weak other 50%; tier 3: alone 40%, + weak other 30%, + medium other 15%, + weak drawback 15%)? yes / no.",
	]))
	return S

INLINE = [(re.compile(r"`([^`]+)`"), r"<code>\1</code>"), (re.compile(r"\*\*([^*]+)\*\*"), r"<strong>\1</strong>")]

def md_cell(x):
	return str(x).replace("|", "\\|")

def render_md(S):
	out = ["<!-- dte:C4 -->", "<!-- Generated by tools/gem-balance.py; do not edit by hand. -->"]
	for item in S:
		kind = item[0]
		if kind in ("h1", "h2", "h3"):
			out += ["", "#" * int(kind[1]) + " " + item[1], ""]
		elif kind == "p":
			out += [item[1], ""]
		elif kind == "ul":
			out += [f"- {x}" for x in item[1]] + [""]
		elif kind == "ol":
			out += [f"{i}. {x}" for i, x in enumerate(item[1], 1)] + [""]
		elif kind == "table":
			_, head, rows = item
			out.append("| " + " | ".join(md_cell(h) for h in head) + " |")
			out.append("|" + "|".join("---" for _ in head) + "|")
			out += ["| " + " | ".join(md_cell(c) for c in r) + " |" for r in rows]
			out.append("")
	return re.sub(r"\n{3,}", "\n\n", "\n".join(out)).strip() + "\n"

def inline_html(text):
	t = html.escape(str(text), quote=False)
	for pat, rep in INLINE:
		t = pat.sub(rep, t)
	return t

def render_html(S):
	body = []
	for item in S:
		kind = item[0]
		if kind in ("h1", "h2", "h3"):
			body.append(f"<{kind}>{inline_html(item[1])}</{kind}>")
		elif kind == "p":
			body.append(f"<p>{inline_html(item[1])}</p>")
		elif kind in ("ul", "ol"):
			body.append(f"<{kind}>" + "".join(f"<li>{inline_html(x)}</li>" for x in item[1]) + f"</{kind}>")
		elif kind == "table":
			_, head, rows = item
			body.append('<div class="scroll"><table><thead><tr>' + "".join(f"<th>{inline_html(h)}</th>" for h in head)
				+ "</tr></thead><tbody>" + "".join("<tr>" + "".join(f"<td>{inline_html(c)}</td>" for c in r) + "</tr>" for r in rows)
				+ "</tbody></table></div>")
	css = """
:root { --bg:#fbfaf7; --fg:#1d1d1b; --muted:#5d5b55; --line:#dedad0; --head:#efece4; --accent:#6b3fa0; }
@media (prefers-color-scheme: dark) { :root { --bg:#17161a; --fg:#e9e6ee; --muted:#a7a2b0; --line:#34313a; --head:#232129; --accent:#c4a2f0; } }
body { background:var(--bg); color:var(--fg); font:15px/1.55 system-ui, sans-serif; margin:0 auto; padding:24px 16px 64px; max-width:1200px; }
h1 { color:var(--accent); margin-top:0; } h2 { border-bottom:2px solid var(--line); padding-bottom:4px; margin-top:40px; }
h3 { margin-top:28px; } p, li { max-width:75ch; } code { background:var(--head); padding:1px 4px; border-radius:3px; font-size:13px; }
.scroll { overflow-x:auto; } table { border-collapse:collapse; margin:8px 0 16px; font-size:13.5px; }
th, td { border:1px solid var(--line); padding:5px 8px; text-align:left; vertical-align:top; }
th { background:var(--head); } tbody tr:nth-child(even) td { background:color-mix(in srgb, var(--head) 40%, transparent); }
"""
	return ("<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
		f"<title>Infused Gems Balance</title><style>{css}</style></head><body>\n" + "\n".join(body) + "\n</body></html>\n")

def main():
	errors, notes = run_checks()
	for n in notes:
		print("ok   " + n)
	if errors:
		for e in errors:
			print("FAIL " + e, file=sys.stderr)
		print(f"{len(errors)} check(s) failed; nothing written.", file=sys.stderr)
		sys.exit(1)
	if "--check" in sys.argv:
		return
	S = document(notes)
	OUT_MD.parent.mkdir(parents=True, exist_ok=True)
	OUT_MD.write_text(render_md(S), encoding="utf-8", newline="\n")
	OUT_HTML.parent.mkdir(parents=True, exist_ok=True)
	OUT_HTML.write_text(render_html(S), encoding="utf-8", newline="\n")
	print(f"wrote {OUT_MD.relative_to(ROOT)} and {OUT_HTML.relative_to(ROOT)}")

if __name__ == "__main__":
	main()
