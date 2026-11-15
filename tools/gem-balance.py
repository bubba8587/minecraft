#!/usr/bin/env python3
# dte:C4
"""Bubba's Infused Gems balance model and table generator (specs/effect-gems.md, "Balance model").

  python tools/gem-balance.py           # run the checks, then write the tables
  python tools/gem-balance.py --check   # run the checks only, write nothing (fails if the mod export is stale)
  python tools/gem-balance.py --export  # run the checks, then write only the mod export (balance.json, brewing recipes)

Writes docs/research/effect-gems-balance.md and build/gem-balance.html (a page for the
owner to open in a browser). Every check failure is printed and exits non-zero, and
nothing is written.

Vanilla numbers are hard-coded below with the 26.3 source file they come from. When the
decompiled source exists (build/mcsrc/26.3/common, made by tools/mcsrc.py) the script
re-reads the potion list and the effect constants from it and fails if any differ, so a
new Minecraft release can't silently change the anchors. Standard library only.
"""
import html, itertools, json, math, random, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "build" / "mcsrc" / "26.3" / "common" / "net" / "minecraft"
OUT_MD = ROOT / "docs" / "research" / "effect-gems-balance.md"
OUT_HTML = ROOT / "build" / "gem-balance.html"

# ---------------------------------------------------------------- tunable model constants
CAP = 0.90            # best possible total, as a fraction of the strongest vanilla level (armor and weapons; owner, question 1)
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

# Healing and Harming on a weapon (owner, Q19): on every hit, no charge. Per hit at strength 1.0, kept a modest bonus,
# below what Sharpness V adds (+3 per hit); never under the floor, so the weakest gem still shows.
HIT_STRIKE_HARM = 2.5   # magic damage per hit at 1.0 (the best weapon, 0.90: 2.25)
HIT_STRIKE_HEAL = 2.0   # HP healed per hit at 1.0 (the best weapon: 1.8)
HIT_STRIKE_FLOOR = 1.0  # at least half a heart per hit
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
# One Infused Gem item (owner): the base gem is its Infusion level, I (amethyst) to VI (nether star).
INFUSION = {b: r for (b, _, _, _), r in zip(BASE_GEMS, ["I", "II", "III", "IV", "V", "VI"])}
INFUSION_NAME = {"amethyst": "amethyst", "quartz": "quartz", "ender_pearl": "ender pearl", "diamond": "diamond",
	"prismarine": "prismarine shard", "nether_star": "nether star"}

def inf(b):
	return f"Infusion {INFUSION[b]} ({INFUSION_NAME[b]})"
BEST_BASE = "nether_star"

# Armor materials (ArmorMaterials.java, 26.3: durability multiplier and chest defense quoted
# for orientation). MAIN_LADDER is the progression the checks hold strictly increasing.
MATERIALS = {
	"leather":   (0.75, "durability x5, chest 3, enchantability 15"),
	"copper":    (0.79, "durability x11, chest 4, enchantability 8 (26.x copper armor)"),
	"chainmail": (0.83, "durability x15, chest 5, enchantability 12"),
	"gold":      (0.93, "durability x7, chest 5, enchantability 25: diamond-level magic bonus (owner, question 2)"),
	"iron":      (0.87, "durability x15, chest 6, enchantability 9"),
	"diamond":   (0.93, "durability x33, chest 8, toughness 2"),
	"netherite": (1.00, "durability x37, chest 8, toughness 3, knockback resistance 0.1"),
}
MAIN_LADDER = ["leather", "copper", "chainmail", "iron", "diamond", "netherite"]
# The turtle helmet takes no gems for now (owner, question 3): the mod's not_socketable tag refuses it.
HELMET_ONLY = set()
# Matching run: a piece's factor from the length of the unbroken same-material run it sits in.
RUN = {1: 0.85, 2: 0.90, 3: 0.95, 4: 1.00}

