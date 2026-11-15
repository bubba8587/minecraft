#!/usr/bin/env python3
# dte:C4
"""Effect gems balance model and table generator (specs/effect-gems.md, "Balance model").

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
CAP = 0.95            # best possible total, as a fraction of the strongest vanilla potion level
STACK_R = 0.85        # each further gem of the same effect counts this much of the one before
SLOTS = 3             # gem slots per armor piece (owner)
PIECES = ["helmet", "chestplate", "leggings", "boots"]   # adjacency order (owner)
MIN_REL_STEP = 0.04   # every ladder step is at least +4% on the step below
MIN_ABS_DROP = 0.025  # from the best build, any one step down loses at least 0.025 of the scale
EARLY_SINGLE_FLOOR = 0.03       # the weakest possible single gem still does this much
EARLY_BUILD_RANGE = (0.10, 0.30)  # a typical early kit lands in this band
OFFENSIVE_WINDOW_S = 4  # an offensive gem's hit deals what the strongest potion does in 4 s
COOLDOWN_S = 30         # healing / harming / absorption burst cooldown

# ---------------------------------------------------------------- factor tables
# Base gems, weakest first. Diamond is the best (owner). Nether star: see NETHER_STAR_NOTE.
BASE_GEMS = [
	("amethyst", "Amethyst shard", 0.80, "Overworld geodes, first trip underground"),
	("quartz", "Nether quartz", 0.84, "First Nether trip"),
	("prismarine", "Prismarine (shard or crystals: open question)", 0.88, "Ocean monuments: a guarded structure"),
	("ender_pearl", "Ender pearl", 0.92, "Endermen; renewable but contested"),
	("nether_star", "Nether star", 0.96, "Wither kill; strength just under diamond, plus a trait (open question)"),
	("diamond", "Diamond", 1.00, "The best base (owner)"),
]
NETHER_STAR_NOTE = ("Nether star ranks just under diamond in raw strength (0.96), because the owner made diamond "
	"the best base, and earns its boss-kill cost with a trait instead. The proposed trait is a beacon-like aura "
	"(the beacon is made from a nether star): its effects also reach nearby players at half strength. "
	"The trait is an open question; the numbers below don't depend on it.")

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

# Enchant tier outcome grades (owner's words: weak / medium / strongest).
GRADES = {"weak": 0.50, "medium": 0.75, "strongest": 1.00}
FORM_MAX_TIER = {"normal": 1, "splash": 2, "lingering": 3}   # owner
# (label, intended-effect grade or None, [(other kind, grade)], proposed odds)
TIERS = {
	1: [("Weak negative effect", None, [("negative", "weak")], 0.50),
		("Weak intended effect", "weak", [], 0.50)],
	2: [("Medium intended + weak other", "medium", [("other", "weak")], 1.00)],
	3: [("Strongest alone", "strongest", [], 0.40),
		("Strongest + weak other", "strongest", [("other", "weak")], 0.30),
		("Strongest + medium other", "strongest", [("other", "medium")], 0.15),
		("Strongest + weak negative", "strongest", [("negative", "weak")], 0.15)],
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

# Each effect: name, potion (Potions.java id), variants {variant: (ticks, amplifier)}, role,
# anchor (what 1.0 is), value(s) numeric (monotonic), show(s) text, impl, src.
# role: main = a gem's intended effect; offensive = main, but applied to what the wearer hits;
#       trigger = main, chance-based on the wearer (mob flavour); secondary = only as a tier "other" roll;
#       negative = only as a tier "negative" roll (on the wearer).
E = {}
def effect(key, **kw):
	E[key] = kw

effect("speed", name="Speed", potion="swiftness", role="main",
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
effect("jump_boost", name="Jump Boost", potion="leaping", role="main",
	variants={"normal": (3600, 0), "long": (9600, 0), "strong": (1800, 1)},
	anchor="Jump Boost II: jump velocity +0.2 (0.1 per level) and safe fall distance +2 (1 per level)",
	value=lambda s: jump_apex(0.42 + 0.2 * s),
	show=lambda s: f"jump {jump_apex(0.42 + 0.2 * s):.2f} blocks (vanilla {jump_apex(0.42):.2f}), safe fall {3 + 2 * s:.1f}",
	impl="Attribute modifiers: jump_strength +0.2 x s and safe_fall_distance +2 x s. (Vanilla adds the jump boost after the block jump factor; the attribute is scaled by it, so it is a little weaker on honey.)",
	src="MobEffects.JUMP_BOOST, LivingEntity.getJumpBoostPower, Attributes.JUMP_STRENGTH 0.42")
effect("regeneration", name="Regeneration", potion="regeneration", role="main",
	variants={"normal": (900, 0), "long": (1800, 0), "strong": (450, 1)},
	anchor="Regeneration II: 1 HP every 25 ticks (interval 50 >> level) = 0.8 HP/s",
	value=lambda s: 0.8 * s, show=lambda s: f"{0.8 * s:.2f} HP/s (1 heart per {2 / (0.8 * s):.1f} s)",
	impl="Tick handler: heal 0.8 x s / 20 HP per tick while hurt (LivingEntity.heal takes floats). Not a MobEffectInstance, because vanilla levels are whole numbers.",
	src="RegenerationMobEffect")
effect("resistance", name="Resistance (from Turtle Master)", potion="turtle_master", role="main",
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
	variants={"normal": (6000, 0)},
	anchor="Luck I: luck +1 (only level)",
	value=lambda s: s, show=lambda s: f"+{s:.2f} luck",
	impl="Attribute modifier on luck, +s (exact).",
	src="MobEffects.LUCK")
effect("instant_health", name="Healing", potion="healing", role="main",
	variants={"normal": (1, 0), "strong": (1, 1)},
	anchor="Healing II: heals 8 HP at once (4 << level)",
	value=lambda s: 8 * s, show=lambda s: f"heals {8 * s:.1f} HP when below half health, every {COOLDOWN_S} s at most",
	impl=f"Instant made permanent as an emergency heal: when the wearer drops below half health, heal 8 x s, then a {COOLDOWN_S} s cooldown. Works the same on mobs.",
	src="HealOrHarmMobEffect")
effect("instant_damage", name="Harming", potion="harming", role="offensive",
	variants={"normal": (1, 0), "strong": (1, 1)},
	anchor="Harming II: 12 magic damage at once (6 << level)",
	value=lambda s: 12 * s, show=lambda s: f"next hit +{12 * s:.1f} magic damage, every {COOLDOWN_S} s at most",
	impl=f"Instant made permanent as a charged strike: the first melee hit after a {COOLDOWN_S} s cooldown deals +12 x s magic damage. Undead targets are healed instead, as in vanilla.",
	src="HealOrHarmMobEffect")
effect("poison", name="Poison", potion="poison", role="offensive",
	variants={"normal": (900, 0), "long": (1800, 0), "strong": (432, 1)},
	anchor="Poison II: 1 HP every 12 ticks (25 >> level) = 1.67 HP/s, never below 1 HP",
	value=lambda s: 25 / 12 * 0.8 * s,
	show=lambda s: f"hit poisons {25 / 12 * 0.8 * s:.2f} HP/s for {OFFENSIVE_WINDOW_S} s ({25 / 12 * 0.8 * s * OFFENSIVE_WINDOW_S:.1f} HP)",
	impl=f"Offensive: each melee hit poisons the target at 1.67 x s HP/s for {OFFENSIVE_WINDOW_S} s (refreshed, not stacked; never below 1 HP as vanilla). A gem mob effect whose amplifier encodes s in 1% steps keeps vanilla's icon and timer.",
	src="PoisonMobEffect")
effect("slowness", name="Slowness", potion="slowness", role="offensive",
	variants={"normal": (1800, 0), "long": (4800, 0), "strong": (400, 3)},
	anchor="Slowness IV (strong Slowness potion): movement speed -60% (-0.15 per level)",
	value=lambda s: 0.6 * s, show=lambda s: f"{pct(-0.6 * s)} speed (target for {OFFENSIVE_WINDOW_S} s, or self as a negative)",
	impl=f"Offensive: hits slow the target by 60% x s for {OFFENSIVE_WINDOW_S} s. As a negative roll: a permanent movement_speed modifier on the wearer.",
	src="MobEffects.SLOWNESS, Potions.STRONG_SLOWNESS")
effect("weakness", name="Weakness", potion="weakness", role="offensive",
	variants={"normal": (1800, 0), "long": (4800, 0)},
	anchor="Weakness I: attack damage -4 (only level)",
	value=lambda s: 4 * s, show=lambda s: f"-{4 * s:.2f} attack damage (target for {OFFENSIVE_WINDOW_S} s, or self as a negative)",
	impl=f"Offensive: hits weaken the target by 4 x s for {OFFENSIVE_WINDOW_S} s. As a negative roll: a permanent attack_damage modifier on the wearer.",
	src="MobEffects.WEAKNESS")
for key, name, extra in [
		("wind_charged", "Wind Charged", "a wind burst when the wearer dies"),
		("weaving", "Weaving", "cobwebs where the wearer dies"),
		("oozing", "Oozing", "2 slimes when the wearer dies"),
		("infested", "Infested", "1-2 silverfish when the wearer is hurt (vanilla 10% per hit)")]:
	base_chance = 0.1 if key == "infested" else 1.0
	effect(key, name=name, potion=key, role="trigger", variants={"normal": (3600, 0)},
		anchor=f"{name} I: {extra} (only level)",
		value=(lambda c: lambda s: c * s)(base_chance),
		show=(lambda c: lambda s: f"{c * s * 100:.1f}% chance")(base_chance),
		impl="Chance-based: the vanilla trigger fires with chance s x vanilla chance. Mob flavour (loot mobs); on players mostly a curiosity.",
		src=f"{''.join(p.title() for p in key.split('_'))}MobEffect")
# Secondary-only (no potion exists; anchors from the strongest natural source)
effect("haste", name="Haste", potion=None, role="secondary", variants={},
	anchor="Haste II (beacon): mining speed x1.4 (+0.2 per level), attack speed +20%",
	value=lambda s: 0.4 * s, show=lambda s: f"mining {pct(0.4 * s)}, attack speed {pct(0.2 * s)}",
	impl="Attribute modifiers: block_break_speed +0.4 x s and attack_speed +0.2 x s (add_multiplied_total).",
	src="Player.getDestroySpeed, MobEffects.HASTE, BeaconBlockEntity")
effect("absorption", name="Absorption", potion=None, role="secondary", variants={},
	anchor="Absorption IV (enchanted golden apple): 16 absorption HP (4 x (level + 1))",
	value=lambda s: 16 * s, show=lambda s: f"{16 * s:.1f} HP shield, refills every {COOLDOWN_S} s",
	impl=f"max_absorption +16 x s, refilled to full {COOLDOWN_S} s after it was last hit.",
	src="AbsorptionMobEffect, Consumables.ENCHANTED_GOLDEN_APPLE")
effect("health_boost", name="Health Boost", potion=None, role="secondary", variants={},
	anchor="Health Boost II: max health +8 (4 per level). No vanilla source gives it, so the anchor is a proposal.",
	value=lambda s: 8 * s, show=lambda s: f"+{8 * s:.1f} max health",
	impl="Attribute modifier on max_health, +8 x s (exact).",
	src="MobEffects.HEALTH_BOOST")
effect("dolphins_grace", name="Dolphin's Grace", potion=None, role="secondary", variants={},
	anchor="Dolphin's Grace: water drag 0.96 instead of 0.8 (0.9 sprinting) (on/off)",
	value=lambda s: 0.8 + 0.16 * s, show=lambda s: f"water drag {0.8 + 0.16 * s:.3f} (vanilla 0.800)",
	impl="On/off made partial: water slow-down 0.8 + 0.16 x s (mixin where travelInWater sets 0.96).",
	src="LivingEntity.travelInWater, DOLPHINS_GRACE_WATER_DRAG")
# Negative-only (tier 1 / tier 3 "weak negative" rolls, on the wearer). Slowness and weakness above double as negatives.
effect("mining_fatigue", name="Mining Fatigue", potion=None, role="negative", variants={},
	anchor="Mining Fatigue III (elder guardian): mining speed x0.027 (0.3 per level, compounding), attack speed -30%",
	value=lambda s: 1 - 0.027 ** s, show=lambda s: f"mining x{0.027 ** s:.2f}, attack speed {pct(-0.3 * s)}",
	impl="Attribute modifiers: block_break_speed x 0.027^s (matches levels I-III exactly at s = 1/3, 2/3, 1) and attack_speed -0.3 x s.",
	src="Player.getDestroySpeed, ElderGuardian")
effect("hunger", name="Hunger", potion=None, role="negative", variants={},
	anchor="Hunger III (pufferfish): exhaustion +0.015 per tick (0.005 per level)",
	value=lambda s: 0.3 * s, show=lambda s: f"extra hunger: 1 food point per {4 / (0.3 * s):.0f} s",
	impl="Tick handler: causeFoodExhaustion(0.015 x s) per tick.",
	src="HungerMobEffect, Consumables.PUFFERFISH")
effect("unluck", name="Bad Luck", potion=None, role="negative", variants={},
	anchor="Bad Luck I: luck -1",
	value=lambda s: s, show=lambda s: f"-{s:.2f} luck",
	impl="Attribute modifier on luck, -s.", src="MobEffects.UNLUCK")

NEGATIVE_POOL = ["slowness", "weakness", "mining_fatigue", "hunger", "unluck"]
OTHER_POOL = ["speed", "strength", "jump_boost", "regeneration", "resistance", "fire_resistance",
	"water_breathing", "night_vision", "slow_falling", "luck", "haste", "absorption", "health_boost", "dolphins_grace"]
GEM_EFFECTS = [k for k, v in E.items() if v["role"] in ("main", "offensive", "trigger")]
EXCLUDED = ("Not gems: nausea, blindness, darkness, glowing, levitation (no potion, harmful or visual only); "
	"bad omen, trial omen, raid omen, hero of the village (event triggers); saturation (permanent food is too strong); "
	"conduit power (a bundle of water breathing, haste and underwater sight: use those gems); wither (no potion); "
	"breath of the nautilus (26.x mount effect, no potion).")

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
	"world/entity/LivingEntity.java": [r"getAmplifier\(\) \+ 1\) \* 5", r"0\.1F \* \(this\.getEffect\(MobEffects\.JUMP_BOOST\)",
		r"Math\.min\(this\.getGravity\(\), 0\.01\)", r"visibilityPercent \*= 0\.7 \* coverPercentage", r"slowDown = 0\.96F",
		r"oxygenBonus \+ 1\.0"],
	"world/entity/player/Player.java": [r"\+ 1\) \* 0\.2F", r"Math\.pow\(0\.3, amplifier \+ 1\)"],
	"world/entity/Entity.java": [r"lava\(\), 4\.0F", r"getMaxAirSupply\(\) \{\s*return 300;"],
	"world/entity/ai/attributes/Attributes.java": [r'"attribute\.name\.jump_strength", 0\.42F', r'"attribute\.name\.gravity", 0\.08'],
	"world/item/component/Consumables.java": [r"HUNGER, 300, 2", r"ABSORPTION, 2400, 3"],
	"world/item/equipment/ArmorMaterials.java": [r"ArmorMaterial COPPER", r"ArmorMaterial TURTLE_SCUTE", r"ArmorMaterial NETHERITE"],
	"world/entity/monster/ElderGuardian.java": [r"MINING_FATIGUE, 6000, 2"],
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
	effects = re.sub(r"\s+", " ", (SRC / "world/effect/MobEffects.java").read_text(encoding="utf-8"))
	for pat in SRC_EXPECT_EFFECTS:
		if not re.search(r"register\( ?" + pat, effects):
			errors.append(f"source: MobEffects.java no longer matches {pat}")
		n += 1
	for rel, pats in SRC_EXPECT_FILES.items():
		p = SRC / rel
		text = p.read_text(encoding="utf-8") if p.exists() else ""
		for pat in pats:
			if not re.search(pat, text):
				errors.append(f"source: {rel} no longer matches {pat}")
			n += 1
	return f"{n} facts matched against build/mcsrc/26.3"

# ---------------------------------------------------------------- the model
W = sum(STACK_R ** j for j in range(SLOTS * len(PIECES)))

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

def stack(contribs):
	"""Total strength of one effect from every gem contribution to it (each <= 1)."""
	c = sorted(contribs, reverse=True)
	return CAP * sum(x * STACK_R ** j for j, x in enumerate(c)) / W

def gem_quality(effect_key, variant, base, grade):
	return variant_factors(E[effect_key]["variants"])[variant] * dict((b, f) for b, _, f, _ in BASE_GEMS)[base] * GRADES[grade]

def build_total(layout, gems_per_piece, quality):
	pf = piece_factors(layout)
	return stack([quality * pf[i] for i in range(4) for _ in range(gems_per_piece[i])])

def top_variant(key):
	vf = variant_factors(E[key]["variants"])
	return max(vf, key=vf.get)

# ---------------------------------------------------------------- checks
def ladder_ok(name, values, errors):
	for (a_name, a), (b_name, b) in zip(values, values[1:]):
		if not (b >= a * (1 + MIN_REL_STEP) - 1e-12):
			errors.append(f"{name}: {a_name} {a} -> {b_name} {b} is not a visible step (+{MIN_REL_STEP:.0%} needed)")

def run_checks():
	errors, notes = [], []
	notes.append("vanilla facts: " + verify_source(errors))
	# 1. Every ladder strictly up with a visible step.
	ladder_ok("base gems", [(b, f) for b, _, f, _ in BASE_GEMS], errors)
	ladder_ok("armor", [(m, MATERIALS[m][0]) for m in MAIN_LADDER], errors)
	ladder_ok("run", [(k, v) for k, v in sorted(RUN.items())], errors)
	ladder_ok("grades", list(GRADES.items()), errors)
	for k in GEM_EFFECTS:
		vf = variant_factors(E[k]["variants"])
		ladder_ok(f"{k} variants", [(v, vf[v]) for v in ("normal", "long", "strong") if v in vf], errors)
	if BASE_GEMS[-1][0] != "diamond":
		errors.append("diamond must be the best base gem (owner)")
	if max(MATERIALS.values())[0] != MATERIALS["netherite"][0] or sum(1 for v in MATERIALS.values() if v[0] >= 1.0) != 1:
		errors.append("netherite must be the only best armor material")
	notes.append("every ladder step is at least +4% on the one below")

	# 2. Gem quality: 1.0 only for strongest potion + lingering + tier 3 + diamond, for every gem effect.
	for k in GEM_EFFECTS:
		hits = []
		for var in E[k]["variants"]:
			for form, maxtier in FORM_MAX_TIER.items():
				for tier in range(1, maxtier + 1):
					for label, grade, _, _ in TIERS[tier]:
						if grade is None:
							continue
						for b, _, _, _ in BASE_GEMS:
							q = gem_quality(k, var, b, grade)
							if q > 1 + 1e-12:
								errors.append(f"{k}: gem quality above 1")
							if q >= 1 - 1e-12:
								hits.append((var, form, tier, b))
		want = [(top_variant(k), "lingering", 3, "diamond")]
		if sorted(set(hits)) != want:
			errors.append(f"{k}: best gem reachable by {hits}, expected only {want}")
	notes.append("a gem's own strength reaches 1.0 only from the strongest potion + lingering + tier 3 + diamond, for every effect")

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
		errors.append(f"the cap is reached by {best_layouts}, expected only full netherite with 12 gems")
	if CAP - second < MIN_ABS_DROP:
		errors.append(f"second-best armor setup {second:.3f} is too close to the best {CAP}")
	notes.append(f"{count:,} armor layouts x gem counts enumerated: only full netherite with 12 gems reaches {CAP}; next best {second:.3f}")

	# 4. One step down from the best build loses a visible amount, for every effect.
	for k in GEM_EFFECTS:
		for label, t in downgrade_rows(k):
			if CAP - t < MIN_ABS_DROP:
				errors.append(f"{k}: '{label}' only drops {CAP - t:.3f} from the best (need {MIN_ABS_DROP})")
	notes.append(f"from the best build, every single step down (potion, base, tier, material, run, one gem fewer) loses at least {MIN_ABS_DROP}")

	# 5. Early game is small but noticeable.
	worst = min(stack([gem_quality(k, "normal", "amethyst", "weak") * MATERIALS["leather"][0] * RUN[1]]) for k in GEM_EFFECTS)
	if worst < EARLY_SINGLE_FLOOR:
		errors.append(f"weakest single gem {worst:.3f} is below the floor {EARLY_SINGLE_FLOOR}")
	early = [build_total(["iron"] * 4, [1, 1, 1, 1], gem_quality(k, "normal", "quartz", "weak")) for k in GEM_EFFECTS]
	if not all(EARLY_BUILD_RANGE[0] <= x <= EARLY_BUILD_RANGE[1] for x in early):
		errors.append(f"early kit range {min(early):.3f}-{max(early):.3f} outside {EARLY_BUILD_RANGE}")
	notes.append(f"weakest single gem (normal potion, amethyst, tier 1, lone leather piece) = {worst:.3f}; early kit (iron set, 4 quartz tier-1 gems) = {min(early):.3f}-{max(early):.3f}")

	# 6. Stacking: never beyond cap x the strongest gem; adding a gem never lowers it; 12 weak < 4 strong.
	rng = random.Random(26_3)
	qualities = sorted({gem_quality(k, v, b, g) for k in GEM_EFFECTS for v in E[k]["variants"] for b, _, _, _ in BASE_GEMS for g in GRADES})
	for _ in range(20000):
		n = rng.randint(1, 12)
		cs = [rng.choice(qualities) * rng.choice(list(MATERIALS.values()))[0] * rng.choice(list(RUN.values())) for _ in range(n)]
		t = stack(cs)
		if t > CAP * max(cs) + 1e-12 or t >= 1.0:
			errors.append(f"stack {cs} = {t} beats cap x strongest gem")
			break
		if n < 12:
			extra = rng.choice(qualities)
			if stack(cs + [extra]) < t - 1e-12:
				errors.append(f"adding gem {extra} to {cs} lowered the total")
				break
	for m in MAIN_LADDER:
		weak12 = build_total([m] * 4, [3, 3, 3, 3], gem_quality("speed", "strong", "diamond", "weak"))
		strong4 = build_total([m] * 4, [1, 1, 1, 1], gem_quality("speed", "strong", "diamond", "strongest"))
		if not weak12 < strong4:
			errors.append(f"{m}: 12 tier-1 gems ({weak12:.3f}) beat 4 tier-3 gems ({strong4:.3f})")
	notes.append(f"stacking: 20,000 random stacks never pass {CAP} x their strongest gem; adding a gem never lowers a total; in every armor set 12 best tier-1 gems lose to 4 tier-3 gems")

	# 7. In-game mappings move one way and never reach the vanilla anchor.
	for k, e in E.items():
		vals = [e["value"](i / 100 * CAP) for i in range(0, 101)]
		if not all(b > a for a, b in zip(vals, vals[1:])):
			errors.append(f"{k}: in-game value is not strictly increasing in strength")
		at_one = e["value"](0.99999) if k == "water_breathing" else e["value"](1.0)
		if not e["value"](CAP) < at_one:
			errors.append(f"{k}: the cap reaches the vanilla anchor")
	notes.append("every in-game mapping rises with strength and stays short of the vanilla level at the cap")
	return errors, notes

# ---------------------------------------------------------------- builds for the tables
def downgrade_rows(k):
	tv = top_variant(k)
	vf = variant_factors(E[k]["variants"])
	best_q = gem_quality(k, tv, "diamond", "strongest")
	rows = []
	lower = [v for v in vf if vf[v] < vf[tv]]
	if lower:
		v2 = max(lower, key=vf.get)
		rows.append((f"{v2} potion instead of {tv}", build_total(["netherite"] * 4, [3] * 4, gem_quality(k, v2, "diamond", "strongest"))))
	rows += [
		("nether star base instead of diamond", build_total(["netherite"] * 4, [3] * 4, gem_quality(k, tv, "nether_star", "strongest"))),
		("splash potion (tier 2 at best: medium)", build_total(["netherite"] * 4, [3] * 4, gem_quality(k, tv, "diamond", "medium"))),
		("full diamond armor", build_total(["diamond"] * 4, [3] * 4, best_q)),
		("diamond boots break the run", build_total(["netherite"] * 3 + ["diamond"], [3] * 4, best_q)),
		("one gem fewer (11)", build_total(["netherite"] * 4, [3, 3, 3, 2], best_q)),
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
	("Best", "Full netherite set, 12 gems; lingering strong potion, diamond, tier 3",
		["netherite"] * 4, [3] * 4, "top", "diamond", "strongest"),
]
LADDER_EFFECTS = ["speed", "strength", "regeneration", "fire_resistance"]

def build_value(k, b):
	_, _, layout, gems, var, base, grade = b
	if var == "top":
		var = top_variant(k)
	return build_total(layout, gems, gem_quality(k, var, base, grade))

# ---------------------------------------------------------------- document
def fmt(x, d=2):
	return f"{x:.{d}f}"

def document(notes):
	S = []  # ("h1"|"h2"|"h3"|"p"|"ul"|"table", ...)
	S.append(("h1", "Effect gems: balance model"))
	S.append(("p", "Generated by `tools/gem-balance.py` from the owner's design in `specs/effect-gems.md`. Do not edit by hand: change the script and rerun it. Vanilla numbers come from the decompiled Minecraft 26.3 source and the script re-checks them against it."))
	S.append(("h2", "The formula in plain words"))
	S.append(("ul", [
		"Every effect is measured on one scale: **1.0 = the strongest vanilla potion level of that effect** (Speed II, Strength II, Regeneration II, Resistance IV from strong Turtle Master...). For on/off effects 1.0 is the full effect.",
		f"**One gem's strength** = potion factor x base gem factor x enchant grade x armor material factor x matching-run factor. Each is at most 1.0.",
		f"**Stacking:** a piece holds {SLOTS} gems, so 12 in all. The gems of one effect are lined up strongest first; the first counts fully, each next one {STACK_R:.0%} of the one before. The sum is scaled so that 12 perfect gems give exactly **{CAP}**. So a full kit of perfect gems is the only way to {CAP}, and a stack can never beat {CAP} x its strongest gem: many weak gems stay weak.",
		f"**The cap is {CAP}**: 5% short of vanilla's best. Close enough that the endgame feels like a potion you never drink (Speed +38% instead of +40%), far enough that on/off effects still show it (5% of fire damage still lands; air lasts 5 minutes, not forever).",
		"**Potion form doesn't multiply**: it sets the highest enchant tier (normal 1, splash 2, lingering 3), and the tier sets the grade. So lingering is needed for the strongest grade.",
		"A gem's \"other\" or \"negative\" effect from the enchant roll uses the same formula with its own grade and joins that effect's stack.",
	]))
	S.append(("p", "Total for an effect, in symbols: `total = 0.95 x sum(gem_i x 0.85^(rank_i)) / sum(0.85^0..0.85^11)`, with `gem = potion x base x grade x material x run`."))

	S.append(("h2", "Factor tables"))
	S.append(("h3", "Base gem"))
	S.append(("table", ["Base gem", "Factor", "Why here"], [[n, fmt(f), w] for _, n, f, w in BASE_GEMS]))
	S.append(("p", NETHER_STAR_NOTE))
	S.append(("h3", "Potion variant (by what that effect really has in 26.3)"))
	S.append(("table", ["Variants that exist", "normal", "long", "strong"], [
		["normal + long + strong (Swiftness, Strength, Leaping, Regeneration, Poison, Slowness, Turtle Master)", "0.80", "0.85", "1.00"],
		["normal + long (Fire Resistance, Night Vision, Invisibility, Water Breathing, Slow Falling, Weakness)", "0.90", "1.00", "-"],
		["normal + strong (Healing, Harming)", "0.80", "-", "1.00"],
		["one only (Luck, Wind Charged, Weaving, Oozing, Infested)", "1.00", "-", "-"],
	]))
	S.append(("h3", "Potion form -> highest enchant tier (owner)"))
	S.append(("table", ["Form", "Highest tier", "Best grade reachable"], [
		["Normal", "1", "weak"], ["Splash", "2", "medium"], ["Lingering", "3", "strongest"]]))
	S.append(("h3", "Enchant tier outcomes"))
	S.append(("table", ["Tier", "Outcome", "Intended effect", "Extra effect", "Proposed odds"],
		[[str(t), lab, (f"{g} ({GRADES[g]:.2f})" if g else "-"),
			", ".join(f"{kind} {gr} ({GRADES[gr]:.2f})" for kind, gr in ex) or "-", f"{p:.0%}"]
			for t, outs in TIERS.items() for lab, g, ex, p in outs]))
	S.append(("p", "\"Other\" rolls from: " + ", ".join(E[k]["name"] for k in OTHER_POOL) + ". \"Negative\" rolls from: " + ", ".join(E[k]["name"] for k in NEGATIVE_POOL) + " (no poison: a permanent poison would pin the wearer at 1 HP)."))
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
	S.append(("h3", "Stacking weight per gem (strongest first)"))
	S.append(("table", ["Gem #"] + [str(i + 1) for i in range(12)],
		[["Share of the cap"] + [f"{STACK_R ** i / W:.3f}" for i in range(12)],
		 ["Running total (perfect gems)"] + [f"{CAP * sum(STACK_R ** j for j in range(i + 1)) / W:.3f}" for i in range(12)]]))

	S.append(("h2", "Each effect: what the scale means in game"))
	cols = [0.05, 0.10, 0.25, 0.50, 0.75, CAP]
	for role, title in [("main", "Gem effects (on the wearer)"), ("offensive", "Offensive gem effects (on what the wearer hits)"),
			("trigger", "Trigger gem effects (chance, mostly for mobs)"), ("secondary", "Only as an \"other\" roll (no potion exists)"),
			("negative", "Only as a \"negative\" roll (on the wearer)")]:
		keys = [k for k in E if E[k]["role"] == role]
		S.append(("h3", title))
		S.append(("table", ["Effect", "1.0 means (vanilla)"] + [f"s = {c:.2f}" for c in cols],
			[[E[k]["name"], E[k]["anchor"]] + [E[k]["show"](c) for c in cols] for k in keys]))
	S.append(("p", EXCLUDED))
	S.append(("h3", "How each effect is made partial"))
	S.append(("table", ["Effect", "Implementation", "Source (26.3)"], [[E[k]["name"], E[k]["impl"], E[k]["src"]] for k in E]))
	S.append(("p", "Gem effects are attribute modifiers, data components or small tick and damage hooks, not potion effect instances: vanilla levels are whole numbers and their icons and timers would claim a full level. Where a vanilla effect instance is wanted (offensive hits, for the icon and timer on the target), a gem mob effect whose amplifier counts strength in 1% steps does it. A gem and a drunk potion of the same effect don't add up: the stronger applies (open question 10)."))

	S.append(("h2", "Progression ladders"))
	S.append(("table", ["Build", "What it is"] + [E[k]["name"] for k in LADDER_EFFECTS],
		[[b[0], b[1]] + [f"{build_value(k, b):.3f}: {E[k]['show'](build_value(k, b))}" for k in LADDER_EFFECTS] for b in BUILDS]))
	S.append(("h3", "One step down from the best (Speed)"))
	S.append(("table", ["Change from the best build", "Total", "In game"],
		[["(best)", fmt(CAP, 3), E["speed"]["show"](CAP)]] + [[lab, fmt(t, 3), E["speed"]["show"](t)] for lab, t in downgrade_rows("speed")]))

	S.append(("h2", "Checks (all passed)"))
	S.append(("ul", notes))

	S.append(("h2", "Open questions for the owner"))
	S.append(("ol", [
		f"Cap at {CAP} (5% short of the strongest potion)? yes / no (0.90 would make the gap easier to feel).",
		"Nether star's special part: (a) aura, its effects also reach nearby players at half strength (model's proposal), (b) never rolls a negative, (c) nothing special, it just ranks between ender pearl and diamond.",
		"Prismarine base gem: (a) prismarine shard, (b) prismarine crystals.",
		"Gold armor: (a) chainmail-level factor 0.83 (model), (b) a magic bonus up to diamond-level 0.93, echoing gold's high enchantability.",
		"Turtle helmet counts as its own material (iron-level, always a run of 1)? yes / no.",
		"Resistance's 1.0: (a) Resistance IV from strong Turtle Master, 80% less damage (the rule as written; gems up to 76%), (b) Resistance II, the beacon level, 40% (gems up to 38%).",
		"Turtle Master gems also carry Turtle Master's Slowness, on the wearer? yes / no.",
		"Harmful-potion gems (poison, slowness, weakness, harming) work on what the wearer hits, not on the wearer? yes / no.",
		f"Healing gems: (a) emergency heal below half health, {COOLDOWN_S} s cooldown (model), (b) a heal every {COOLDOWN_S} s regardless.",
		"A gem and a drunk potion of the same effect: the stronger applies, they don't add? yes / no.",
		"Undead wearers follow vanilla's inversion (a healing gem hurts a zombie, harming heals it)? yes / no.",
		"Invisibility gems give stealth only (mobs notice you later, hidden on the locator bar), never a see-through body? yes / no.",
		"Wind Charged, Weaving, Oozing and Infested gems exist only as mob loot? yes / no.",
		"Effects with no potion (haste, absorption, health boost, dolphin's grace) can appear as an \"other\" roll? yes / no.",
		"Enchant odds as proposed (tier 1: 50/50; tier 3: alone 40%, +weak other 30%, +medium other 15%, +weak negative 15%)? yes / no.",
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
		f"<title>Effect Gems Balance</title><style>{css}</style></head><body>\n" + "\n".join(body) + "\n</body></html>\n")

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