# Weapon material (proposal: yes, a factor, for consistency with armor). ToolMaterial.java, 26.3.
# Swords, axes and spears come in every material; the others have none and get a fixed factor.
WEAPON_MATERIALS = {
	"wood":      (0.75, "durability 59, enchantability 15"),
	"stone":     (0.79, "durability 131, enchantability 5"),
	"copper":    (0.83, "durability 190, enchantability 13 (26.x copper tools)"),
	"gold":      (0.93, "durability 32, enchantability 22: diamond-level magic bonus, like gold armor (owner, question 2)"),
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
# (label, intended-effect grade or None, [(other kind, grade)], odds). Owner (Q34): the plain result is the most common at
# every tier, so randomness stays controllable for people who invest a lot.
TIERS = {
	1: [("Weak intended effect", "weak", [], 0.75),
		("Weak intended + weak drawback", "weak", [("drawback", "weak")], 0.20),
		("Weak drawback alone", None, [("drawback", "weak")], 0.05)],
	2: [("Medium intended alone", "medium", [], 0.55),
		("Medium intended + weak other", "medium", [("other", "weak")], 0.30),
		("Medium intended + weak drawback", "medium", [("drawback", "weak")], 0.15)],
	3: [("Strongest alone", "strongest", [], 0.50),
		("Strongest + weak other", "strongest", [("other", "weak")], 0.25),
		("Strongest + medium other", "strongest", [("other", "medium")], 0.10),
		("Strongest + weak drawback", "strongest", [("drawback", "weak")], 0.10),
		("Strongest + medium drawback", "strongest", [("drawback", "medium")], 0.05)],
}

def drawback_size(grade):
	"""A drawback roll's fixed size: DRAWBACK_ONE for a weak one, scaled by the grade ratio for a medium one."""
	return DRAWBACK_ONE * GRADES[grade] / GRADES["weak"]

def drawback_sum(sizes):
	"""Several drawbacks of one effect saturate toward DRAWBACK_CAP (n weak ones: drawback_total(n))."""
	return DRAWBACK_CAP * (1 - math.prod(1 - x / DRAWBACK_CAP for x in sizes))

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
#       duration scales; instant = a strike on every hit. Beneficial effects are scaled unless said otherwise.
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
effect("strength", name="Strength", potion="strength", role="main", level_one=0.5,
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
effect("resistance", name="Resistance", potion=None, role="main", level_one=0.5, variants={"normal": None},
	source=("found", ["rare", "hard", "ominous"], "woodland mansion chests (rare), bastion treasure (hard); also an 'other' roll on beneficial gems; no potion gives Resistance alone"),
	anchor="Resistance II (beacon level, owner): damage taken -40% ((level x 5) / 25)",
	value=lambda s: 0.4 * s, show=lambda s: f"damage taken {pct(-0.4 * s)}",
	impl="Damage hook (mixin at LivingEntity.getDamageAfterMagicAbsorb, beside vanilla's resistance), x (1 - 0.4 x s), skipping bypasses_resistance damage. With Turtle Master gems or a vanilla Resistance effect, the strongest reduction applies.",
	src="LivingEntity.getDamageAfterMagicAbsorb, BeaconBlockEntity")
# A combined gem (owner): the Turtle Master potion's Resistance and Slowness together, one effect "turtle_master" (a mod
# status effect, since vanilla has none), both parts scaled by the same strength against the strong potion. Its
# Slowness is part of the effect (full strength, owner), never a drawback, and doesn't make the gem harmful.
effect("turtle_master", name="Turtle Master", potion="turtle_master", role="main", level_one=0.75,
	variants={"normal": (400, 2), "long": (800, 2), "strong": (400, 3)},
	anchor="Strong Turtle Master, as drunk: Resistance IV (damage taken -80%) and Slowness VI (speed -90%) together, 20 s",
	value=lambda s: 0.8 * s, show=lambda s: f"damage taken {pct(-0.8 * s)} and {pct(-0.9 * s)} speed",
	impl="Both parts at strength s: the resistance damage hook x (1 - 0.8 x s) and a movement_speed modifier -0.9 x s (add_multiplied_total). Against Resistance gems the strongest reduction applies.",
	src="Potions.STRONG_TURTLE_MASTER (Slowness 400 ticks amplifier 5, Resistance amplifier 3)")
COMPOUND = {"turtle_master": {"resistance": 0.8, "slowness": -0.9}}  # a mod status effect, not one of vanilla's
SLOWNESS_AMP = {"normal": 3, "long": 3, "strong": 5}  # Turtle Master's Slowness amplifiers (Potions.java)
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
INVISIBILITY_LOCATOR_HIDE_AT = 0.5   # owner (Q33): from this strength the wearer is hidden on the locator bar
INVISIBILITY_FULL_AT = CAP            # owner (Q33): only the perfect complete set gives real vanilla invisibility
effect("invisibility", name="Invisibility", potion="invisibility", role="main",
	variants={"normal": (3600, 0), "long": (9600, 0)},
	anchor="Invisibility with no armor: mobs see you at 7% range (0.7 x 10% minimum armor cover). In full armor vanilla gives only 70%.",
	value=lambda s: 0.93 * s,
	show=lambda s: (f"mobs notice you at {100 * (1 - 0.93 * s):.0f}% range" + (", hidden on the locator bar" if s >= INVISIBILITY_LOCATOR_HIDE_AT else "")
		+ (", and real invisibility (the perfect complete set)" if s >= INVISIBILITY_FULL_AT - 1e-9 else "")),
	impl=f"Stealth that scales (owner, Q33): mobs' visibility check x (1 - 0.93 x s) (hook at LivingEntity.getVisibilityPercent), hidden on the locator bar from s {INVISIBILITY_LOCATOR_HIDE_AT} (waypoint_transmit_range -100%), and only the perfect complete set (s = the cap) gives the real vanilla effect, see-through body and all; armor renders as in vanilla.",
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
		"fishing treasure and shipwrecks (common), buried treasure (rare)"),
	anchor="Luck I: luck +1 (only level)",
	value=lambda s: s, show=lambda s: f"+{s:.2f} luck",
	impl="Attribute modifier on luck, +s (exact).",
	src="MobEffects.LUCK")
effect("instant_health", name="Healing", potion="healing", role="healing", kind="instant",
	variants={"normal": (1, 0), "strong": (1, 1)},
	unit=HIT_STRIKE_HEAL, unit_fmt=lambda x: f"heals the target {x:.1f} HP",
	hit_anchor_text="Strongest potion (strong Healing): Healing II, 8 HP at once (4 << level)",
	anchor="Its own scale: 1.0 on the gem scale returns 25% of each hit (the best build, 0.95, returns exactly 25%)",
	value=heal_fraction,
	show=lambda s: f"{heal_fraction(s) * 100:.1f}% of each non-lethal hit back (6 HP hit: +{6 * heal_fraction(s):.2f} HP)",
	impl=("After the wearer takes damage from an attack (an entity source) or a fall and survives it, heal that "
		"damage instance's health loss x the gem's fraction, at once. A killing blow still kills (the heal runs "
		"after the hit, only if the wearer is alive). Mixin after LivingEntity.actuallyHurt. Works the same on mobs."),
	src="LivingEntity.actuallyHurt, DamageTypeTags.IS_FALL, HealOrHarmMobEffect")
# Harmful gems: their anchor is what the strongest potion of the effect gives, as drunk (the hit-applied curve; owner).
effect("poison", name="Poison", potion="poison", role="harmful", kind="scaled", level_one=0.48,
	variants={"normal": (900, 0), "long": (1800, 0), "strong": (432, 1)},
	unit=25 / 12 * 0.8, unit_fmt=lambda x: f"{x:.2f} HP/s poison",
	anchor="Strongest potion (strong Poison), as drunk: Poison II for 21.6 s, 1 HP every 12 ticks (25 >> level) = 1.67 HP/s, never below 1 HP",
	impl="Gem Poison mob effect (vanilla's icon and rules; potency in 1% steps): never below 1 HP, undead immune, milk clears it.",
	src="PoisonMobEffect, Potions.STRONG_POISON")
effect("slowness", name="Slowness", potion="slowness", role="harmful", kind="scaled", level_one=0.25,
	variants={"normal": (1800, 0), "long": (4800, 0), "strong": (400, 3)},
	unit=0.6, unit_fmt=lambda x: f"{pct(-x)} speed",
	anchor="Strongest potion (strong Slowness), as drunk: Slowness IV for 20 s, movement speed -60% (-0.15 per level)",
	wearer=lambda s: f"{pct(-0.6 * s)} speed",
	impl="Gem Slowness mob effect: movement_speed -0.6 x potency (add_multiplied_total). As a drawback: the same modifier, permanent while worn.",
	src="MobEffects.SLOWNESS, Potions.STRONG_SLOWNESS")
effect("weakness", name="Weakness", potion="weakness", role="harmful", kind="scaled", level_one=1.0,
	variants={"normal": (1800, 0), "long": (4800, 0)},
	unit=4.0, unit_fmt=lambda x: f"-{x:.2f} attack damage",
	anchor="Strongest potion (long Weakness), as drunk: Weakness I for 4:00, attack damage -4 (only level)",
	wearer=lambda s: f"-{4 * s:.2f} attack damage",
	impl="Gem Weakness mob effect: attack_damage -4 x potency. As a drawback: the same modifier, permanent while worn.",
	src="MobEffects.WEAKNESS, Potions.LONG_WEAKNESS")
effect("instant_damage", name="Harming", potion="harming", role="harmful", kind="instant",
	variants={"normal": (1, 0), "strong": (1, 1)},
	unit=HIT_STRIKE_HARM, unit_fmt=lambda x: f"+{x:.1f} magic damage",
	anchor="Strongest potion (strong Harming): Harming II, 12 magic damage at once (6 << level)",
	impl=f"On a weapon: every hit or arrow deals +{HIT_STRIKE_HARM} x s magic damage (at least {HIT_STRIKE_FLOOR}). Undead are healed instead, as in vanilla. In armor: the Healing gem's mirror.",
	src="HealOrHarmMobEffect")
for key, name, extra in [
		("wind_charged", "Wind Charged", "a wind burst when the target dies"),
		("weaving", "Weaving", "cobwebs where the target dies"),
		("oozing", "Oozing", "2 slimes when the target dies"),
		("infested", "Infested", "1-2 silverfish when the target is hurt (vanilla 10% per hit)")]:
	effect(key, name=name, potion=key, role="harmful", kind="onoff", variants={"normal": (3600, 0)},
		unit=None, unit_fmt=None,
		anchor=f"Potion of {name}, as drunk: {name} I for 3:00: {extra} (only level)",
		impl="The vanilla effect itself (it has one level), for a duration scaled by s.",
		src=f"{''.join(p.title() for p in key.split('_'))}MobEffect, Potions.{key.upper()}")
# Armor gems with no brewable potion: an item that carries the effect in vanilla goes in the bottle slot
# (26.3's bottle slot takes any item a brewing recipe names as input), or the gem is found-only.
effect("haste", name="Haste", potion=None, role="main", level_one=0.5, variants={"normal": None},
	source=("found", ["common", "rare", "hard", "ominous"], "mineshaft chests (common), stronghold chests (rare), ancient city chests (hard); only beacons give Haste in vanilla"),
	anchor="Haste II (beacon): mining speed x1.4 (+0.2 per level), attack speed +20%",
	value=lambda s: 0.4 * s, show=lambda s: f"mining {pct(0.4 * s)}, attack speed {pct(0.2 * s)}",
	impl="Attribute modifiers: block_break_speed +0.4 x s and attack_speed +0.2 x s (add_multiplied_total).",
	src="Player.getDestroySpeed, MobEffects.HASTE, BeaconBlockEntity")
effect("absorption", name="Absorption", potion=None, role="main", level_one=0.25, variants={"normal": None, "strong": None},
	source=("item", "golden apple (Absorption I: normal) or enchanted golden apple (Absorption IV: strong)"),
	anchor="Absorption IV (enchanted golden apple): 16 absorption HP (4 x (level + 1))",
	value=lambda s: 16 * s, show=lambda s: f"{16 * s:.1f} HP shield, full when put on, refills 30 s after it was last hit",
	impl="max_absorption +16 x s, filled when the gems are put on (at most once per 30 s, owner), refilled to full 30 s after it was last hit.",
	src="AbsorptionMobEffect, Consumables.GOLDEN_APPLE / ENCHANTED_GOLDEN_APPLE")
effect("health_boost", name="Health Boost", potion=None, role="main", level_one=0.5, variants={"normal": None},
	source=("found", ["hard", "ominous"], "end city and ancient city chests (hard); no vanilla item or source gives Health Boost"),
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
HERO_BEST_DISCOUNT = 0.20  # owner (Q28): the best build makes trades at most 20% cheaper; the gem scale is HERO_BEST_DISCOUNT / CAP at 1.0
HERO_CUT = HERO_BEST_DISCOUNT / CAP
effect("hero_of_the_village", name="Hero of the Village", potion=None, role="main", level_one=0.3 / 0.55, variants={"normal": None},
	source=("found", ["common", "rare", "ominous"], "hero-of-the-village gifts (common), pillager outpost chests (rare); only winning a raid gives it in vanilla"),
	anchor="Hero of the Village V (a level-5 raid won): villager prices cut by 0.3 + 0.0625 x 4 = 55% of the base cost",
	value=lambda s: HERO_CUT * s, show=lambda s: f"villager trades {HERO_CUT * s * 100:.0f}% cheaper",
	impl=f"Price hook beside vanilla's (the stronger applies): the hero modifier {HERO_CUT:.3f} x s, so the best build is {HERO_BEST_DISCOUNT:.0%} cheaper (owner) (mixin at Villager.updateSpecialPrices). Villagers' gifts stay vanilla-only (the effect itself).",
	src="Villager.updateSpecialPrices, Raid")
# More weapon gems (harmful effects with no potion): the anchor is the strongest vanilla source.
# A source that is a hit (arrow, bullet, mob attack) applies its full duration; any other source (food,
# area pulse, spell) also counts at its full duration: weapons are measured against potions and full vanilla
# sources, not tipped arrows (owner, 2026-11-14). is_hit only records what kind of source it is.
def hit_anchor(ticks, amp, is_hit):
	return (ticks, amp)
effect("wither", name="Wither", potion=None, role="harmful", kind="scaled", level_one=0.5, variants={"normal": None},
	source=("item", "wither rose (dropped where the Wither kills a mob)"),
	hit=hit_anchor(200, 1, True), unit=1.0, unit_fmt=lambda x: f"{x:.2f} HP/s wither",
	anchor="The Wither boss's skull (owner, Q21): Wither II, 1 HP every 20 ticks = 1.0 HP/s (can kill, unlike poison)",
	impl="Gem Wither mob effect (vanilla's icon; potency in 1% steps); it can kill, as vanilla's does.",
	src="WitherSkeleton.doHurtTarget, WitherMobEffect")
effect("mining_fatigue", name="Mining Fatigue", potion=None, role="harmful", kind="scaled", level_one=1 / 3, variants={"normal": None},
	source=("found", ["hard", "ominous"], "elder guardian drops (a rare extra drop, hard); vanilla's only source is the elder guardian"),
	hit=hit_anchor(6000, 2, False), unit=1.0, unit_fmt=lambda p: f"mining x{0.027 ** p:.2f}, attack speed {pct(-0.3 * p)}",
	anchor="Elder guardian: Mining Fatigue III (mining x0.027, attack speed -30%) for 5:00",
	wearer=lambda s: f"mining x{0.027 ** s:.2f}, attack speed {pct(-0.3 * s)}",
	impl="Gem Mining Fatigue mob effect: block_break_speed x 0.027^potency and attack_speed -0.3 x potency. As a drawback: the same, permanent while worn.",
	src="ElderGuardian, Player.getDestroySpeed")
effect("hunger", name="Hunger", potion=None, role="harmful", kind="scaled", level_one=1 / 3, variants={"normal": None},
	source=("item", "rotten flesh (Hunger I, 80% chance in vanilla)"),
	hit=hit_anchor(300, 2, False), unit=0.3, unit_fmt=lambda x: f"exhaustion +{x / 20:.4f}/tick",
	anchor="Pufferfish: Hunger III (exhaustion +0.015 per tick) for 15 s",
	wearer=lambda s: f"extra hunger: 1 food point per {4 / (0.3 * s):.0f} s",
	impl="Gem Hunger mob effect: causeFoodExhaustion(0.015 x potency) per tick (players only; mobs have no food). As a drawback: the same, permanent while worn.",
	src="HungerMobEffect, Consumables.PUFFERFISH / ROTTEN_FLESH")
for key, name, src_item, ticks, is_hit, how, src in [
		("glowing", "Glowing", "glowstone dust", 200, True, "Spectral arrow hit: Glowing for 10 s", "SpectralArrow"),
		("levitation", "Levitation", "shulker shell", 200, True, "Shulker bullet hit: Levitation I for 10 s", "ShulkerBullet"),
		("blindness", "Blindness", "suspicious stew of azure bluet or open eyeblossom (Fabric API's components ingredient)", 400, False,
			"Illusioner's spell: Blindness for 20 s", "Illusioner, Blocks.AZURE_BLUET"),
		("nausea", "Nausea", "pufferfish", 300, False, "Pufferfish: Nausea for 15 s", "Consumables.PUFFERFISH"),
		("darkness", "Darkness", "echo shard (ancient cities)", 260, False, "Warden's pulse: Darkness for 13 s", "Warden")]:
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
# heals the target). Their hit 1.0 is the strongest potion of the effect, as drunk, at its worn 1.0 level (owner:
# potions, not tipped arrows). With no potion, the strongest vanilla source (Absorption: the enchanted golden
# apple), or, with none, a standard 3:00 potion (proposal).
DEFAULT_HIT_TICKS = 3600
E["absorption"]["hit"] = (2400, 3)

def arrow_anchor(key):
	"""(ticks, amplifier) of the hit 1.0: the strongest potion as drunk (highest level; ties: longest), or the hit anchor."""
	e = E[key]
	if "hit" in e:
		return e["hit"]
	if not e["potion"]:
		return DEFAULT_HIT_TICKS, 0
	v = max(e["variants"].values(), key=lambda tv: (tv[1], tv[0]))
	ticks, amp = v
	return ticks, amp

# Hit duration (owner, 2026-11-14): potency is measured against the strongest potion level, but a hit lasts seconds, not
# a potion's minutes: MAX_HIT_SECONDS at the best weapon build, less with less strength, never below the effect's floor.
# Each hit refreshes, it never adds.
MAX_HIT_SECONDS = 8.0
# Floors (owner: every hit must do something a player notices). Seconds, or a heal amount for Regeneration.
#   poison, wither: the hit's own 10-tick damage cooldown, then at least 3 damage ticks (every 10 ticks), plus margin
#   regeneration: long enough to heal at least 1 heart (2 HP) at the hit's potency
#   darkness: vanilla fades it in over 22 ticks, so a floor past that; nausea fades in over 150 ticks: a floor long
#   enough for the spin to be well under way
#   everything else: 2 s
HIT_FLOOR_SECONDS_DEFAULT = 2.0
HIT_FLOOR_SECONDS = {"poison": 2.5, "wither": 2.5, "darkness": 3.0, "nausea": 5.0, "hunger": 4.0}  # hunger: owner
HIT_FLOOR_HEAL_HP = {"regeneration": 2.0}
# Hunger on hit (owner, "bit of both"): twice Hunger III's drain while it lasts, at least 4 s, and every hit costs a player
# target at least one food point's worth of exhaustion (4.0); whatever the effect won't drain in time is added at the hit.
HIT_HUNGER_RATE = 2 * 0.015
HIT_FLOOR_EXHAUSTION = {"hunger": 4.0}

def hit_floor(key, potency):
	if key in HIT_FLOOR_HEAL_HP:
		return HIT_FLOOR_HEAL_HP[key] / (HIT_UNIT[key] * potency)
	return HIT_FLOOR_SECONDS.get(key, HIT_FLOOR_SECONDS_DEFAULT)

def weapon_hit(key, s):
	"""(potency fraction of the strongest potion level, duration in seconds) one hit applies at weapon strength s."""
	e = E[key]
	if e["kind"] == "scaled":
		p = s ** WEAPON_SPLIT
		return p, max(hit_floor(key, p), MAX_HIT_SECONDS * (s / CAP) ** (1 - WEAPON_SPLIT))
	if e["kind"] == "onoff":
		return 1.0, max(hit_floor(key, 1.0), MAX_HIT_SECONDS * s / CAP)
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
	return f"{e['unit_fmt'](max(HIT_STRIKE_FLOOR, p * e['unit']))} every hit"

def hit_anchor_text(key):
	e = E[key]
	if "hit_anchor_text" in e:
		return e["hit_anchor_text"]
	if not e["good"]:
		return e["anchor"]
	ticks, _ = arrow_anchor(key)
	level = e["anchor"].split(":")[0]
	if key == "absorption":
		return f"{level} for {ticks / T:.0f} s (the enchanted golden apple)"
	if not e["potion"]:
		return f"{level} for {ticks / T:.0f} s (no vanilla timed source: a 3:00 potion, proposal)"
	return f"Strongest potion, as drunk: {level} for {ticks / T:.0f} s"

# Harmful gems worn (pointless, but allowed: a poison gem in your armor poisons you, owner).
DEATH_TRIGGER = {"wind_charged": "a wind burst", "weaving": "cobwebs", "oozing": "2 slimes"}
ALWAYS_ON = {"glowing"}  # worn: on all the time (owner, Q14); can't be partial
# Worn screen effects made partial and steady on the wearer's own screen (owner after playtest, 2026-11-15: one gem gave
# total darkness that blinked): s of the way to vanilla's strongest look. Darkness: lightmap dim and fog; Blindness: fog;
# Nausea: wobble. No vanilla effect instance on the wearer, so nothing pulses or restarts.
SCREEN_EFFECTS = {"darkness": "a steady dimming", "blindness": "fog closing in", "nausea": "a screen wobble"}
LEVITATION_RISE = 0.02  # worn Levitation: rise speed at strength 1.0, blocks per tick (vanilla Levitation I: 0.05), owner Q14: a slow drift
PULSED = ALWAYS_ON | set(SCREEN_EFFECTS) | {"levitation"}

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
	if key == "levitation":
		return f"drifts the wearer up at {LEVITATION_RISE * s * T * 0.98:.2f} blocks/s, all the time"
	if key in SCREEN_EFFECTS:
		return f"{SCREEN_EFFECTS[key]} on the wearer's screen, {s:.0%} of the way to {e['name']}'s strongest, steady"
	if key in ALWAYS_ON:
		return f"{e['name']} on the wearer all the time"
	return potency_text(key, s) + (" (never below 1 HP)" if key == "poison" else "") + ", all the time"

def worn_anchor_text(key):
	e = E[key]
	if e["good"]:
		return e["anchor"]
	if key == "instant_damage":
		return "Its own scale, the Healing gem's mirror: the best build adds 25% to each hit taken"
	if key in DEATH_TRIGGER or key == "infested":
		return f"{e['name']}: the vanilla trigger every time (on/off; the strength is its chance)"
	if key == "levitation":
		return f"Levitation, made a slow drift: rise speed {LEVITATION_RISE} blocks/tick at 1.0 (vanilla Levitation I: 0.05)"
	if key in SCREEN_EFFECTS:
		return f"{e['name']}'s strongest look on the wearer's screen, made partial and steady (owner)"
	if key in ALWAYS_ON:
		return f"{e['name']}: on all the time (on/off, can't be partial; owner)"
	return f"The hit anchor's level ({potency_text(key, 1.0)}), on the wearer all the time"

def worn_value(key, s):
	e = E[key]
	if e["good"]:
		return e["value"](s)
	return heal_fraction(s) if key == "instant_damage" else s

# Loot (owner: gems of every type and gemmed armor are findable loot, in chests and worn by mobs; rare and mostly weak,
# better in harder places). One table: the loot levels, the places, the mob gear. A found gem is made as if brewed and
# enchanted: an Infusion level and a tier drawn from its level's lists (a repeat is a weight), then the normal tier odds.
# Craftable gems from loot never use Infusion VI (nether star), so the perfect craftable gem stays crafted only; a
# found-only gem has no other source, so ominous vaults can give a perfect one.
# level: (description, base gems (repeats weight them), tiers (repeats weight them), potion variant ("normal" = the
#         weakest; "any" = any of the effect's variants), armor materials for gemmed gear, gems per gemmed piece (min, max))
LOOT = {
	"common":  ("Common loot (dungeons, mineshafts, temples, shipwrecks, ruins, fishing treasure, hero gifts, most mobs)",
		["amethyst", "amethyst", "quartz"], [1], "normal", ["leather", "copper", "golden", "chainmail"], (1, 1)),
	"rare":    ("Rare loot (strongholds, buried treasure, outposts, mansions, nether fortresses, piglins)",
		["amethyst", "quartz", "quartz", "ender_pearl"], [1, 1, 2], "normal", ["chainmail", "golden", "iron"], (1, 1)),
	"hard":    ("Hard places (ancient cities, bastions, end cities, trial vaults, elder guardians, trial chamber mobs)",
		["quartz", "ender_pearl", "ender_pearl", "diamond"], [1, 2, 2], "any", ["iron", "diamond"], (1, 2)),
	"ominous": ("Ominous vaults in trial chambers",
		["ender_pearl", "diamond", "diamond", "prismarine", "nether_star"], [2, 2, 3], "any", ["diamond"], (1, 3)),
}
GEAR_SHARE = 0.25  # of a level's rolls, the share that is a gemmed armor piece instead of a gem
FOUND_SHARE = 1 / 3  # where a place has found-only gems (and general ones too): the share of its rolls that is one of them
HERO_GIFTS = ["armorer", "baby", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher", "leatherworker",
	"librarian", "mason", "shepherd", "toolsmith", "unemployed", "weaponsmith"]
FOUND_ONLY_KEYS = ["haste", "luck", "health_boost", "hero_of_the_village", "mining_fatigue", "resistance"]
# Places: (vanilla loot table, loot context type, level, chance per chest / catch / gift / kill, found-only gems there,
#          general gems and gear too). The chance is per opening: one extra roll that adds a gem or a gemmed piece.
LOOT_PLACES = [
	("chests/simple_dungeon", "chest", "common", 0.06, [], True),
	("chests/abandoned_mineshaft", "chest", "common", 0.05, ["haste"], True),
	("chests/desert_pyramid", "chest", "common", 0.05, [], True),
	("chests/jungle_temple", "chest", "common", 0.05, [], True),
	("chests/igloo_chest", "chest", "common", 0.05, [], True),
	("chests/shipwreck_treasure", "chest", "common", 0.05, ["luck"], True),
	("chests/underwater_ruin_big", "chest", "common", 0.05, [], True),
	("chests/ruined_portal", "chest", "common", 0.04, [], True),
	("gameplay/fishing/treasure", "fishing", "common", 0.05, ["luck"], True),
	*[(f"gameplay/hero_of_the_village/{p}_gift", "gift", "common", 0.05, ["hero_of_the_village"], True) for p in HERO_GIFTS],
	("chests/stronghold_corridor", "chest", "rare", 0.06, ["haste"], True),
	("chests/stronghold_crossing", "chest", "rare", 0.06, ["haste"], True),
	("chests/stronghold_library", "chest", "rare", 0.08, ["haste"], True),
	("chests/buried_treasure", "chest", "rare", 0.10, ["luck"], True),
	("chests/pillager_outpost", "chest", "rare", 0.08, ["hero_of_the_village"], True),
	("chests/woodland_mansion", "chest", "rare", 0.10, ["resistance"], True),
	("chests/nether_bridge", "chest", "rare", 0.06, [], True),
	("chests/ancient_city", "chest", "hard", 0.12, ["haste", "health_boost"], True),
	("chests/bastion_treasure", "chest", "hard", 0.15, ["resistance"], True),
	("chests/bastion_other", "chest", "hard", 0.06, [], True),
	("chests/bastion_bridge", "chest", "hard", 0.06, [], True),
	("chests/bastion_hoglin_stable", "chest", "hard", 0.06, [], True),
	("chests/end_city_treasure", "chest", "hard", 0.12, ["health_boost"], True),
	("chests/trial_chambers/reward", "chest", "hard", 0.08, [], True),
	("entities/elder_guardian", "entity", "hard", 0.20, ["mining_fatigue"], False),
	("chests/trial_chambers/reward_ominous", "chest", "ominous", 0.10, FOUND_ONLY_KEYS, True),
]
# Mob gear: each armor piece a mob spawns with gets gems with this chance (vanilla drop chances; the gems stay in the
# piece). (who, entity predicate or None for every other mob, level, chance per piece)
MOB_GEMS = [
	("Piglins", {"minecraft:entity_type": "minecraft:piglin"}, "rare", 0.06),
	("Every other mob that spawns in armor (zombies, husks, drowned, skeletons, strays, bogged...)", None, "common", 0.04),
]
# Mobs from ominous trial spawners wear vanilla's trial chamber equipment table (they never drop it, vanilla).
TRIAL_EQUIPMENT = ("equipment/trial_chamber", "hard", 0.10)

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
HARMFUL_DRAWBACKS = ["slowness", "weakness", "mining_fatigue", "hunger", "unluck", "poison", "wither"]  # poison, wither: owner Q8
# Owner (Q9): a harmful gem's drawback is any beneficial effect that works as a short effect on a target, so real, random
# combinations happen. Only Healing is left out: it is instant and has no mild timed form to land for a few seconds.
BENEFICIAL_DRAWBACKS = [k for k in GOOD_GEMS if E[k].get("kind") != "instant"]
# Drawback roll weights (default 1): poison and wither roll, but half as often as the others (owner, Q8).
DRAWBACK_WEIGHTS = {"poison": 0.5, "wither": 0.5}
NEVER_DRAWBACK = {
	"instant_damage": "instant, no mild form", "instant_health": "instant, no mild form",
	"blindness": "ruins the screen, not just the gem", "darkness": "ruins the screen, not just the gem",
	"nausea": "ruins the screen, not just the gem", "levitation": "dangerous, not mild",
	"glowing": "harmless alone, a beacon in PvP", "wind_charged": "only felt at death", "weaving": "only felt at death",
	"oozing": "only felt at death", "infested": "spawns mobs, not mild",
}
# Main effect only, never an "other" roll.
OTHER_EXCLUDED = {"instant_health": "its own per-hit curve: main effect only", "instant_damage": "a strike on every hit: main effect only",
	"hero_of_the_village": "a permanent villager discount stays a rare found gem: main effect only"}

def other_pool(k):
	return [o for o in GEM_EFFECTS if E[o]["good"] == E[k]["good"] and o != k and o not in OTHER_EXCLUDED]

def drawback_pool(k):
	"""Opposite kind, never its direct opposite, never a part the gem already has (Turtle Master's Slowness)."""
	return [d for d in (HARMFUL_DRAWBACKS if E[k]["good"] else BENEFICIAL_DRAWBACKS)
		if d != k and d not in OPPOSITE.get(k, ()) and d not in COMPOUND.get(k, {})]

EXAMPLES = [("speed", "strength", "hunger"), ("poison", "slowness", "resistance"), ("night_vision", "water_breathing", "mining_fatigue")]

def source_text(k):
	src = E[k]["source"]
	if src[0] == "potion":
		return f"Craftable: {E[k]['potion'].replace('_', ' ')} potion (normal, splash or lingering) + base gem (sets the Infusion level)"
	if src[0] == "item":
		return f"Craftable: {src[1]} in the bottle slot + base gem (sets the Infusion level); normal form only, so tier 1 at most"
	if E[k]["potion"]:
		return f"Found: {src[2]}; plus ominous vaults. Also brewed from its potion (normal, splash or lingering), which has no survival recipe"
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
	"world/entity/projectile/hurtingprojectile/WitherSkull.java": [r"MobEffects\.WITHER, 20 \* witherSeconds, 1\)"],
	"world/entity/projectile/arrow/SpectralArrow.java": [r"int duration = 200", r"MobEffects\.GLOWING"],
	"world/entity/projectile/ShulkerBullet.java": [r"MobEffects\.LEVITATION, 200\)"],
	"world/entity/monster/illager/Illusioner.java": [r"MobEffects\.BLINDNESS, 400\)"],
	"world/entity/monster/warden/Warden.java": [r"MobEffects\.DARKNESS, 260"],
	"world/entity/npc/villager/Villager.java": [r"0\.3F \+ 0\.0625F \* heroOfTheVillage\.getAmplifier\(\)"],
	"world/level/block/Blocks.java": [r"FlowerBlock\(MobEffects\.SATURATION, 0\.35F", r"FlowerBlock\(MobEffects\.BLINDNESS, 11\.0F", r"WitherRoseBlock\(MobEffects\.WITHER",
		r"GOLDEN_DANDELION = register"],
	"world/level/block/EyeblossomBlock.java": [r"OPEN\(true, MobEffects\.BLINDNESS, 11\.0F"],
	"world/level/block/FlowerBlock.java": [r"Mth\.floor\(effectSeconds \* 20\.0F\)"],
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
			if key == "turtle_master":
				pat = (rf'new Potion\("turtle_master", new MobEffectInstance\(MobEffects\.SLOWNESS, {ticks}, {SLOWNESS_AMP[var]}\), '
					rf'new MobEffectInstance\(MobEffects\.RESISTANCE, {ticks}{amp_s}\)')
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
	for t, outs in TIERS.items():
		if outs[0][2] or outs[0][3] != max(o[3] for o in outs):
			errors.append(f"tier {t}: the plain result must come first and be the most common (owner, Q34)")
	if [o[1] for o in TIERS[2]] != ["medium"] * 3:
		errors.append("tier 2 outcomes are all medium (owner)")
	for g in {g for outs in TIERS.values() for _, _, extra, _ in outs for kind, g in extra if kind == "drawback"}:
		if not drawback_size(g) < DRAWBACK_CAP or drawback_sum([drawback_size(g)] * 12) >= DRAWBACK_CAP:
			errors.append(f"a {g} drawback reaches the drawback cap")
	notes.append("every ladder step (Infusion level, armor, weapon material, run, grade, potion variant) is at least +4% on the one below; Infusion VI (nether star) and netherite are the only bests; each tier's odds add up to 100%")

	# 2. Gem quality: 1.0 only for strongest potion + lingering + tier 3 + nether star, for every gem effect.
	for k in GEM_EFFECTS:
		hits = []
		if E[k]["source"][0] == "found":
			for src in E[k]["source"][1]:
				_, bases, tiers = LOOT[src][:3]
				for tier in set(tiers):
					for label, grade, _, _ in TIERS[tier]:
						if grade is None:
							continue
						for b in set(bases):
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
	notes.append("a gem's own strength reaches 1.0 only from the strongest potion or source item + lingering form + tier 3 + Infusion VI (nether star), for every craftable gem; a found-only gem is perfect only from ominous loot at Infusion VI and tier 3")

	# 2b. Every 26.3 status effect is classified: a gem (craftable or found), drawback-only, or not a gem.
	if SRC.exists():
		mob = re.sub(r"\s+", " ", (SRC / "world/effect/MobEffects.java").read_text(encoding="utf-8"))
		ids = set(re.findall(r'Holder<MobEffect> [A-Z_]+ = register\( ?"([a-z_]+)"', mob))
		known = (set(GEM_EFFECTS) - set(COMPOUND)) | set(NOT_GEMS) | set(DRAWBACK_ONLY)
		if ids != known:
			errors.append(f"status effects not classified: {sorted(ids - known)}; classified but not in 26.3: {sorted(known - ids)}")
		n_ids = len(ids)
	else:
		n_ids = len((set(GEM_EFFECTS) - set(COMPOUND)) | set(NOT_GEMS) | set(DRAWBACK_ONLY))
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
	notes.append(f"weakest single armor gem (normal potion, Infusion I, tier 1, lone leather piece) = {worst:.3f}; early armor kit (iron set, 4 Infusion II tier-1 gems) = {min(early):.3f}-{max(early):.3f}; "
		f"weakest weapon gem (wooden sword, Infusion I, tier 1) = {wworst:.3f}; early weapon (iron sword, 1 Infusion II tier-1 gem) = {min(wearly):.3f}-{max(wearly):.3f}")

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
		if not DRAWBACK_CAP < E[k].get("level_one", 1.0):
			errors.append(f"{k}: drawbacks can reach vanilla level I")
	weakest_benefit = min(gem_quality(k, "normal", "amethyst", "strongest") * MATERIALS["leather"][0] * RUN[1] for k in GEM_EFFECTS)
	if not DRAWBACK_ONE < weakest_benefit:
		errors.append(f"a drawback ({DRAWBACK_ONE}) outweighs the weakest tier-3 gem's own share ({weakest_benefit:.3f})")
	for k in NEVER_DRAWBACK:
		if k in HARMFUL_DRAWBACKS + BENEFICIAL_DRAWBACKS:
			errors.append(f"{k} must never be a drawback ({NEVER_DRAWBACK[k]})")
	notes.append(f"drawbacks: one is {DRAWBACK_ONE:.0%} of the strongest vanilla level, any number of one effect stays under {DRAWBACK_CAP:.0%}, below vanilla's level I for every drawback effect; Poison and Wither roll at half weight (owner); Healing, Harming and the rest of the never-drawback list never roll")

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

	# 10. Weapons: per hit, potency and duration both rise; nothing reaches the strongest potion (owner: potions are the 1.0).
	for k in GEM_EFFECTS:
		p_prev = d_prev = -1
		for i in range(1, 101):
			p, d = weapon_hit(k, i / 100 * CAP)
			kind = E[k]["kind"]
			# Regeneration's floor is a heal amount, so its floored duration shortens as the rate rises; the heal never drops.
			grows = p * d >= p_prev * d_prev - 1e-12 if k in HIT_FLOOR_HEAL_HP else d >= d_prev - 1e-12
			if (kind != "onoff" and not p > p_prev) or (kind != "instant" and not grows):
				errors.append(f"{k}: weapon potency not rising or duration falling")
				break
			if kind != "instant" and d < hit_floor(k, p) - 1e-12:
				errors.append(f"{k}: a hit lasts less than its floor")
				break
			p_prev, d_prev = p, d
		p, d = weapon_hit(k, CAP)
		if E[k]["kind"] != "instant" and abs(d - MAX_HIT_SECONDS) > 1e-9 and d > MAX_HIT_SECONDS:
			errors.append(f"{k}: the best weapon's hit lasts {d:.1f} s, past {MAX_HIT_SECONDS} s")
		if E[k]["kind"] == "scaled" and not p < 1.0:
			errors.append(f"{k}: the best weapon gem's potency reaches the strongest potion's level")
	notes.append(f"weapons: per hit, potency rises with strength toward the strongest potion's level (never reaching it); a hit lasts at most {MAX_HIT_SECONDS:.0f} s (the best weapon), less below it, never under its floor (owner); a real tipped arrow and a gem of the same effect don't add, the stronger level applies")
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
		("Infusion V (prismarine shard) instead of VI (nether star)", build_total(["netherite"] * 4, [3] * 4, gem_quality(k, tv, "prismarine", "strongest"))),
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
		("Infusion V (prismarine shard) instead of VI (nether star)", weapon_total("netherite", 2, gem_quality(k, tv, "prismarine", "strongest"))),
		("splash potion (tier 2 at best: medium)", weapon_total("netherite", 2, gem_quality(k, tv, BEST_BASE, "medium"))),
		("diamond weapon", weapon_total("diamond", 2, best_q)),
		("bow, crossbow, trident or mace", weapon_total("bow", 2, best_q)),
		("one gem (the other slot empty or another effect)", weapon_total("netherite", 1, best_q)),
	]
	return rows

BUILDS = [
	("Starter", "1 gem on a lone leather chestplate; normal potion, Infusion I (amethyst), tier 1 weak",
		[None, "leather", None, None], [0, 1, 0, 0], "normal", "amethyst", "weak"),
	("Early", "Full iron set, 1 gem per piece (4); normal potion, Infusion II (quartz), tier 1 weak",
		["iron"] * 4, [1] * 4, "normal", "quartz", "weak"),
	("Mid", "Full diamond set, 2 gems per piece (8); splash of the strong potion, Infusion III (ender pearl), tier 2 medium",
		["diamond"] * 4, [2] * 4, "top", "ender_pearl", "medium"),
	("Late", "Full diamond set, 12 gems; lingering strong potion, Infusion IV (diamond), tier 3",
		["diamond"] * 4, [3] * 4, "top", "diamond", "strongest"),
	("Best", "Full netherite set, 12 gems; lingering strong potion, Infusion VI (nether star), tier 3",
		["netherite"] * 4, [3] * 4, "top", BEST_BASE, "strongest"),
]
WEAPON_BUILDS = [
	("Starter", "Wooden sword, 1 gem; normal potion, Infusion I (amethyst), tier 1 weak", "wood", 1, "normal", "amethyst", "weak"),
	("Early", "Iron sword, 1 gem; normal potion, Infusion II (quartz), tier 1 weak", "iron", 1, "normal", "quartz", "weak"),
	("Mid", "Diamond sword, 2 gems; splash of the strong potion, Infusion III (ender pearl), tier 2 medium", "diamond", 2, "top", "ender_pearl", "medium"),
	("Late", "Diamond sword, 2 gems; lingering strong potion, Infusion IV (diamond), tier 3", "diamond", 2, "top", "diamond", "strongest"),
	("Bow", "Bow, 2 gems; lingering strong potion, Infusion VI (nether star), tier 3 (the best ranged)", "bow", 2, "top", BEST_BASE, "strongest"),
	("Best", "Netherite sword, axe or spear, 2 gems; lingering strong potion, Infusion VI (nether star), tier 3", "netherite", 2, "top", BEST_BASE, "strongest"),
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
	S = []  # ("h1"|"h2"|"h3"|"p"|"ul"|"ol"|"table", ...)
	S.append(("h1", "Bubba's Infused Gems: balance model"))
	S.append(("p", "Generated by `tools/gem-balance.py` from the owner's design in `specs/effect-gems.md`. Do not edit by hand: change the script and rerun it. Vanilla numbers come from the decompiled Minecraft 26.3 source and the script re-checks them against it."))
	S.append(("h2", "The formula in plain words"))
	S.append(("ul", [
		"**A gem doesn't care where it goes** (owner). Every gem, from any potion, item or loot, fits armor (3 slots a piece) or a weapon (2 slots). **The slot decides who its effects reach:** in armor, the wearer; in a weapon, what it hits (bows and crossbows: the arrow). All of a gem's effects go the same way: main effect, any other effect and any drawback. So a Poison gem in your armor poisons you, and a Regeneration gem on a sword heals the target: allowed, just pointless.",
		f"**There is one Infused Gem item** (owner); the base gem it was brewed from is its **Infusion level**: {', '.join(inf(b) for b in BASE)}. Names read like *Infused Gem: Infusion III, Speed (Splash)*.",
		"Every effect has **two curves on one scale**, 1.0 = the strongest vanilla level. **Worn** (armor): the effect on the wearer all the time, 1.0 = the strongest level of the effect (Speed II, Strength II, Resistance IV...), on/off effects made partial. **Hit-applied** (weapon): what one hit gives the target, 1.0 = the strongest vanilla potion of that effect as drunk (owner: potions, not tipped arrows), so a hit is a nerfed potion. Healing has its own, flatter worn curve.",
		"**One gem's strength** = potion factor x Infusion factor x enchant grade x gear factor. In armor the gear factor is armor material x matching run; in a weapon it is the weapon's material. Each is at most 1.0.",
		f"**Armor stacking:** a piece holds {SLOTS} gems, so 12 in all. The gems of one effect are lined up strongest first; the first counts fully, each next one {STACK_R:.0%} of the one before. The sum is scaled so that 12 perfect gems give exactly **{CAP}**.",
		f"**Weapon stacking:** a weapon holds {WEAPON_SLOTS} gems, same rule, scaled so 2 perfect gems give exactly **{CAP}**. One perfect gem gives {CAP / WW:.3f}: two gems of one effect, or one gem each of two effects.",
		f"**The cap is {CAP}**: 5% short of vanilla's best. Only the single best build reaches it: **lingering potion + Infusion VI (nether star) + tier 3 + full contiguous netherite** (armor), or **+ a netherite weapon with both slots** (weapons).",
		"**Potion form doesn't multiply**: it sets the highest enchant tier (normal 1, splash 2, lingering 3), and the tier sets the grade. So lingering is needed for the strongest grade.",
		"**Infusion level changes strength only** (owner): no traits. Order, by how hard the base gem is to get: amethyst, quartz, ender pearl, diamond, prismarine shard, nether star.",
		"**One roll table per gem** (owner): its main effect, maybe an **other** effect of the same kind (a beneficial gem's bonus is another beneficial effect, a harmful gem's another harmful one, never the main effect itself), maybe a **drawback** of the opposite kind that is **never the main effect's direct opposite**. The other effect uses the same formula with its own grade and joins that effect's stack; a drawback is a fixed, mild size (below).",
	]))
	S.append(("p", f"Total for an effect, in symbols: `total = {CAP} x sum(gem_i x {STACK_R}^(rank_i)) / sum({STACK_R}^0..{STACK_R}^(slots-1))`, with `gem = potion x infusion x grade x gear`."))

	S.append(("h2", "Factor tables"))
	S.append(("h3", "Infusion level (the base gem; strength only)"))
	S.append(("table", ["Infusion", "Base gem", "Factor", "Why here"], [[inf(b).split(" (")[0], n, fmt(f), w] for b, n, f, w in BASE_GEMS]))
	S.append(("h3", "Potion variant (by what that effect really has in 26.3)"))
	S.append(("table", ["Variants that exist", "normal", "long", "strong"], [
		["normal + long + strong (Swiftness, Strength, Leaping, Regeneration, Poison, Slowness, Turtle Master)", "0.80", "0.85", "1.00"],
		["normal + long (Fire Resistance, Night Vision, Invisibility, Water Breathing, Slow Falling, Weakness)", "0.90", "1.00", "-"],
		["normal + strong (Healing, Harming)", "0.80", "-", "1.00"],
		["one only (Luck, Wind Charged, Weaving, Oozing, Infested), and every item-sourced or found-only gem but Absorption", "1.00", "-", "-"],
		["Absorption: golden apple (normal) or enchanted golden apple (strong)", "0.80", "-", "1.00"],
	]))
	S.append(("h3", "Potion form -> highest enchant tier (owner)"))
	S.append(("table", ["Form", "Highest tier", "Best grade reachable"], [
		["Normal", "1", "weak"], ["Splash", "2", "medium"], ["Lingering", "3", "strongest"]]))
	S.append(("p", "The form is the potion's form, fixed at brewing; nothing raises it later (owner: potion -> gem, no other pathway). An item has no splash or lingering form, so an Infused Gem brewed from an item is normal form and reaches tier 1 at most; that gap is accepted."))
	S.append(("h3", "Enchant tier outcomes"))
	S.append(("table", ["Tier", "Outcome", "Main effect", "Extra effect", "Proposed odds"],
		[[str(t), lab, (f"{g} ({GRADES[g]:.2f})" if g else "-"),
			", ".join(f"{kind} {gr}" + (f" ({GRADES[gr]:.2f})" if kind == "other" else f" ({DRAWBACK_ONE:.2f}, fixed)") for kind, gr in ex) or "-", f"{p:.0%}"]
			for t, outs in TIERS.items() for lab, g, ex, p in outs]))
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
	S.append(("p", "Every one of the 40 status effects in 26.3 (the script reads them from the source and fails if one is missing). Any gem goes into armor or a weapon. An effect with a potion is brewed from it. One without a potion is brewed from a vanilla item that already carries it: in 26.3 the brewing stand's bottle slot takes any item that a brewing recipe names as its input (BrewingStandMenu.PotionSlot asks the recipes, PotionIngredient.isPotionInput), so this is data, no menu change. Stews are told apart by their stew effect with Fabric API's components ingredient. An effect with no sensible item is a **found-only** gem: loot and mob gear, never brewed."))
	S.append(("table", ["Effect", "Kind", "How it's had"],
		[[E[k]["name"], "beneficial" if E[k]["good"] else "harmful", source_text(k)] for k in GEM_EFFECTS]
		+ [[E[k]["name"], "harmful", "Drawback only: " + why] for k, why in DRAWBACK_ONLY.items()]
		+ [[k.replace("_", " ").title(), "-", "Not a gem: " + why] for k, why in NOT_GEMS.items()]))
	S.append(("h3", "Found-only gems: what they come with"))
	S.append(("p", "A found gem is made as if brewed and enchanted: its potion factor is 1.0 (one form of the effect), its Infusion level is picked from the loot source's list and its tier rolled from the source's tiers with the normal tier odds. Same strength scale as crafted gems; only ominous loot can give a perfect one."))
	S.append(("table", ["Loot source", "Infusion levels", "Tiers", "Strength range (gem quality)"],
		[[name, ", ".join(inf(b) for b in bases), ", ".join(map(str, tiers)),
			f"{min(BASE[b] for b in bases) * min(GRADES[g] for t in tiers for _, g, _, _ in TIERS[t] if g):.2f} - {max(BASE[b] for b in bases) * max(GRADES[g] for t in tiers for _, g, _, _ in TIERS[t] if g):.2f}"
			+ (" (or a drawback-only roll at tier 1)" if 1 in tiers else "")]
			for name, bases, tiers, *_ in LOOT.values()]))
	S.append(("h2", "Loot: where gems are found (one table, `LOOT_PLACES`)"))
	S.append(("p", "Every place below gets **one extra roll** with the given chance per chest (fishing: per treasure catch; hero gift: per gift; elder guardian: per kill by a player). The roll adds a finished gem of any craftable effect, or (a quarter of the time) a gemmed armor piece of the level's materials, or, where found-only gems are listed, one of them (a third of the time). Vanilla's tables are never replaced; the extra roll is a data file of the mod (`loot_table/inject/...`), so a datapack can change or empty any of it. Rare and mostly weak, better in harder places (owner). Craftable gems from loot never reach Infusion VI, so a perfect craftable gem is always crafted; found-only gems can be perfect only from ominous vaults."))
	S.append(("table", ["Level", "Infusion (weighted)", "Tiers (weighted)", "Potion variant", "Gemmed gear", "Gems per piece", "Gem quality (craftable)"],
		[[level, ", ".join(INFUSION[b] for b in bases), ", ".join(map(str, tiers)), "weakest" if variant == "normal" else "any",
			", ".join(mats), f"{lo}" if lo == hi else f"{lo}-{hi}", f"{loot_quality(level, True)[0]:.2f} - {loot_quality(level, True)[1]:.2f}"]
			for level, (_, bases, tiers, variant, mats, (lo, hi)) in LOOT.items()]))
	places = {}
	for t, ctx, level, chance, fnd, general in LOOT_PLACES:
		label = "Hero of the Village gifts (every profession)" if t.startswith("gameplay/hero_of_the_village/") else t
		places.setdefault(label, (level, chance, fnd, general))
	S.append(("table", ["Where", "Level", "Chance", "Found-only gems there"],
		[[w, lv, f"{c:.0%}", ", ".join(E[k]["name"] for k in f) + ("" if g else " (only)") if f else "-"] for w, (lv, c, f, g) in places.items()]))
	S.append(("h3", "Gems on mobs"))
	S.append(("p", "Each armor piece a mob spawns with gets gems with the chance below; the mob gets the gems' effects like a player (undead follow the undead rules). The piece drops with vanilla's equipment drop chance and keeps its gems. Mobs from ominous trial spawners wear vanilla's trial equipment table, which gets gems too; they never drop their gear (vanilla)."))
	S.append(("table", ["Who", "Level", "Chance per armor piece", "Gems"],
		[[who, level, f"{chance:.0%}", str(gem_count(level))] for who, _, level, chance in MOB_GEMS]
		+ [["Ominous trial spawner mobs", TRIAL_EQUIPMENT[1], f"{TRIAL_EQUIPMENT[2]:.0%}", "1-2" if gem_count(TRIAL_EQUIPMENT[1]) != 1 else "1"]]))

	S.append(("h2", "Roll tables"))
	S.append(("p", "One roll table per gem, whatever slot it later goes into. **Other** (tier 2 weak, tier 3 weak or medium): any gem effect of the main effect's kind except itself and the main-effect-only ones ("
		+ "; ".join(f"{E[k]['name']}: {why}" for k, why in OTHER_EXCLUDED.items()) + "). **Drawback** (tier 1 alone, tier 3 with the strongest main effect): one of the opposite kind, never the main effect's direct opposite."))
	S.append(("h3", "Direct opposites (never a drawback of each other)"))
	S.append(("table", ["Beneficial", "Harmful", "Why they cancel out"], [[E[a]["name"], E[b]["name"], why] for a, b, why in INVERSE_PAIRS]))
	S.append(("p", "Checked and left out (no direct opposite in vanilla): " + "; ".join(f"**{E[k]['name']}**: {why}" for k, why in NO_OPPOSITE.items())
		+ ". Harmful effects with no beneficial counterpart: Nausea, Wind Charged, Weaving, Oozing, Infested."))
	S.append(("h3", "Drawback candidates"))
	S.append(("p", "A beneficial gem's drawback rolls from: " + ", ".join(E[k]["name"] for k in HARMFUL_DRAWBACKS)
		+ ". A harmful gem's drawback rolls from: " + ", ".join(E[k]["name"] for k in BENEFICIAL_DRAWBACKS)
		+ ". Minus the main effect's direct opposite, which leaves every gem at least "
		+ f"{min(len(drawback_pool(k)) for k in GEM_EFFECTS)} candidates. Never a drawback: "
		+ "; ".join(f"{E[k]['name']} ({why})" for k, why in NEVER_DRAWBACK.items()) + "."))
	S.append(("table", ["Gem", "Kind", "Other rolls from", "Drawback rolls from", "Excluded as its direct opposite"],
		[[E[k]["name"], "beneficial" if E[k]["good"] else "harmful",
			f"any other {'beneficial' if E[k]['good'] else 'harmful'} gem effect ({len(other_pool(k))})",
			", ".join(E[d]["name"] for d in drawback_pool(k)),
			", ".join(E[o]["name"] for o in sorted(OPPOSITE.get(k, ()))) or "-"] for k in GEM_EFFECTS]))
	S.append(("h3", "Three example gems, armor vs weapon"))
	S.append(("p", f"Strengths used: the main effect at the best build ({CAP}: 12 perfect gems in full netherite, or 2 in a netherite weapon) and as one perfect gem alone (armor {stack([1.0]):.3f} in a full netherite set; weapon {wstack([1.0]):.3f}); an other effect as the only gem of that effect, Infusion VI in netherite, weak ({stack([GRADES['weak']]):.3f} armor, {wstack([GRADES['weak']]):.3f} weapon) or medium ({stack([GRADES['medium']]):.3f}, {wstack([GRADES['medium']]):.3f}); one drawback ({DRAWBACK_ONE:.2f})."))
	for k, o, d in EXAMPLES:
		S.append(("p", f"**{E[k]['name']}** ({'beneficial' if E[k]['good'] else 'harmful'}). Other rolls from {len(other_pool(k))} {'beneficial' if E[k]['good'] else 'harmful'} effects; drawback rolls from "
			+ ", ".join(E[x]["name"] for x in drawback_pool(k)) + "; never " + (", ".join(E[x]["name"] for x in sorted(OPPOSITE.get(k, ()))) or "-") + "."))
		S.append(("table", ["Part", "In armor (the wearer, while worn)", "In a weapon (the target, per hit or arrow)"], [
			["Main, best build", worn_show(k, CAP), hit_show(k, CAP)],
			["Main, one perfect gem", worn_show(k, stack([1.0])), hit_show(k, wstack([1.0]))],
			[f"Weak other: e.g. {E[o]['name']}", worn_show(o, stack([GRADES['weak']])), hit_show(o, wstack([GRADES['weak']]))],
			[f"Medium other: e.g. {E[o]['name']}", worn_show(o, stack([GRADES['medium']])), hit_show(o, wstack([GRADES['medium']]))],
			[f"Drawback: e.g. {E[d]['name']}", potency_text(d, DRAWBACK_ONE) + ", all the time", f"{potency_text(d, DRAWBACK_ONE)} for {DRAWBACK_HIT_S} s, refreshed by each hit"],
		]))

	S.append(("h2", "In armor: what each gem does to the wearer"))
	cols = [0.05, 0.10, 0.25, 0.50, 0.75, CAP]
	S.append(("p", "Beneficial gems are what armor is for. Harmful gems in armor hit the wearer (pointless, but allowed): damage-over-time and attribute effects at the same level as their hit anchor, Glowing on all the time, Darkness, Blindness and Nausea as a steady, partial dimming, fog or wobble on the wearer's screen, Levitation a slow drift up by strength (owner), death and hurt triggers as a chance. Harming in armor mirrors the Healing gem: each survivable hit taken deals a share more."))
	S.append(("table", ["Effect", "1.0 means (worn)"] + [f"s = {c:.2f}" for c in cols],
		[[E[k]["name"], worn_anchor_text(k)] + [worn_show(k, c) for c in cols] for k in GEM_EFFECTS if k != "instant_health"]))

	S.append(("h2", "Healing gem (in armor)"))
	S.append(("p", "When the wearer takes damage from an attack or a fall and survives it, they instantly get back a fraction of the health that hit took. A killing blow still kills. Its own curve: `heal = " + f"{HEAL_BEST} x (total / {CAP})^{HEAL_EXP}" + "`, so the best build returns exactly 25% and early gems are flattened upward to stay noticeable. On a weapon it heals the target a little on every hit (below)."))
	S.append(("p", "**Why 25% at the very best:** it works like 25% less damage from survivable hits, between Resistance I (20%) and Resistance II (40%), and far below the Resistance gem's own best (76%) or full Protection IV armor (64%). It never stops a one-shot, never heals outside a fight, and is always a net loss (you can't farm it with fall damage). A Healing II potion gives 8 HP at once; at 25% the wearer must take 32 HP of hits, about one and a half health bars, to get that much back. So it is a comfort in long fights, not a second health bar."))
	S.append(("table", ["Build", "Gem total", "Share of each hit returned", "A 6 HP zombie hit", "A 20 HP fall (survived)"],
		[[b[0], fmt(build_value("instant_health", b), 3), f"{heal_fraction(build_value('instant_health', b)) * 100:.1f}%",
			f"+{6 * heal_fraction(build_value('instant_health', b)):.2f} HP", f"+{20 * heal_fraction(build_value('instant_health', b)):.1f} HP"] for b in BUILDS]))

	S.append(("h2", "In a weapon: what one hit gives the target"))
	S.append(("p", f"A weapon holds {WEAPON_SLOTS} gems. Swords, axes, maces, tridents and spears apply the gem's effects to what they hit; bows and crossbows turn each fired arrow into its tipped form carrying them (picked up again, it's a plain arrow). Each hit **refreshes** the effect, it never stacks. A real tipped arrow fired from a gemmed bow: the stronger effect applies (higher strength wins, then longer duration), never both. Harmful gems are what weapons are for; a beneficial gem on a weapon buffs the target (pointless, but allowed)."))
	S.append(("p", f"**Per hit (owner):** potency is measured against the strongest potion level: potency = s^{WEAPON_SPLIT} of the 1.0 level (it approaches Poison II, Slowness IV... at the cap, never reaching it). Duration is short: at most {MAX_HIT_SECONDS:.0f} s from the best weapon build, x (s / cap)^{1 - WEAPON_SPLIT} below it, and never under the effect's floor, so every hit does something a player notices: " + ", ".join(f"{E[k]['name']} {v:g} s" for k, v in HIT_FLOOR_SECONDS.items()) + f", Regeneration long enough to heal {HIT_FLOOR_HEAL_HP['regeneration']:g} HP, everything else {HIT_FLOOR_SECONDS_DEFAULT:g} s. Each hit refreshes, it never adds. On/off harmful effects (Wind Charged, Weaving, Oozing, Infested, Glowing, Levitation, Blindness, Nausea, Darkness) are the vanilla effect, their strength all in duration. Harming and Healing strike on every hit (owner): s x {HIT_STRIKE_HARM:g} magic damage or s x {HIT_STRIKE_HEAL:g} HP healed on the target, at least {HIT_STRIKE_FLOOR:g}, a modest bonus below Sharpness V's +3."))
	S.append(("table", ["Effect", "1.0 means (one hit)"] + [f"s = {c:.2f}" for c in cols],
		[[E[k]["name"], hit_anchor_text(k)] + [hit_show(k, c) for c in cols] for k in BAD_GEMS + GOOD_GEMS]))

	S.append(("h2", "Drawbacks"))
	S.append(("p", f"A drawback roll is a fixed size: **{DRAWBACK_ONE:.0%} of the strongest vanilla level** of that effect for a weak one, {drawback_size('medium'):.0%} for tier 3's medium one, whatever the potion, Infusion level or gear (a better gem shouldn't hurt more). More drawbacks of the same effect add less and less and never pass {DRAWBACK_CAP:.0%}, which is below vanilla's weakest level of every drawback effect. So they sting, but never as much as a real potion of it. It goes where the gem's main effect goes: in armor, on the wearer while worn; in a weapon, on the target for {DRAWBACK_HIT_S} s, refreshed by each hit (a beneficial gem's harmful drawback on a weapon therefore lands on the target too)."))
	dcols = [1, 2, 3, 12]
	S.append(("table", ["Drawback", "Rolls on", "1.0 means (vanilla)"] + [f"{n} drawback{'s' if n > 1 else ''} ({drawback_total(n):.3f})" for n in dcols],
		[[E[k]["name"], "beneficial gems" if not E[k]["good"] else "harmful gems", DRAWBACK_ANCHOR.get(k, E[k]["anchor"])]
			+ [potency_text(k, drawback_total(n)) for n in dcols] for k in HARMFUL_DRAWBACKS + BENEFICIAL_DRAWBACKS]))

	S.append(("h3", "How each effect is made partial"))
	S.append(("table", ["Effect", "Implementation", "Source (26.3)"], [[E[k]["name"], E[k]["impl"], E[k]["src"]] for k in E]))
	S.append(("p", "Worn (armor) effects are attribute modifiers, data components or small tick and damage hooks, not potion effect instances: vanilla levels are whole numbers and their icons and timers would claim a full level. Hit-applied (weapon) effects, and weapon drawbacks on the target, are gem mob effects whose amplifier counts strength in 1% steps, with vanilla's icon and timer; a gem arrow carries one as its potion contents with a duration scale of 1. A gem and a potion of the same effect don't add up: the stronger applies (owner)."))

	S.append(("h2", "Progression ladders"))
	S.append(("h3", "Armor"))
	S.append(("table", ["Build", "What it is"] + [E[k]["name"] for k in LADDER_EFFECTS],
		[[b[0], b[1]] + [f"{build_value(k, b):.3f}: {worn_show(k, build_value(k, b))}" for k in LADDER_EFFECTS] for b in BUILDS]))
	S.append(("h3", "One step down from the best armor (Speed)"))
	S.append(("table", ["Change from the best build", "Total", "In game"],
		[["(best)", fmt(CAP, 3), worn_show("speed", CAP)]] + [[lab, fmt(t, 3), worn_show("speed", t)] for lab, t in downgrade_rows("speed")]))
	S.append(("h3", "Weapons"))
	S.append(("table", ["Build", "What it is"] + [E[k]["name"] for k in WEAPON_LADDER_EFFECTS],
		[[b[0], b[1]] + [f"{weapon_build_value(k, b):.3f}: {hit_show(k, weapon_build_value(k, b))}" for k in WEAPON_LADDER_EFFECTS] for b in WEAPON_BUILDS]))
	S.append(("h3", "One step down from the best weapon (Poison)"))
	S.append(("table", ["Change from the best build", "Total", "In game"],
		[["(best)", fmt(CAP, 3), hit_show("poison", CAP)]] + [[lab, fmt(t, 3), hit_show("poison", t)] for lab, t in weapon_downgrade_rows("poison")]))

	S.append(("h2", "Checks (all passed)"))
	S.append(("ul", notes))

	S.append(("h2", "Open questions for the owner"))
	S.append(("ol", [
		f"Answered (owner, 2026-11-14): the cap is {CAP}, 10% short of the strongest vanilla level.",
		"Answered (owner, 2026-11-14): gold armor and gold weapons get the magic bonus, diamond-level 0.93 (gold's high enchantability); netherite stays the only material that reaches the cap.",
		"Answered (owner, 2026-11-14): the turtle helmet takes no gems for now.",
		f"Answered (owner, 2026-11-14): Resistance's 1.0 is Resistance II, the beacon level (40%, gems up to {40 * CAP:.0f}%); no potion makes a plain Resistance gem.",
		"Answered (owner, 2026-11-14): a combined Turtle Master gem, brewed from Turtle Master potions, gives its Resistance and its full Slowness together, measured against the strong potion (Resistance IV + Slowness VI).",
		f"Healing gem in armor returns at most {HEAL_BEST:.0%} of each survivable hit (early iron kit about {heal_fraction(build_value('instant_health', BUILDS[1])) * 100:.0f}%)? yes / no.",
		f"Answered (owner, 2026-11-14): drawbacks stay {DRAWBACK_ONE:.0%} for one, under {DRAWBACK_CAP:.0%} for many, for now; tuned by playtesting.",
		"Answered (owner, 2026-11-14): Poison and Wither do roll as a beneficial gem's drawback, at half the weight of the others (gem Poison still never kills).",
		"Answered (owner, 2026-11-14): a harmful gem's drawback rolls from every beneficial effect that works as a short effect on a target (all but Healing), minus the direct opposite: random, real combinations.",
		"Answered (owner, 2026-11-14): Conduit Power and Mining Fatigue are direct opposites.",
		"Answered (owner, 2026-11-14): Jump Boost, Resistance, Health Boost, Absorption, Fire Resistance, Water Breathing and Dolphin's Grace have no direct opposite.",
		"Answered (owner, 2026-11-14): Healing, Harming and Hero of the Village are main-effect only.",
		"Answered (owner, 2026-11-14): Harming in armor mirrors the Healing gem: each survivable hit taken deals up to 25% more.",
		f"Answered (owner, 2026-11-14): harmful on/off gems in armor are always on; Levitation is a slow drift up by strength (at most {LEVITATION_RISE} blocks/tick, vanilla Levitation I 0.05); Blindness, Darkness, Nausea and Glowing can't be partial, so on is on.",
		f"Answered (owner, 2026-11-14): beneficial gems with no potion (Haste, Health Boost, Saturation, Dolphin's Grace, Conduit Power, Hero of the Village) on a weapon follow the same rule: potency against the level a potion, beacon or source gives, at most {MAX_HIT_SECONDS:.0f} s, never under the floor.",
		"Answered (owner, 2026-11-14): weapon material is a strength factor, like armor.",
		"Answered (owner, 2026-11-14): bows, crossbows, tridents and maces stay diamond-level 0.93 for now (per-weapon tuning is backlogged).",
		"Answered (owner, 2026-11-14): superseded by the hit rule: potency against the strongest potion level, a short duration with floors.",
		f"Answered (owner, 2026-11-14): Healing and Harming on a weapon apply on every hit, no charge: up to {HIT_STRIKE_HARM:g} magic damage or {HIT_STRIKE_HEAL:g} HP per hit at 1.0, at least {HIT_STRIKE_FLOOR:g}.",
		f"Answered (owner, 2026-11-14): a hit's potency is measured against the strongest vanilla potion level (Poison II, Slowness IV), but it lasts at most {MAX_HIT_SECONDS:.0f} s (the best weapon), less with less strength, never under the effect's floor (every hit must do something noticeable).",
		"Answered (owner, 2026-11-14): Wither's 1.0 is the Wither boss's skull, Wither II (1 HP/s), for now.",
		"Answered (owner, 2026-11-14): as modeled (item-sources table): Wither gems from the wither rose.",
		"Answered (owner, 2026-11-14): as modeled (item-sources table): Saturation gems from suspicious stew of dandelion, golden dandelion or blue orchid.",
		"Answered (owner, 2026-11-14): as modeled (item-sources table): Blindness gems from suspicious stew of azure bluet or open eyeblossom, Nausea gems from pufferfish.",
		"Answered (owner, 2026-11-14): as modeled (item-sources table): Darkness gems from echo shards.",
		"Answered (owner, 2026-11-14): as modeled (item-sources table): Conduit Power gems from a heart of the sea, Dolphin's Grace gems from a nautilus shell.",
		"Answered (owner, 2026-11-14): as modeled (item-sources table): Haste, Health Boost, Luck, Hero of the Village (and now Resistance) are found-only.",
		f"Answered (owner, 2026-11-14): Hero of the Village gems stay, toned down: at most {HERO_BEST_DISCOUNT:.0%} cheaper trades at the best build.",
		"Answered (owner, 2026-11-14): Mining Fatigue gems from elder guardians (a rare extra drop) and ominous vaults, as modeled (loot is later work).",
		"Answered (owner, 2026-11-14): Bad Omen, Trial Omen, Raid Omen and Breath of the Nautilus are not gems.",
		"Answered (owner, 2026-11-14): a gem and a potion of the same effect don't add, the stronger applies, implemented with care for the edge cases (spec).",
		"Answered (owner, 2026-11-14): undead follow vanilla's inversion: worn Healing makes a survivable hit hurt an undead wearer up to 25% more and worn Harming gives it up to 25% back; on weapons Harming heals undead and Healing hurts them; Poison and Regeneration do nothing to undead.",
		f"Answered (owner, 2026-11-14): Invisibility gems give stealth that scales with strength, hidden on the locator bar from {INVISIBILITY_LOCATOR_HIDE_AT}; only the perfect complete set gives real invisibility (armor still renders, as in vanilla).",
		"Answered (owner, 2026-11-14): the plain result is the most common at every tier (tier 1: weak 75%, + weak drawback 20%, drawback only 5%; tier 2: medium 55%, + weak other 30%, + weak drawback 15%; tier 3: strongest 50%, + weak other 25%, + medium other 10%, + weak drawback 10%, + medium drawback 5%); the table's hover preview shows the main effect and the kind and size of any extra roll.",
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

# ---------------------------------------------------------------- export for the mod (one source of numbers)
# The mod (mods/bubbas_infused_gems) reads BALANCE_JSON at startup; its brewing recipes are generated here too.
# Run `python tools/gem-balance.py` (or `--export`) after any change; `--check` fails when the committed files are stale.
MOD = ROOT / "mods" / "bubbas_infused_gems" / "src" / "main" / "resources"
BALANCE_JSON = MOD / "bubbas_infused_gems" / "balance.json"
RECIPE_DIR = MOD / "data" / "bubbas_infused_gems" / "recipe" / "infuse"
MOD_ID = "bubbas_infused_gems"
BASE_ITEMS = {"amethyst": "minecraft:amethyst_shard", "quartz": "minecraft:quartz", "ender_pearl": "minecraft:ender_pearl",
	"diamond": "minecraft:diamond", "prismarine": "minecraft:prismarine_shard", "nether_star": "minecraft:nether_star"}
# Items that brew an item-sourced Infused Gem (bottle slot), by effect and potion variant: (file name, ingredient). The
# stews are matched by their stew effect with Fabric API's components ingredient; every vanilla flower of one effect
# gives the same component (dandelion, golden dandelion and blue orchid: Saturation 7 ticks; azure bluet and open
# eyeblossom: Blindness 220 ticks; FlowerBlock / EyeblossomBlock, effect seconds x 20), so one recipe covers them all.
def stew(effect, ticks):
	return {"fabric:type": "fabric:components", "base": "minecraft:suspicious_stew",
		"components": {"minecraft:suspicious_stew_effects": [{"id": f"minecraft:{effect}", "duration": ticks}]}}
STEW_TICKS = {"saturation": 7, "blindness": 220}  # verified against Blocks.java in verify_source
ITEM_SOURCES = {
	"absorption": {"normal": ("golden_apple", "minecraft:golden_apple"), "strong": ("enchanted_golden_apple", "minecraft:enchanted_golden_apple")},
	"dolphins_grace": {"normal": ("nautilus_shell", "minecraft:nautilus_shell")}, "conduit_power": {"normal": ("heart_of_the_sea", "minecraft:heart_of_the_sea")},
	"wither": {"normal": ("wither_rose", "minecraft:wither_rose")}, "hunger": {"normal": ("rotten_flesh", "minecraft:rotten_flesh")},
	"glowing": {"normal": ("glowstone_dust", "minecraft:glowstone_dust")}, "levitation": {"normal": ("shulker_shell", "minecraft:shulker_shell")},
	"nausea": {"normal": ("pufferfish", "minecraft:pufferfish")}, "darkness": {"normal": ("echo_shard", "minecraft:echo_shard")},
	"saturation": {"normal": ("saturation_stew", stew("saturation", STEW_TICKS["saturation"]))},
	"blindness": {"normal": ("blindness_stew", stew("blindness", STEW_TICKS["blindness"]))},
}
def source_item_id(ingredient):
	return ingredient if isinstance(ingredient, str) else ingredient["base"]
FORM_ITEMS = {"normal": "minecraft:potion", "splash": "minecraft:splash_potion", "lingering": "minecraft:lingering_potion"}

# How the mod realises each effect worn (armor, on the wearer) at strength s. "attributes": vanilla attribute modifiers,
# amount x s (curve "pow": base^s - 1; "odds": s / (1 - s)). The other keys are the numbers the mod's hooks use. These
# are the same numbers as the effect table's descriptions above; check_export() holds them equal.
WORN = {
	"speed": {"attributes": [("minecraft:movement_speed", 0.4, "add_multiplied_total")]},
	"strength": {"attributes": [("minecraft:attack_damage", 6.0, "add_value")]},
	"jump_boost": {"attributes": [("minecraft:jump_strength", 0.2, "add_value"), ("minecraft:safe_fall_distance", 2.0, "add_value")]},
	"haste": {"attributes": [("minecraft:block_break_speed", 0.4, "add_multiplied_total"), ("minecraft:attack_speed", 0.2, "add_multiplied_total")]},
	"luck": {"attributes": [("minecraft:luck", 1.0, "add_value")]},
	"unluck": {"attributes": [("minecraft:luck", -1.0, "add_value")]},
	"slowness": {"attributes": [("minecraft:movement_speed", -0.6, "add_multiplied_total")]},
	"weakness": {"attributes": [("minecraft:attack_damage", -4.0, "add_value")]},
	"mining_fatigue": {"attributes": [("minecraft:block_break_speed", 0.027, "add_multiplied_total", "pow"), ("minecraft:attack_speed", -0.3, "add_multiplied_total")]},
	"fire_resistance": {"attributes": [("minecraft:burning_time", -1.0, "add_multiplied_total")], "fire_damage_reduction": 1.0},
	"water_breathing": {"attributes": [("minecraft:oxygen_bonus", 1.0, "add_value", "odds")]},
	"slow_falling": {"attributes": [("minecraft:fall_damage_multiplier", -1.0, "add_multiplied_total")], "gravity_reduction": 0.07},
	"absorption": {"attributes": [("minecraft:max_absorption", 16.0, "add_value")], "refill_ticks": 600},
	"health_boost": {"attributes": [("minecraft:max_health", 8.0, "add_value")]},
	"regeneration": {"heal_per_second": 0.8},
	"resistance": {"damage_reduction": 0.4},
	"turtle_master": {"attributes": [("minecraft:movement_speed", -0.9, "add_multiplied_total")], "damage_reduction": 0.8},
	"night_vision": {"intensity": 1.0},
	"instant_health": {"heal_curve": True},
	"instant_damage": {"harm_curve": True},
	"poison": {"damage_per_second": 25 / 12 * 0.8, "min_health": 1.0},
	"wither": {"damage_per_second": 1.0},
	"hunger": {"exhaustion_per_tick": 0.015},
	"invisibility": {"attributes": [("minecraft:waypoint_transmit_range", -1.0, "add_multiplied_total", f"step:{INVISIBILITY_LOCATOR_HIDE_AT}")],
		"visibility_reduction": 0.93, "full_at": INVISIBILITY_FULL_AT},
	"dolphins_grace": {"drag_target": 0.96},
	"saturation": {"exhaustion_reduction": 1.0},
	"conduit_power": {"attributes": [("minecraft:oxygen_bonus", 1.0, "add_value", "odds"), ("minecraft:submerged_mining_speed", 0.8, "add_value")],
		"wet_only": True, "underwater_sight": 1.0},
	"hero_of_the_village": {"price_cut": HERO_CUT},
	**{k: {"always_on": True} for k in sorted(ALWAYS_ON)},
	**{k: {"screen": True} for k in sorted(SCREEN_EFFECTS)},
	"levitation": {"rise_per_tick": LEVITATION_RISE},
	**{k: {"death_chance": 1.0} for k in ("wind_charged", "weaving", "oozing")},
	"infested": {"hurt_chance": 1.0},
}
# Amplifier of each effect's 1.0 level (what a vanilla instance is compared against: "the stronger applies"). Effects
# with a potion take their strongest variant's; the rest are listed.
WORN_AMPLIFIER = {"haste": 1, "absorption": 3, "health_boost": 1, "hero_of_the_village": 4, "mining_fatigue": 2, "hunger": 2}

# ------------------------------------------- open-question defaults the mod relies on (change here; the mod reads them)
RULES = {
	"gem_vs_potion": "stronger",   # Q31: a gem and a vanilla effect of the same effect don't add; the stronger applies ("add" = v0 behaviour)
	"undead_inversion": True,      # Q32: Healing/Harming gems swap on undead; undead ignore poison and regeneration gems
	"invisibility_stealth": True,  # Q33: Invisibility gems are stealth only, never a see-through body
}
# What one hit applies at potency 1.0, for effects whose hit isn't an attribute set from WORN: damage or healing per
# second (a gem mob effect), a per-hit strike's damage or heal.
HIT_UNIT = {"poison": 25 / 12 * 0.8, "wither": 1.0, "regeneration": 0.8, "resistance": 0.4, "turtle_master": 0.8, "hunger": HIT_HUNGER_RATE,
	"instant_damage": HIT_STRIKE_HARM, "instant_health": HIT_STRIKE_HEAL}
def effect_id(k):
	"""The status effect id a gem names: vanilla's, or the mod's own for a combined gem."""
	return f"{MOD_ID}:{k}" if k in COMPOUND else f"minecraft:{k}"

def worn_amplifier(k):
	e = E[k]
	if e["potion"]:
		return max(amp for _, amp in e["variants"].values())
	return WORN_AMPLIFIER.get(k, 0)

def potion_id(e, variant):
	return "minecraft:" + ("" if variant == "normal" else variant + "_") + e["potion"]

def export_data():
	effects = {}
	for k, e in E.items():
		src = e["source"]
		d = {"name": e["name"], "good": e["good"], "role": e["role"], "hit_kind": e.get("kind", "scaled"),
			"level_one": e.get("level_one", 1.0)}
		if k in GEM_EFFECTS:
			vf = variant_factors(e["variants"])
			d["variants"] = {}
			for v, f in vf.items():
				vd = {"factor": f}
				if e["potion"]:
					vd["potion"] = potion_id(e, v)
				elif k in ITEM_SOURCES:
					vd["item"] = source_item_id(ITEM_SOURCES[k][v][1])
				d["variants"][v] = vd
			d["source"] = src[0]
			if src[0] == "found":
				d["loot"] = src[1]
			ticks, amp = arrow_anchor(k)
			d["hit"] = {"ticks": round(MAX_HIT_SECONDS * T), "amplifier": amp,
				"floor_ticks": round(HIT_FLOOR_SECONDS.get(k, HIT_FLOOR_SECONDS_DEFAULT) * T), "floor_heal_hp": HIT_FLOOR_HEAL_HP.get(k, 0.0),
				"floor_exhaustion": HIT_FLOOR_EXHAUSTION.get(k, 0.0)}
			d["other_pool"] = [] if k in OTHER_EXCLUDED else other_pool(k)
			d["drawback_pool"] = drawback_pool(k)
			d["drawback_weights"] = [DRAWBACK_WEIGHTS.get(x, 1.0) for x in drawback_pool(k)]
		if k in WORN:
			w = dict(WORN[k])
			if "attributes" in w:
				w["attributes"] = [{"attribute": a[0], "amount": a[1], "operation": a[2], "curve": a[3] if len(a) > 3 else "linear"}
					for a in w["attributes"]]
			d["worn"] = w
		if k in HIT_UNIT:
			d["hit_unit"] = HIT_UNIT[k]
		d["worn_amplifier"] = worn_amplifier(k)
		d["id"] = effect_id(k)
		if k in COMPOUND:
			d["parts"] = COMPOUND[k]
		d["opposites"] = sorted(OPPOSITE.get(k, ()))
		d["never_drawback"] = k in NEVER_DRAWBACK
		effects[k] = d
	armor_builds = [{"name": b[0], "layout": b[2], "gems": b[3], "effect": "speed",
		"variant": top_variant("speed") if b[4] == "top" else b[4], "base": b[5], "grade": b[6], "total": round(build_value("speed", b), 6)}
		for b in BUILDS]
	weapon_builds = [{"name": b[0], "weapon": b[2], "gems": b[3], "effect": "poison",
		"variant": top_variant("poison") if b[4] == "top" else b[4], "base": b[5], "grade": b[6], "total": round(weapon_build_value("poison", b), 6)}
		for b in WEAPON_BUILDS]
	return {
		"_comment": "Generated by tools/gem-balance.py from the balance model (specs/effect-gems.md). Do not edit by hand.",
		"cap": CAP, "stack_r": STACK_R, "armor_slots": SLOTS, "weapon_slots": WEAPON_SLOTS, "pieces": PIECES,
		"armor_norm": W, "weapon_norm": WW,
		"heal": {"best": HEAL_BEST, "exp": HEAL_EXP},
		"drawback": {"one": DRAWBACK_ONE, "cap": DRAWBACK_CAP, "hit_seconds": DRAWBACK_HIT_S},
		"hit_strike_floor": HIT_STRIKE_FLOOR, "weapon_split": WEAPON_SPLIT, "max_hit_seconds": MAX_HIT_SECONDS, "tipped_arrow_scale": TIPPED_ARROW_SCALE,
		"infusion": [{"level": i + 1, "numeral": INFUSION[b], "base": b, "item": BASE_ITEMS[b], "factor": f}
			for i, (b, _, f, _) in enumerate(BASE_GEMS)],
		"armor_materials": {m: f for m, (f, _) in MATERIALS.items()}, "helmet_only": sorted(HELMET_ONLY),
		"run": {str(k): v for k, v in RUN.items()},
		"weapon_materials": WEAPON_ALL,
		"grades": GRADES, "form_max_tier": FORM_MAX_TIER,
		"tiers": {str(t): [{"label": o[0], "main": o[1],
			"other": next((g for kind, g in o[2] if kind == "other"), None),
			"drawback": next((g for kind, g in o[2] if kind == "drawback"), None), "odds": o[3]} for o in outs] for t, outs in TIERS.items()},
		"loot": {k: {"name": v[0], "bases": v[1], "tiers": v[2], "variant": v[3]} for k, v in LOOT.items()},
		"loot_inject": {f"minecraft:{t}": f"{MOD_ID}:inject/{t}" for t, *_ in LOOT_PLACES},
		"loot_chance": {f"minecraft:{t}": c for t, _, _, c, *_ in LOOT_PLACES},
		"loot_equipment": {f"minecraft:{TRIAL_EQUIPMENT[0]}": f"{MOD_ID}:trial_chamber_equipment"},
		"mob_armor_modifier": f"{MOD_ID}:mob_armor",
		"effects": effects,
		"inverse_pairs": [[a, b] for a, b, _ in INVERSE_PAIRS],
		"rules": RULES,
		"brewable": sorted(k for k in GEM_EFFECTS if brewable(k)),
		"checks": {"armor_builds": armor_builds, "weapon_builds": weapon_builds,
			"drawback_totals": [round(drawback_total(n), 6) for n in range(0, 13)],
			"heal_fraction": {f"{s:.3f}": round(heal_fraction(s), 6) for s in (0.034, 0.155, 0.495, 0.813, 0.95)}},
	}

def brewable(k):
	"""An Infused Gem brews for every effect with a potion (Luck too: its potion has no survival recipe, but potions
	from commands, other mods or loot brew) or a source item."""
	return bool(E[k]["potion"]) or k in ITEM_SOURCES

def brewing_recipes():
	"""(file name, recipe) for every brewable Infused Gem: potion (3 forms) or source item + base gem -> Infused Gem."""
	out = []
	for k in GEM_EFFECTS:
		if not brewable(k):
			continue
		e = E[k]
		for v in variant_factors(e["variants"]):
			for lvl, (b, _, _, _) in enumerate(BASE_GEMS, 1):
				if e["potion"]:
					for form, item in FORM_ITEMS.items():
						inp = {"item": item, "potion_contents": {"potions": potion_id(e, v)}}
						out.append((f"{form}_{v}_{e['potion']}_{b}", inp, k, v, form, lvl, b))
				else:
					name, ingredient = ITEM_SOURCES[k][v]
					out.append((f"{name}_{b}", {"item": ingredient}, k, v, "normal", lvl, b))
	recipes = []
	for name, inp, k, v, form, lvl, b in out:
		recipes.append((name + ".json", {"type": "minecraft:brewing", "input": inp, "reagent": {"item": BASE_ITEMS[b]},
			"output": {"id": f"{MOD_ID}:infused_gem", "components": {f"{MOD_ID}:infused_gem":
				{"effect": effect_id(k), "variant": v, "form": form, "infusion": lvl}}}}))
	return recipes

# ---------------------------------------------------------------- loot files (data: a datapack can override any of them)
DATA = MOD / "data" / MOD_ID
LOOT_GEMS_DIR = DATA / "loot_table" / "gems"
LOOT_INJECT_DIR = DATA / "loot_table" / "inject"
MODIFIER_DIR = DATA / "item_modifier"
GENERATED_DIRS = [RECIPE_DIR, LOOT_GEMS_DIR, LOOT_INJECT_DIR, MODIFIER_DIR]
VANILLA_DATA = ROOT / "build" / "mcsrc" / "26.3" / "vanilla" / "data" / "minecraft"
ARMOR_PIECES = ["helmet", "chestplate", "leggings", "boots"]
INFUSION_LEVEL = {b: i + 1 for i, (b, _, _, _) in enumerate(BASE_GEMS)}

def loot_infusions(level, craftable):
	"""Infusion levels a level's gems are drawn from (repeats weight them); craftable gems never Infusion VI."""
	return [INFUSION_LEVEL[b] for b in LOOT[level][1] if not (craftable and b == BEST_BASE)]

def gem_roll(level, effects=None):
	"""The roll fields of the mod's loot functions: no effects = any craftable gem effect."""
	d = {}
	if effects:
		d["effects"] = [effect_id(k) for k in effects]
	d.update({"infusion": loot_infusions(level, not effects), "tier": LOOT[level][2], "variant": LOOT[level][3]})
	return d

def gem_count(level):
	lo, hi = LOOT[level][5]
	return lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}

def level_pieces(level):
	return [f"minecraft:{m}_{p}" for m in LOOT[level][4] for p in ARMOR_PIECES]

def level_table(level):
	pieces = level_pieces(level)
	gem_weight = round(len(pieces) * (1 - GEAR_SHARE) / GEAR_SHARE)
	entries = [{"type": "minecraft:item", "name": f"{MOD_ID}:gem", "weight": gem_weight,
		"modifier": {"type": f"{MOD_ID}:random_gem", **gem_roll(level)}}]
	entries += [{"type": "minecraft:item", "name": item, "weight": 1,
		"modifier": {"type": f"{MOD_ID}:socket_random_gems", "count": gem_count(level), **gem_roll(level)}} for item in pieces]
	return {"type": "minecraft:empty", "pools": [{"rolls": 1, "entries": entries}]}

def inject_table(table, ctx, level, chance, found, general):
	entries = []
	both = general and found
	if general:
		entries.append({"type": "minecraft:loot_table", "value": f"{MOD_ID}:gems/{level}", "weight": round((1 - FOUND_SHARE) * 3) if both else 1})
	if found:
		entries.append({"type": "minecraft:item", "name": f"{MOD_ID}:gem", "weight": round(FOUND_SHARE * 3) if both else 1,
			"modifier": {"type": f"{MOD_ID}:random_gem", **gem_roll(level, found)}})
	condition = {"type": "minecraft:random_chance", "chance": chance}
	if ctx == "entity":
		condition = {"type": "minecraft:all_of", "terms": [{"type": "minecraft:killed_by_player"}, condition]}
	return {"type": f"minecraft:{ctx}", "pools": [{"rolls": 1, "condition": condition, "entries": entries}]}

def mob_armor_modifier():
	functions = []
	others = [m[1] for m in MOB_GEMS if m[1]]
	for who, predicate, level, chance in MOB_GEMS:
		if predicate:
			who_cond = {"type": "minecraft:entity_properties", "entity": "this", "predicate": predicate}
		else:
			who_cond = {"type": "minecraft:inverted", "term": {"type": "minecraft:any_of", "terms": [
				{"type": "minecraft:entity_properties", "entity": "this", "predicate": p} for p in others]}}
		functions.append({"type": f"{MOD_ID}:socket_random_gems",
			"condition": {"type": "minecraft:all_of", "terms": [who_cond, {"type": "minecraft:random_chance", "chance": chance}]},
			"count": gem_count(level), **gem_roll(level)})
	return {"type": "minecraft:sequence", "functions": functions}

def loot_files():
	files = {LOOT_GEMS_DIR / f"{level}.json": level_table(level) for level in LOOT}
	for place in LOOT_PLACES:
		files[LOOT_INJECT_DIR / f"{place[0]}.json"] = inject_table(*place)
	files[MODIFIER_DIR / "mob_armor.json"] = mob_armor_modifier()
	t, level, chance = TRIAL_EQUIPMENT
	files[MODIFIER_DIR / "trial_chamber_equipment.json"] = {"type": f"{MOD_ID}:socket_random_gems",
		"condition": {"type": "minecraft:random_chance", "chance": chance}, "count": gem_count(level), **gem_roll(level)}
	return files

def loot_quality(level, craftable):
	"""(weakest, strongest) gem quality a level's gems can have (a drawback-only roll aside)."""
	_, bases, tiers, variant, _, _ = LOOT[level]
	grades = [GRADES[g] for t in set(tiers) for _, g, _, _ in TIERS[t] if g]
	bs = [BASE[b] for b in bases if not (craftable and b == BEST_BASE)]
	if craftable:
		vmax = max(max(variant_factors(E[k]["variants"]).values()) for k in GEM_EFFECTS if E[k]["source"][0] != "found") if variant == "any" else \
			max(variant_factors(E[k]["variants"]).get("normal", 1.0) for k in GEM_EFFECTS if E[k]["source"][0] != "found")
		vmin = min(min(variant_factors(E[k]["variants"]).values()) for k in GEM_EFFECTS if E[k]["source"][0] != "found")
	else:
		vmax = vmin = 1.0
	return vmin * min(bs) * min(grades), vmax * max(bs) * max(grades)

def check_loot(errors, notes):
	found = [k for k in GEM_EFFECTS if E[k]["source"][0] == "found"]
	if sorted(found) != sorted(FOUND_ONLY_KEYS):
		errors.append(f"loot: FOUND_ONLY_KEYS {sorted(FOUND_ONLY_KEYS)} differ from the found-only gems {sorted(found)}")
	for k in found:
		levels = {p[2] for p in LOOT_PLACES if k in p[4]}
		if levels != set(E[k]["source"][1]):
			errors.append(f"loot: {k} is listed at {sorted(levels)} but its source says {sorted(E[k]['source'][1])}")
	for t, ctx, level, chance, fnd, general in LOOT_PLACES:
		if level not in LOOT or not 0 < chance <= 0.25 or not (fnd or general):
			errors.append(f"loot: bad place {t}")
		if VANILLA_DATA.exists() and not (VANILLA_DATA / "loot_table" / f"{t}.json").exists():
			errors.append(f"loot: vanilla 26.3 has no loot table {t}")
		if VANILLA_DATA.exists() and (VANILLA_DATA / "loot_table" / f"{t}.json").exists():
			vtype = json.loads((VANILLA_DATA / "loot_table" / f"{t}.json").read_text(encoding="utf-8")).get("type")
			if vtype != f"minecraft:{ctx}":
				errors.append(f"loot: {t} is a {vtype} table, not minecraft:{ctx}")
		for k in fnd:
			if k not in found:
				errors.append(f"loot: {t} lists {k}, which is not a found-only gem")
	if VANILLA_DATA.exists() and not (VANILLA_DATA / "loot_table" / f"{TRIAL_EQUIPMENT[0]}.json").exists():
		errors.append(f"loot: vanilla 26.3 has no loot table {TRIAL_EQUIPMENT[0]}")
	for level in LOOT:
		for item in level_pieces(level):
			if not re.search(rf"Item {item.split(':')[1].upper()} = ", (SRC / "world/item/Items.java").read_text(encoding="utf-8")) and SRC.exists():
				errors.append(f"loot: no item {item}")
	order = list(LOOT)
	for a, b in zip(order, order[1:]):
		if not loot_quality(b, True)[1] > loot_quality(a, True)[1]:
			errors.append(f"loot: {b} gems are not better than {a} gems")
	for level in LOOT:
		if loot_quality(level, True)[1] >= 1 - 1e-9:
			errors.append(f"loot: a craftable gem from {level} loot is perfect; perfect craftable gems are crafted only")
	if any(not 0 < m[3] <= 0.1 for m in MOB_GEMS) or not 0 < TRIAL_EQUIPMENT[2] <= 0.25:
		errors.append("loot: mob gear chances out of range")
	notes.append(f"loot: {len(LOOT_PLACES)} vanilla tables get one extra roll (chance {min(p[3] for p in LOOT_PLACES):.0%}-{max(p[3] for p in LOOT_PLACES):.0%}); "
		"each level's best gem beats the level below; no craftable gem from loot is perfect (Infusion VI is never used for them); "
		"every found-only gem is listed at exactly the levels its source names; every table and item exists in 26.3")

def check_export(errors):
	"""The WORN numbers match the effect table (the descriptions the owner reads)."""
	for k, amount in [("speed", 0.4), ("strength", 6.0), ("luck", 1.0), ("health_boost", 8.0), ("absorption", 16.0)]:
		if abs(E[k]["value"](0.5) - amount * 0.5) > 1e-9 or WORN[k]["attributes"][0][1] != amount:
			errors.append(f"export: {k} worn attribute {WORN[k]['attributes'][0][1]} differs from the effect table")
	if abs(E["regeneration"]["value"](0.5) - WORN["regeneration"]["heal_per_second"] * 0.5) > 1e-9:
		errors.append("export: regeneration heal rate differs from the effect table")
	if abs(E["resistance"]["value"](0.5) - WORN["resistance"]["damage_reduction"] * 0.5) > 1e-9:
		errors.append("export: resistance reduction differs from the effect table")
	if abs(E["poison"]["unit"] - HIT_UNIT["poison"]) > 1e-9 or E["weakness"]["unit"] != -WORN["weakness"]["attributes"][0][1] \
			or E["slowness"]["unit"] != -WORN["slowness"]["attributes"][0][1] or E["wither"]["unit"] != HIT_UNIT["wither"]:
		errors.append("export: a harmful unit differs from the effect table")
	for k in GEM_EFFECTS + list(DRAWBACK_ONLY):
		if k not in WORN:
			errors.append(f"export: {k} has no WORN entry (every gem works in armor)")
	for k in GEM_EFFECTS:
		if E[k]["source"][0] == "item" and k not in ITEM_SOURCES:
			errors.append(f"export: {k} is item-sourced but has no ITEM_SOURCES entry")
		if E[k]["source"][0] == "found" and brewable(k) and not E[k]["potion"]:
			errors.append(f"export: found-only {k} brews from an item")
	for k in ITEM_SOURCES:
		if set(ITEM_SOURCES[k]) != set(E[k]["variants"]):
			errors.append(f"export: {k} source items don't cover its variants")

def export_files():
	"""{path: text} for every generated file in the mod."""
	files = {BALANCE_JSON: json.dumps(export_data(), indent="\t", sort_keys=False) + "\n"}
	for name, r in brewing_recipes():
		files[RECIPE_DIR / name] = json.dumps(r, indent="\t") + "\n"
	for path, d in loot_files().items():
		files[path] = json.dumps(d, indent="\t") + "\n"
	return files

def stale_exports():
	files = export_files()
	# git may check text files out with CRLF on Windows; compare the text, not the line endings
	stale = [p for p, t in files.items() if not p.exists() or p.read_bytes().decode("utf-8").replace(chr(13), "") != t]
	extra = [p for d in GENERATED_DIRS if d.exists() for p in d.rglob("*.json") if p not in files]
	return files, stale, extra

def write_exports():
	files, stale, extra = stale_exports()
	for p in extra:
		p.unlink()
	for p in stale:
		p.parent.mkdir(parents=True, exist_ok=True)
		p.write_text(files[p], encoding="utf-8", newline="\n")
	print(f"export: {len(files)} files ({len(stale)} written, {len(extra)} removed) under {MOD.relative_to(ROOT)}")

def main():
	errors, notes = run_checks()
	check_export(errors)
	check_loot(errors, notes)
	for n in notes:
		print("ok   " + n)
	if errors:
		for e in errors:
			print("FAIL " + e, file=sys.stderr)
		print(f"{len(errors)} check(s) failed; nothing written.", file=sys.stderr)
		sys.exit(1)
	if "--check" in sys.argv:
		_, stale, extra = stale_exports()
		if stale or extra:
			print(f"FAIL mod export is stale ({len(stale)} changed, {len(extra)} extra); run python tools/gem-balance.py --export", file=sys.stderr)
			sys.exit(1)
		print("ok   mod export (balance.json, brewing recipes) is up to date")
		return
	write_exports()
	if "--export" in sys.argv:
		return
	S = document(notes)
	OUT_MD.parent.mkdir(parents=True, exist_ok=True)
	OUT_MD.write_text(render_md(S), encoding="utf-8", newline="\n")
	OUT_HTML.parent.mkdir(parents=True, exist_ok=True)
	OUT_HTML.write_text(render_html(S), encoding="utf-8", newline="\n")
	print(f"wrote {OUT_MD.relative_to(ROOT)} and {OUT_HTML.relative_to(ROOT)}")

if __name__ == "__main__":
	main()
