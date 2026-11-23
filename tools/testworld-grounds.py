#!/usr/bin/env python3
# dte:A3
"""Generate the Gem Testing Grounds builder functions, testworlds/builder/data/bubba_testworld/function/grounds*.mcfunction
(specs/testworld.md). Edit this script, not the generated .mcfunction files, then run:

  python tools/testworld-grounds.py          then  python tools/build-testworld.py

Player-visible names come from the mods' own lang files, so a rename in a mod reaches the test world on the next run:
effect names from Infused Gems (falling back to vanilla's lang in build/mcsrc/, then to the key), and the gem level
label ("Gem Power") from the gem item's name pattern. Wording that only exists on signs is in the constants below.
Every sign line is checked against the sign limits (15 characters, 13 bold). Standard library only.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "testworlds" / "builder" / "data" / "bubba_testworld" / "function"
GEMS_LANG = ROOT / "mods" / "bubbas_infused_gems" / "src" / "main" / "resources" / "assets" / "bubbas_infused_gems" / "lang" / "en_us.json"
VANILLA_LANG = ROOT / "build" / "mcsrc" / "26.3" / "vanilla" / "assets" / "minecraft" / "lang" / "en_us.json"


def _lang(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


LANG = _lang(GEMS_LANG)
VANILLA = _lang(VANILLA_LANG)
# The gem level label, from "Gem: Gem Power %s, %s" (item.bubbas_infused_gems.gem.named).
_m = re.match(r"[^:]*:\s*(.+?)\s*%s", LANG.get("item.bubbas_infused_gems.gem.named", ""))
LEVEL = _m.group(1) if _m else "Gem Power"

# Sign-only wording (not in any mod's lang file).
PERFECT_SET = "perfect set"                      # a perfect netherite kit sign
TWIN_SET = f"{LEVEL} V 88%"                      # its Gem Power V twin, one-word effect names
TWIN_SET_SHORT = f"{LEVEL.split()[-1]} V 88% +"  # the twin's line under a two-line effect name
TWIN_KEY_1, TWIN_KEY_2 = "birch: Gem", f"{LEVEL.split()[-1]} V (88%)"  # wardrobe entrance sign
FILES = {}
cur = None


def begin(name, header):
    global cur
    cur = FILES.setdefault(name, [])
    cur.append(f"# {header}")


SEEN = {}


def c(s):
    import re
    m = re.match(r"setblock (\S+ \S+ \S+) ", s)
    if m:
        assert m.group(1) not in SEEN, f"two setblocks at {m.group(1)}: {SEEN.get(m.group(1))[:60]} / {s[:60]}"
        SEEN[m.group(1)] = s
    cur.append(s)


def q(s):
    """SNBT string literal: single quotes when the text has double quotes."""
    if '"' in s:
        assert "'" not in s, s
        return "'" + s + "'"
    return '"' + s.replace("\\", "\\\\") + '"'


def line(text, color=None, bold=False, cmd=None):
    assert len(text) <= (13 if bold else 15), f"sign line too long: {text!r}"
    parts = [f"text:{q(text)}"]
    if bold:
        parts.append("bold:true")
    if color:
        parts.append(f'color:"{color}"')
    if cmd:
        parts.append(f'click_event:{{action:"run_command",command:{q(cmd)}}}')
    return "{" + ",".join(parts) + "}"


def sign(x, y, z, block, lines, glow=False):
    """lines: up to 4 tuples (text, color, bold, cmd). A sign with any command is a click sign (allow_op_features)."""
    lines = list(lines) + [("",)] * (4 - len(lines))
    msgs = [line(*l) if isinstance(l, tuple) else line(l) for l in lines]
    clickable = any(isinstance(l, tuple) and len(l) > 3 and l[3] for l in lines)
    nbt = []
    if clickable:
        nbt.append("allow_op_features:1b")
    else:
        nbt.append("is_waxed:1b")
    ft = f"messages:[{','.join(msgs)}]"
    if glow:
        ft = "has_glowing_text:1b," + ft
    nbt.append(f"front_text:{{{ft}}}")
    c(f"setblock {x} {y} {z} {block}{{{','.join(nbt)}}}")


def stand(rot, wood="oak"):
    return f"minecraft:{wood}_sign[rotation={rot}]"


def wall(facing, wood="oak"):
    return f"minecraft:{wood}_wall_sign[facing={facing}]"


def info(x, y, z, block, title, color, *rest):
    sign(x, y, z, block, [(title, color, True)] + [(r, None) for r in rest])


GEM = '{{effect:"{id}",infusion:6,tier:3,main:1.0f}}'
STRIP = "item override entity @s armor.* with minecraft:air"
SOUND = "playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"

EFFECT_KEYS = ["speed", "strength", "jump_boost", "regeneration", "resistance", "turtle_master", "fire_resistance",
               "water_breathing", "night_vision", "invisibility", "slow_falling", "luck", "instant_health", "wind_charged",
               "weaving", "oozing", "silence", "ender", "breeze", "haste", "absorption", "health_boost", "dolphins_grace",
               "saturation", "conduit_power", "hero_of_the_village", "poison", "slowness", "weakness", "instant_damage",
               "wither", "hunger", "mining_fatigue", "glowing", "levitation", "blindness", "nausea", "darkness", "infested"]


def effect_name(key):
    """The effect's player-visible name: Infused Gems' lang first, then vanilla's."""
    for k in (f"bubbas_infused_gems.effect.{key}", f"effect.bubbas_infused_gems.{key}", f"effect.bubbas_infused_gems.gem_{key}"):
        if k in LANG:
            return LANG[k]
    return VANILLA.get(f"effect.minecraft.{key}", key.replace("_", " ").title())


def sign_lines(name, width=13):
    """A bold name as one sign line, or two when it is longer than a bold line holds (most even split)."""
    if len(name) <= width:
        return [name]
    words = name.split()
    splits = [(" ".join(words[:i]), " ".join(words[i:])) for i in range(1, len(words))]
    fits = [p for p in splits if len(p[0]) <= width and len(p[1]) <= width]
    assert fits, f"name too long for a sign: {name!r}"
    return list(min(fits, key=lambda p: (abs(len(p[0]) - len(p[1])), -len(p[0]))))


NAMES = {k: sign_lines(effect_name(k)) for k in EFFECT_KEYS}
MOD_IDS = {"turtle_master", "silence", "ender", "breeze"}
HARMFUL = ["poison", "slowness", "weakness", "instant_damage", "wither", "hunger", "mining_fatigue", "glowing",
           "levitation", "blindness", "nausea", "darkness", "infested"]


def eid(key):
    return ("bubbas_infused_gems:" if key in MOD_IDS else "minecraft:") + key


# Gems whose test needs a tool (owner: Haste needs mining tools, not a sword): the kit sign also gives these.
KIT_TOOLS = {
    "haste": (["netherite_pickaxe", "netherite_shovel", "netherite_axe"], "+ sword, tools"),
    "mining_fatigue": (["netherite_pickaxe"], "sword, pickaxe"),
    "conduit_power": (["netherite_pickaxe"], "+ sword, pick"),
    "luck": (["fishing_rod"], "+ sword, rod"),
}


def loot_give(items):
    """One command giving several items: an inline loot table, one pool per item."""
    pools = ",".join(f'{{rolls:1,entries:[{{type:"minecraft:item",name:"minecraft:{i}"}}]}}' for i in items)
    return f"loot give @s loot {{pools:[{pools}]}}"


def kit_sign(x, y, z, block, key, material="netherite", note="click to wear", infusion=6):
    """Click: take off worn armor, /gems kit <effect> [material] [infusion], equip sound (and tools for KIT_TOOLS
    gems). Perfect netherite sets say "perfect set"; Gem Power V twins (88%) say so (owner: test what the perfect set's
    extras add)."""
    color = "dark_red" if key in HARMFUL else "dark_green"
    kit = f"gems kit {key}" + ("" if material == "netherite" and infusion == 6 else f" {material}")
    if infusion != 6:
        kit += f" {infusion}"
    name = NAMES[key]
    if infusion == 5:
        mat, mat2 = TWIN_SET, TWIN_SET_SHORT
    elif material != "netherite":
        mat = {"gold": "golden"}.get(material, material) + " set"
        mat2 = mat + " +"
    else:
        mat, mat2 = PERFECT_SET, PERFECT_SET + " +"
    if key in KIT_TOOLS and material == "netherite":
        items, label = KIT_TOOLS[key]
        if len(name) == 1:
            lines = [(name[0], color, True, STRIP), (mat, None, False, kit), (label, None, False, loot_give(items)),
                     (note, "dark_gray" if infusion == 5 else "gray", False, SOUND)]
        else:
            lines = [(name[0], color, True, STRIP), (name[1], color, True, kit), (mat2, None, False, loot_give(items)),
                     (label, None, False, SOUND)]
        sign(x, y, z, block, lines)
        return
    if len(name) == 1:
        lines = [(name[0], color, True, STRIP), (mat, None, False, kit), ("+ sword", None, False, SOUND),
                 (note, "dark_gray" if infusion == 5 else "gray")]
    else:
        lines = [(name[0], color, True, STRIP), (name[1], color, True, kit), (mat2, None, False, SOUND),
                 ("sword", None)]
    sign(x, y, z, block, lines)


def kit_pair(x, y, z, block, block_v, key, note="click to wear", dx=1, dz=0):
    """A perfect kit sign and its Gem Power V twin (on a birch sign) next to it."""
    kit_sign(x, y, z, block, key, note=note)
    kit_sign(x + dx, y, z + dz, block_v, key, note=note, infusion=5)


def weapon_sign(x, y, z, block, key, item="netherite_sword", label="sword, 2 gems", extra=None):
    g = GEM.format(id=eid(key))
    cmd = f"give @s minecraft:{item}[bubbas_infused_gems:socketed_gems=[{g},{g}]]"
    name = NAMES[key]
    color = "dark_red" if key in HARMFUL else "dark_green"
    lines = [(name[0], color, True, cmd)]
    if len(name) > 1:
        lines.append((name[1], color, True))
    lines.append((label, None, False, extra))
    lines.append(("click to get", "gray"))
    sign(x, y, z, block, lines)


def mode_signs(x1, y, z1, block, dx=1, dz=0):
    sign(x1, y, z1, block, [("Survival", "dark_blue", True, "gamemode survival @s"), "click to take", "damage here"])
    sign(x1 + dx, y, z1 + dz, block, [("Creative", "dark_blue", True, "gamemode creative @s"), "click to stop", "taking damage"])


def lights(x1, z1, x2, z2, y=-56, step=6):
    xs = list(range(x1, x2 + 1, step))
    zs = list(range(z1, z2 + 1, step))
    for x in xs:
        for z in zs:
            c(f"setblock {x} {y} {z} minecraft:light[level=15]")


def path(x1, z1, x2, z2):
    c(f"fill {x1} -61 {z1} {x2} -61 {z2} minecraft:dirt_path replace minecraft:grass_block")


def cmdblock(x, y, z, kind, facing, command, auto):
    block = {"impulse": "command_block", "repeat": "repeating_command_block", "chain": "chain_command_block"}[kind]
    c(f"setblock {x} {y} {z} minecraft:{block}[facing={facing}]{{Command:{q(command)},auto:{1 if auto else 0}b,TrackOutput:0b}}")


# ---------------------------------------------------------------- paths, hub signs, game rules
begin("grounds", "Gem Testing Grounds: x -47..46, z 11..63, south of the hub (specs/testworld.md). Paths and hub signs;"
      "\n# each area has its own grounds_* function.")
c("gamerule command_block_output false")
c("scoreboard objectives add tw_clock dummy")
c("scoreboard objectives add tw_hp dummy")
c("scoreboard players set #gallery tw_clock 0")
path(-1, 11, 1, 15)
path(-46, 16, 46, 17)   # avenue A
path(-47, 33, 46, 34)   # avenue B
path(-46, 50, 46, 51)   # avenue C
path(-27, 18, -26, 49)  # west lane
path(27, 18, 28, 49)    # east lane
path(-47, 35, -47, 39)  # to the live range door
info(-7, -60, 3, stand(0), "Gem Grounds", "dark_purple", "behind you:", "wardrobe, labs", "and ranges")
info(-2, -60, 11, stand(8), "Gem Grounds", "dark_purple", "click signs to", "wear gem sets;", "labs both ways")
info(2, -60, 11, stand(8), "Wardrobe", "dark_purple", "straight on:", "every gem set,", "one click each")
lights(-45, 16, 45, 17, step=10)

# ---------------------------------------------------------------- wardrobe
begin("grounds_wardrobe", "Wardrobe: aisle x -3..3, z 18..32 between walls at x -4 and x 4 (z 19..31, 3 high). Click a sign:"
      "\n# take off worn armor, /gems kit <effect> (perfect netherite set, 3 gems a piece, and a sword).")
c("fill -3 -61 18 3 -61 32 minecraft:dark_oak_planks")
c("fill -4 -60 19 -4 -57 31 minecraft:dark_oak_planks")
c("fill 4 -60 19 4 -57 31 minecraft:dark_oak_planks")
c("fill -4 -56 19 -4 -56 31 minecraft:dark_oak_slab")
c("fill 4 -56 19 4 -56 31 minecraft:dark_oak_slab")
WEST_TOP = ["speed", "strength", "jump_boost", "regeneration", "resistance", "turtle_master", "fire_resistance",
            "water_breathing", "night_vision", "invisibility", "slow_falling", "luck", "instant_health"]
WEST_LOW = ["wind_charged", "weaving", "oozing", "silence", "ender", "breeze", "haste", "absorption", "health_boost",
            "dolphins_grace", "saturation", "conduit_power", "hero_of_the_village"]
for i, k in enumerate(WEST_TOP):
    kit_sign(-3, -57, 19 + i, wall("east"), k)
    kit_sign(-3, -58, 19 + i, wall("east", "birch"), k, infusion=5)
for i, k in enumerate(WEST_LOW):
    kit_sign(-3, -59, 19 + i, wall("east"), k)
    kit_sign(-3, -60, 19 + i, wall("east", "birch"), k, infusion=5)
for i, k in enumerate(HARMFUL):
    note = "harms you!" if k != "levitation" else "you will rise!"
    kit_sign(3, -57, 19 + i, wall("west"), k, note=note)
    kit_sign(3, -58, 19 + i, wall("west", "birch"), k, note=note, infusion=5)
for i, m in enumerate(["leather", "chainmail", "copper", "iron", "gold", "diamond", "netherite"]):
    kit_sign(3, -59, 19 + i, wall("west"), "speed", material=m, note="compare speed")
sign(3, -59, 27, wall("west"), [("Take off", "black", True, STRIP), ("armor", "black", True), "deletes worn", "armor"])
sign(3, -59, 28, wall("west"), [("Empty bag", "black", True, "clear @s"), "clears your", "whole", "inventory"])
sign(3, -59, 29, wall("west"), [("No effects", "black", True, "effect clear @s"), "clears potion", "effects (gems", "stay on)"])
sign(3, -59, 30, wall("west"), [("Survival", "dark_blue", True, "gamemode survival @s"), "click to", "switch"])
sign(3, -59, 31, wall("west"), [("Creative", "dark_blue", True, "gamemode creative @s"), "click to", "switch"])
sign(3, -60, 27, wall("west"), [("Totem of", "dark_aqua", True, "give @s bubbas_totems_artifacts:totem_of_warding"),
                                 ("Warding", "dark_aqua", True), "held: scares", "monsters away"])
info(-3, -60, 18, stand(8), "Wardrobe", "dark_purple", "oak: perfect", TWIN_KEY_1, TWIN_KEY_2)
info(3, -60, 18, stand(8), "Wardrobe", "dark_purple", "right: helpful", "left: harmful,", "materials,tools")
lights(-1, 19, 1, 31, step=6)
c("setblock 0 -56 25 minecraft:light[level=15]")

# ---------------------------------------------------------------- armor stands
begin("grounds_stands", "Armor stands south of avenue B, x -4..4, z 37: showcase sets to take piece by piece.")
STANDS = [(-4, "speed", "netherite", "netherite_sword", "Speed"), (-2, "fire_resistance", "diamond", "diamond_sword", "Fire Resistance"),
          (0, "water_breathing", "iron", "iron_sword", "Water Breathing"), (2, "slow_falling", "golden", "golden_sword", "Slow Falling"),
          (4, "jump_boost", "leather", "wooden_sword", "Jump Boost")]
c("fill -5 -61 36 5 -61 38 minecraft:polished_andesite")
for x, key, mat, sword, label in STANDS:
    g = GEM.format(id=eid(key))
    gems3 = f'components:{{"bubbas_infused_gems:socketed_gems":[{g},{g},{g}]}}'
    gems2 = f'components:{{"bubbas_infused_gems:socketed_gems":[{g},{g}]}}'
    eq = ",".join(f'{slot}:{{id:"minecraft:{mat}_{piece}",count:1,{gems3}}}'
                  for slot, piece in [("head", "helmet"), ("chest", "chestplate"), ("legs", "leggings"), ("feet", "boots")])
    eq += f',mainhand:{{id:"minecraft:{sword}",count:1,{gems2}}}'
    c(f'summon minecraft:armor_stand {x}.5 -60 37.5 {{Tags:["tw_stand"],ShowArms:1b,NoGravity:1b,Rotation:[180f,0f],'
      f'CustomName:"{label}",equipment:{{{eq}}}}}')
info(-6, -60, 35, stand(8), "Armor Stands", "dark_purple", "click a piece", "to take it;", "perfect gems")
for x, key, mat, sword, label in STANDS:
    words = label.split()
    sign(x, -60, 35, stand(8), [(w, "dark_green", True) for w in words] + [(mat + " set", None)])
lights(-3, 37, 3, 37, step=6)

# ---------------------------------------------------------------- arrow gallery (dispensers)
begin("grounds_gallery", "Arrow gallery x -44..-30, z 19..31: stand on the red block (-37, 22); a lever (-35, -60, 20) runs a"
      "\n# command block clock under the floor (y -62) that fires the three dispensers in turn and refills them.")
c("fill -44 -61 20 -30 -61 31 minecraft:smooth_stone")
c("setblock -37 -61 22 minecraft:red_concrete")
c("fill -40 -60 19 -34 -58 19 minecraft:stone_bricks")
DISP = [(-37, 30, "north", (-37, 31)), (-43, 22, "east", (-44, 22)), (-31, 22, "west", (-30, 22))]
for x, z, f, (bx, bz) in DISP:
    c(f"setblock {x} -60 {z} minecraft:stone_bricks")
    c(f"setblock {bx} -60 {bz} minecraft:stone_bricks")
    c(f"setblock {bx} -59 {bz} minecraft:stone_bricks")
    c(f'setblock {x} -59 {z} minecraft:dispenser[facing={f}]{{Items:[{{Slot:0b,id:"minecraft:arrow",count:64}}]}}')
c("setblock -35 -60 20 minecraft:lever[face=floor,facing=south,powered=false]")
chain = ["scoreboard players add #gallery tw_clock 1"]
for t, (x, z, f, (bx, bz)) in zip((20, 40, 60), DISP):
    chain.append(f"execute if score #gallery tw_clock matches {t} run setblock {bx} -59 {bz} minecraft:redstone_block")
    chain.append(f"execute if score #gallery tw_clock matches {t + 2} run setblock {bx} -59 {bz} minecraft:stone_bricks")
for x, z, f, _ in DISP:
    chain.append(f"execute if score #gallery tw_clock matches 62.. run item replace block {x} -59 {z} container.0 with minecraft:arrow 64")
chain.append("execute if score #gallery tw_clock matches 62.. run scoreboard players set #gallery tw_clock 0")
for i, cmd in enumerate(chain):
    cmdblock(-35 - i, -62, 20, "repeat" if i == 0 else "chain", "west", cmd, auto=i > 0)
sign(-36, -59, 20, wall("south"), [("Lever", "dark_red", True), "turns arrows", "on and off:", "1 a second"])
sign(-38, -59, 20, wall("south"), [("Survival", "dark_blue", True, "gamemode survival @s"), "click to take", "damage here"])
sign(-39, -59, 20, wall("south"), [("Creative", "dark_blue", True, "gamemode creative @s"), "click to stop", "taking damage"])
info(-37, -59, 18, wall("north"), "Arrow Gallery", "dark_red", "stand on red,", "flip the lever", "behind you")
kit_pair(-36, -59, 18, wall("north"), wall("north", "birch"), "ender", note="blinks you")
kit_pair(-38, -59, 18, wall("north"), wall("north", "birch"), "breeze", note="deflects", dx=-1)
kit_sign(-34, -59, 18, wall("north"), "turtle_master", note="takes hits")
kit_sign(-40, -59, 18, wall("north"), "resistance", note="takes hits")
lights(-43, 20, -31, 31, step=6)

# ---------------------------------------------------------------- live range (real archers)
begin("grounds_range", "Live range x -46..-32, z 37..47: walls 5 high, door in the west wall at z 38 (out of the archers' sight from"
      "\n# the avenue). Skeletons and a pillager sit in boats on a ledge (z 45..46); they only shoot survival players.")
c("fill -46 -60 37 -32 -56 47 minecraft:stone_bricks outline")
c("fill -45 -60 38 -33 -56 46 minecraft:air")
c("fill -46 -61 37 -32 -61 47 minecraft:smooth_stone")
c("fill -46 -60 38 -46 -59 38 minecraft:air")
c("fill -45 -60 45 -33 -58 46 minecraft:stone_bricks")
c("setblock -39 -61 40 minecraft:red_concrete")
TURRETS = [(-42.5, "skeleton", "bow"), (-39.5, "pillager", "crossbow"), (-36.5, "skeleton", "bow")]


def turret_cmd(x, mob, weapon):
    return (f'summon minecraft:oak_boat {x} -57 45.5 {{Tags:["tw_turret"],Rotation:[180f,0f],Passengers:[{{id:"minecraft:{mob}",'
            f'Tags:["tw_turret"],PersistenceRequired:1b,equipment:{{mainhand:{{id:"minecraft:{weapon}",count:1}}}}}}]}}')


for t in TURRETS:
    c(turret_cmd(*t))
sign(-44, -59, 38, wall("south"), [("Live Range", "dark_red", True), "archers shoot", "survival", "players only"])
sign(-43, -59, 38, wall("south"), [("Turrets on", "dark_red", True, "kill @e[tag=tw_turret]"), ("re-seats the", None, False, turret_cmd(*TURRETS[0])),
                                   ("archers", None, False, turret_cmd(*TURRETS[1])), ("", None, False, turret_cmd(*TURRETS[2]))])
sign(-42, -59, 38, wall("south"), [("Turrets off", "dark_red", True, "kill @e[tag=tw_turret]"),
                                   ("removes them", None, False, "kill @e[type=minecraft:arrow,x=-46,y=-61,z=37,dx=14,dy=6,dz=10]"),
                                   ("and arrows", None, False, "kill @e[type=minecraft:item,x=-46,y=-61,z=37,dx=14,dy=6,dz=10]")])
kit_pair(-40, -59, 38, wall("south"), wall("south", "birch"), "ender", note="blinks you")
kit_pair(-38, -59, 38, wall("south"), wall("south", "birch"), "breeze", note="deflects")
sign(-36, -59, 38, wall("south"), [("Survival", "dark_blue", True, "gamemode survival @s"), "click to take", "damage here"])
sign(-35, -59, 38, wall("south"), [("Creative", "dark_blue", True, "gamemode creative @s"), "click to stop", "taking damage"])
info(-45, -60, 35, stand(8), "Live Range", "dark_red", "door on the", "west side;", "real archers")
lights(-44, 39, -34, 44, y=-57, step=5)

# ---------------------------------------------------------------- drop pads and jump steps
begin("grounds_drops", "Drop pads x -22..-10, z 19..31: stone brick pillars at z 23 (ladder on the north side), each top running"
      "\n# 2 blocks south; step off the south end onto the landing (z 26..31). Jump steps x -13..-7, z 40.")
for x, h in zip((-22, -19, -16, -13, -10), (4, 8, 12, 16, 24)):
    top = -61 + h
    c(f"fill {x} -60 23 {x} {top} 23 minecraft:stone_bricks")
    c(f"fill {x} {top} 24 {x} {top} 25 minecraft:stone_bricks")
    c(f"fill {x} -60 22 {x} {top} 22 minecraft:ladder[facing=north]")
    dmg = h - 3
    hearts = f"{dmg / 2:g}"
    if h == 24:
        sign(x + 1, -60, 21, stand(8), [(f"Drop {h}", "dark_red", True), "step off: 21", "damage, kills", "at full health"])
    else:
        sign(x + 1, -60, 21, stand(8), [(f"Drop {h}", "dark_aqua", True), f"step off: {dmg}", "damage", f"({hearts} hearts)"])
    c(f"setblock {x} {top + 3} 24 minecraft:light[level=15]")
for i, k in enumerate(["wind_charged", "oozing", "slow_falling", "jump_boost", "instant_health"]):
    kit_sign(-23 + i, -60, 19, stand(8), k, note={"wind_charged": "bounces you", "oozing": "slime landing",
                                                  "instant_health": "heals falls"}.get(k, "click to wear"))
info(-17, -60, 19, stand(8), "Drop Pads", "dark_aqua", "climb a ladder,", "step off the", "south end")
sign(-16, -60, 19, stand(8), [("Heal me", "dark_blue", True, "effect give @s minecraft:instant_health 1 5 true"), "click for", "full health"])
mode_signs(-15, -60, 19, stand(8))
lights(-22, 20, -10, 31, step=6)
# jump steps
for x, h in zip((-13, -11, -9, -7), (1, 2, 3, 4)):
    c(f"fill {x} -60 40 {x} {-61 + h} 40 minecraft:polished_andesite")
    sign(x, -60, 39, stand(8), [(f"{h} high", "dark_aqua", True), "vanilla jumps 1" if h == 1 else "Jump Boost?"])
kit_sign(-12, -60, 36, stand(8), "jump_boost", note="jump higher")
info(-10, -60, 36, stand(8), "Jump Steps", "dark_aqua", "1 to 4 blocks;", "how high can", "you jump?")
lights(-13, 38, -7, 38, step=6)

# ---------------------------------------------------------------- cobweb field
begin("grounds_webs", "Cobweb field x -23..-17, z 38..46: a cobweb lane (x -23..-21, 2 high) beside an open lane (x -19..-17).")
c("fill -23 -60 38 -21 -59 46 minecraft:cobweb")
c("fill -20 -60 38 -20 -60 46 minecraft:stone_brick_wall")
info(-22, -60, 36, stand(8), "Cobweb Field", "dark_green", "walk the web", "lane, then the", "open one")
kit_pair(-21, -60, 36, stand(8), stand(8, "birch"), "weaving", note="faster webs", dx=-2)
sign(-20, -60, 36, stand(8), [("Reset webs", "dark_green", True, "fill -23 -60 38 -21 -59 46 minecraft:cobweb replace minecraft:air"),
                              "puts every web", "back"])
info(-18, -60, 36, stand(8), "Open Lane", "dark_green", "same length,", "no webs")
sign(-19, -60, 36, stand(8), [("Shears", "dark_green", True, "give @s minecraft:shears"), "cut webs", "quickly"])
lights(-22, 38, -16, 46, step=6)

# ---------------------------------------------------------------- pool and sky column
begin("grounds_pool", "Deep pool x 8..18, z 20..30: stone brick tank, water 7 deep (y -60..-54), windows in the north wall, stairs up"
      "\n# the west side (x 7). Sky column x 21..25, z 20..25: a 3x3 water pit and a 40-block height pole.")
c("fill 8 -61 20 18 -54 30 minecraft:stone_bricks")
c("fill 9 -60 21 17 -54 29 minecraft:water")
c("fill 10 -59 20 16 -56 20 minecraft:glass")
c("fill 10 -60 28 12 -60 28 minecraft:deepslate")
for k in range(7):
    z = 22 + k
    if k:
        c(f"fill 7 -60 {z} 7 {-61 + k} {z} minecraft:stone_bricks")
    c(f"setblock 7 {-60 + k} {z} minecraft:stone_brick_stairs[facing=south]")
info(5, -60, 19, stand(8), "Deep Pool", "dark_aqua", "7 deep; stairs", "on the west", "side")
kit_pair(6, -60, 19, stand(8), stand(8, "birch"), "water_breathing", note="air lasts")
kit_sign(8, -60, 19, stand(8), "dolphins_grace", note="swim faster")
kit_pair(9, -60, 19, stand(8), stand(8, "birch"), "conduit_power", note="mine the floor")
info(11, -60, 19, stand(8), "Pool Floor", "dark_aqua", "deepslate at", "the bottom to", "mine in water")
sign(12, -60, 19, stand(8), [("Pickaxe", "dark_aqua", True, "give @s minecraft:netherite_pickaxe"), "a plain", "netherite pick", "for the floor"])
for x in (9, 15):
    for z in (22, 28):
        c(f"setblock {x} -50 {z} minecraft:light[level=15]")
# sky column
c("fill 22 -62 22 24 -61 24 minecraft:water")
c("fill 25 -60 23 25 -21 23 minecraft:white_concrete")
for h in range(5, 41, 5):
    c(f"setblock 25 {-61 + h} 23 minecraft:red_concrete")
for h in (10, 20, 30, 40):
    info(25, -61 + h, 22, wall("north"), f"{h} up", "dark_red", "red block every", "5 blocks")
info(23, -60, 20, stand(8), "Sky Column", "dark_aqua", "stand in the", "water and", "float up")
kit_sign(22, -60, 20, stand(8), "levitation", note="you will rise!")
sign(24, -60, 20, stand(8), [("Take off", "black", True, STRIP), ("armor", "black", True), "to drop back", "into the water"])
for h in (0, 10, 20, 30):
    c(f"setblock 21 {-56 + h} 21 minecraft:light[level=15]")

# ---------------------------------------------------------------- mob pen
begin("grounds_pen", "Mob pen x 30..44, z 19..31: glass box 4 high with a glass roof, oak door in the west wall at z 25; spawn"
      "\n# signs at z 18 summon one mob each into it; Clear pen removes everything inside.")
c("fill 30 -60 19 44 -56 31 minecraft:glass outline")
c("fill 31 -60 20 43 -57 30 minecraft:air")
c("fill 30 -61 19 44 -61 31 minecraft:coarse_dirt")
c("setblock 30 -60 25 minecraft:oak_door[facing=east,half=lower,hinge=left,open=false] strict")
c("setblock 30 -59 25 minecraft:oak_door[facing=east,half=upper,hinge=left,open=false] strict")
MOBS = [("Pig", "pig", 34), ("Cow", "cow", 35), ("Zombie", "zombie", 36), ("Skeleton", "skeleton", 37),
        ("Spider", "spider", 38), ("Wither", "wither_skeleton", 39), ("Husk", "husk", 40), ("Slime", "slime", 41)]
info(31, -60, 18, stand(8), "Mob Pen", "dark_red", "click to spawn", "one; fight", "inside")
for label, mob, x in MOBS:
    cmd = f"summon minecraft:{mob} {x}.5 -60 27.5" + (" {Size:1}" if mob == "slime" else "")
    if mob == "wither_skeleton":
        sign(x - 2, -60, 18, stand(8), [("Wither", "dark_red", True, cmd), ("Skeleton", "dark_red", True), "click to spawn"])
    else:
        sign(x - 2, -60, 18, stand(8), [(label, "dark_red", True, cmd), "click to spawn"])
sign(40, -60, 18, stand(8), [("Clear pen", "black", True, "kill @e[type=!minecraft:player,x=31,y=-60,z=20,dx=12,dy=3,dz=10]"),
                             "removes every", "mob and item", "inside"])
kit_sign(41, -60, 18, stand(8), "invisibility", note="sneak up")
kit_sign(45, -60, 18, stand(8, "birch"), "invisibility", note="sneak up", infusion=5)
weapon_sign(42, -60, 18, stand(8), "oozing")
weapon_sign(43, -60, 18, stand(8), "weaving")
weapon_sign(44, -60, 18, stand(8), "wind_charged", item="mace", label="mace, 2 gems")
lights(32, 21, 42, 30, y=-57, step=5)
lights(31, 18, 43, 18, y=-56, step=6)

# ---------------------------------------------------------------- target dummies
begin("grounds_dummies", "Target dummies x 8..24, z 36..46: five NoAI mobs in a fence pen (z 40..45); their health shows on the"
      "\n# action bar while a player is in the area (repeating command block under the floor at y -62).")
c("fill 8 -60 40 24 -60 45 minecraft:oak_fence outline")
c("setblock 16 -60 40 minecraft:oak_fence_gate[facing=north]")
DUMMIES = [("zombie", "Zombie", 10, "dark_green"), ("skeleton", "Skeleton", 13, "gray"),
           ("wither_skeleton", "Wither Skeleton", 16, "dark_gray"), ("spider", "Spider", 19, "dark_red"), ("pig", "Pig", 22, "light_purple")]


def dummy_cmd(mob, label, x):
    return (f'summon minecraft:{mob} {x}.5 -60 43.5 {{Tags:["tw_dummy","tw_d_{mob}"],NoAI:1b,Silent:1b,PersistenceRequired:1b,'
            f'CustomName:"{label}",Rotation:[180f,0f]}}')


for mob, label, x, _ in DUMMIES:
    c(dummy_cmd(mob, label, x))
BOX = "x=7,y=-61,z=35,dx=18,dy=8,dz=11"
bar = ",".join(f'{{text:"{label.split()[0]} ",color:"gray"}},{{score:{{name:"@e[tag=tw_d_{mob},limit=1]",objective:"tw_hp"}},color:"{col}"}},{{text:"   "}}'
               for mob, label, x, col in DUMMIES)
cmdblock(9, -62, 37, "repeat", "east",
         f"execute if entity @a[{BOX}] as @e[tag=tw_dummy] store result score @s tw_hp run data get entity @s Health", True)
cmdblock(10, -62, 37, "chain", "east", f"title @a[{BOX}] actionbar [{bar}]", True)
reset = ["kill @e[tag=tw_dummy]", "kill @e[type=minecraft:item,x=8,y=-61,z=40,dx=16,dy=6,dz=5]"]
reset += [dummy_cmd(mob, label, x) for mob, label, x, _ in DUMMIES]
reset.append("setblock 9 -61 39 minecraft:grass_block")
for i, cmd in enumerate(reset):
    cmdblock(9 + i, -62, 39, "impulse" if i == 0 else "chain", "east", cmd, auto=i > 0)
info(8, -60, 37, stand(8), "Dummies", "dark_red", "hit them; their", "health shows", "on screen")
for i, (k, item, label) in enumerate([("poison", "netherite_sword", None), ("wither", "netherite_sword", None),
                                      ("instant_damage", "netherite_sword", None), ("instant_health", "netherite_sword", None),
                                      ("slowness", "netherite_sword", None), ("weakness", "netherite_sword", None),
                                      ("levitation", "netherite_sword", None), ("glowing", "netherite_sword", None),
                                      ("nausea", "netherite_sword", None)]):
    weapon_sign(9 + i, -60, 37, stand(8), k, item=item)
weapon_sign(18, -60, 37, stand(8), "poison", item="bow", label="bow + arrows", extra="give @s minecraft:arrow 64")
sign(20, -60, 37, stand(8), [("Heal dummies", "black", True, "execute as @e[tag=tw_dummy] run data modify entity @s Health set value 100f"),
                             ("and clear", None, False, "effect clear @e[tag=tw_dummy]"), "their effects"])
sign(21, -60, 37, stand(8), [("Reset", "black", True, "setblock 9 -61 39 minecraft:redstone_block"), ("dummies", "black", True),
                             "brings back", "killed ones"])
info(23, -60, 37, stand(8), "Undead", "dark_red", "Healing hurts", "them, Harming", "heals them")
lights(10, 38, 22, 44, step=6)

# ---------------------------------------------------------------- villagers
begin("grounds_villagers", "Villagers x 30..44, z 36..46: three NoAI, invulnerable level 5 villagers at their work stations with"
      "\n# fixed big-emerald trades, for the Hero of the Village gem.")
c("fill 30 -61 37 44 -61 45 minecraft:cobblestone")
VILL = [("armorer", 33, "blast_furnace[facing=north]",
         '{buy:{id:"minecraft:emerald",count:20},sell:{id:"minecraft:diamond_chestplate",count:1},maxUses:9999,rewardExp:0b,xp:0},'
         '{buy:{id:"minecraft:emerald",count:12},sell:{id:"minecraft:iron_helmet",count:1},maxUses:9999,rewardExp:0b,xp:0}'),
        ("librarian", 37, "lectern[facing=north]",
         '{buy:{id:"minecraft:emerald",count:32},sell:{id:"minecraft:enchanted_book",count:1,components:{"minecraft:stored_enchantments":{"minecraft:mending":1}}},maxUses:9999,rewardExp:0b,xp:0},'
         '{buy:{id:"minecraft:emerald",count:16},sell:{id:"minecraft:name_tag",count:1},maxUses:9999,rewardExp:0b,xp:0}'),
        ("weaponsmith", 41, "grindstone[face=floor,facing=north]",
         '{buy:{id:"minecraft:emerald",count:24},sell:{id:"minecraft:diamond_sword",count:1},maxUses:9999,rewardExp:0b,xp:0},'
         '{buy:{id:"minecraft:emerald",count:10},sell:{id:"minecraft:iron_axe",count:1},maxUses:9999,rewardExp:0b,xp:0}')]
for prof, x, station, offers in VILL:
    c(f"setblock {x} -60 43 minecraft:{station}")
    c(f'summon minecraft:villager {x}.5 -60 41.5 {{Tags:["tw_villager"],NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,'
      f'Rotation:[180f,0f],VillagerData:{{profession:"minecraft:{prof}",level:5,type:"minecraft:plains"}},Offers:{{Recipes:[{offers}]}}}}')
c('setblock 31 -60 38 minecraft:chest[facing=north]{CustomName:"Emeralds",Items:[{Slot:0b,id:"minecraft:emerald",count:64},'
  '{Slot:1b,id:"minecraft:emerald",count:64},{Slot:2b,id:"minecraft:emerald",count:64},{Slot:3b,id:"minecraft:emerald",count:64},'
  '{Slot:4b,id:"minecraft:emerald_block",count:64}]}')
info(32, -60, 37, stand(8), "Villagers", "dark_green", "trade, then", "wear Hero and", "trade again")
kit_sign(34, -60, 37, stand(8), "hero_of_the_village")
info(36, -60, 37, stand(8), "Prices", "dark_green", "20 to 32", "emeralds, so a", "cut shows")
lights(31, 38, 43, 44, step=6)

# ---------------------------------------------------------------- sculk corridor
begin("grounds_sculk", "Sculk corridor x -42..-38, z 53..62: walk the 3-wide hall (x -41..-39) past sculk sensors (west wall) and"
      "\n# calibrated sensors (east wall) with lamps on top; two zombie sleepers lie at the far end (z 61).")
c("fill -42 -61 52 -38 -61 63 minecraft:deepslate_tiles")
c("fill -42 -60 53 -42 -58 62 minecraft:deepslate_tiles")
c("fill -38 -60 53 -38 -58 62 minecraft:deepslate_tiles")
c("fill -41 -60 63 -39 -58 63 minecraft:deepslate_tiles")
for z in (54, 56, 58, 60):
    c(f"setblock -42 -60 {z} minecraft:sculk_sensor")
    c(f"setblock -42 -59 {z} minecraft:redstone_lamp")
    c(f"setblock -38 -60 {z} minecraft:calibrated_sculk_sensor[facing=north]")
    c(f"setblock -38 -59 {z} minecraft:redstone_lamp")
SLEEPERS = ["setblock -41 -60 61 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]",
            "setblock -39 -60 61 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]"]
for s in SLEEPERS:
    c(s)
info(-43, -60, 52, stand(8), "Sculk Hall", "dark_aqua", "walk in: lamps", "light and the", "sleepers wake")
kit_sign(-44, -60, 52, stand(8), "silence", note="walk silently")
sign(-37, -60, 52, stand(8), [("Reset", "dark_aqua", True, "kill @e[type=bubbas_reworked_mobs:woken_zombie,x=-40,y=-60,z=58,distance=..24]"),
                              ("sleepers", "dark_aqua", True, SLEEPERS[0]),
                              ("puts both back", None, False, SLEEPERS[1]), "to sleep"])
lights(-40, 53, -40, 59, step=6)

# ---------------------------------------------------------------- lava moat
begin("grounds_lava", "Lava moat x -28..-20, z 54..61: lava 2 deep (y -62..-61) in a deepslate brick rim, stairs out at the south"
      "\n# end (x -24). Lava sources in a closed pit never flow; nothing flammable is within reach (warped signs).")
c("fill -28 -61 54 -20 -61 61 minecraft:deepslate_bricks")
c("fill -27 -62 55 -21 -61 60 minecraft:lava")
c("setblock -24 -62 59 minecraft:deepslate_brick_stairs[facing=south]")
c("setblock -24 -61 60 minecraft:deepslate_brick_stairs[facing=south]")
info(-26, -60, 53, stand(8, "warped"), "Lava Moat", "dark_red", "walk in from", "here; stairs", "out at the end")
kit_pair(-25, -60, 53, stand(8, "warped"), stand(8, "warped"), "fire_resistance", note="less burning")
sign(-23, -60, 53, stand(8, "warped"), [("Get me out", "dark_red", True, "tp @s -23.5 -60 62.5 180 0"),
                                         ("puts you past", None, False, "effect give @s minecraft:fire_resistance 10 0 true"), "the moat with", "fire protection"])
mode_signs(-22, -60, 53, stand(8, "warped"))
lights(-27, 53, -21, 62, y=-55, step=6)

# ---------------------------------------------------------------- dark room
begin("grounds_dark", "Dark room x -12..-4, z 53..61: sealed blackstone box, no light inside; signs teleport in and out.")
c("fill -12 -61 53 -4 -56 61 minecraft:polished_blackstone_bricks outline")
c("setblock -9 -59 61 minecraft:red_concrete")
c("setblock -8 -59 61 minecraft:lime_concrete")
c("setblock -7 -59 61 minecraft:blue_concrete")
c('summon minecraft:zombie -7.5 -60 59.5 {Tags:["tw_dark"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f]}')
sign(-8, -59, 54, wall("south"), [("Way out", "yellow", True, "tp @s -7.5 -60 51.5 180 0"), ("click me", "yellow"), ("(glowing sign)", "gold")], glow=True)
sign(-9, -60, 52, stand(8), [("Dark Room", "black", True, "tp @s -7.5 -60 56.5 180 0"), "click to go in;", "pitch dark,", "a zombie waits"])
kit_pair(-8, -60, 52, stand(8), stand(8, "birch"), "night_vision", note="see inside", dx=-2)
kit_sign(-7, -60, 52, stand(8), "darkness")
kit_sign(-6, -60, 52, stand(8), "blindness")
sign(-5, -60, 52, stand(8), [("No effects", "black", True, "effect clear @s"), "clears potion", "effects"])
lights(-11, 51, -5, 51, y=-57, step=6)

# ---------------------------------------------------------------- food bench
begin("grounds_food", "Food bench x 0..8, z 52..57: hunger buttons (Hunger effect for a moment), a food chest, gem sets.")
c("fill 0 -61 52 8 -61 57 minecraft:spruce_planks")
c('setblock 4 -60 55 minecraft:chest[facing=north]{CustomName:"Food",Items:[{Slot:0b,id:"minecraft:cooked_beef",count:64},'
  '{Slot:1b,id:"minecraft:bread",count:64},{Slot:2b,id:"minecraft:golden_carrot",count:64},{Slot:3b,id:"minecraft:apple",count:64},'
  '{Slot:4b,id:"minecraft:rotten_flesh",count:64},{Slot:5b,id:"minecraft:milk_bucket",count:1}]}')
c("setblock 3 -60 55 minecraft:barrel[facing=up]")
c("setblock 5 -60 55 minecraft:smoker[facing=north]")
info(1, -60, 53, stand(8), "Food Bench", "black", "hunger only", "counts in", "survival")
sign(2, -60, 53, stand(8), [("Hungry", "dark_red", True, "effect give @s minecraft:hunger 2 200 true"), "drops about", "half your food"])
sign(3, -60, 53, stand(8), [("Starving", "dark_red", True, "effect give @s minecraft:hunger 5 200 true"), "empties your", "food bar"])
sign(4, -60, 53, stand(8), [("Fill up", "dark_green", True, "effect give @s minecraft:saturation 1 20 true"), "full food and", "saturation"])
kit_pair(5, -60, 53, stand(8), stand(8, "birch"), "saturation", note="slow refill", dx=-5)
kit_sign(6, -60, 53, stand(8), "hunger", note="drains food")
mode_signs(7, -60, 53, stand(8))
lights(1, 54, 7, 54, step=6)

# ---------------------------------------------------------------- mining wall
begin("grounds_mining", "Mining wall x 13..21, z 56: stone, deepslate and obsidian, 3 high, for Haste and Mining Fatigue.")
c("fill 9 -61 52 25 -61 57 minecraft:gravel")
c("fill 10 -60 56 12 -58 56 minecraft:dirt")
c("fill 22 -60 56 24 -58 56 minecraft:oak_log")
c("fill 13 -60 56 15 -58 56 minecraft:stone")
c("fill 16 -60 56 18 -58 56 minecraft:deepslate")
c("fill 19 -60 56 21 -58 56 minecraft:obsidian")
info(13, -60, 53, stand(8), "Mining Wall", "black", "dirt to logs;", "mine in", "survival")
EFF = '{"minecraft:enchantments":{"minecraft:efficiency":5}}'
tools = []
for row, mat in enumerate(["netherite", "diamond", "iron"]):
    for col, kind in enumerate(["pickaxe", "shovel", "axe"]):
        tools.append(f'{{Slot:{row * 9 + col}b,id:"minecraft:{mat}_{kind}",count:1}}')
        tools.append(f'{{Slot:{row * 9 + col + 4}b,id:"minecraft:{mat}_{kind}",count:1,components:{EFF}}}')
c(f'setblock 16 -60 53 minecraft:chest[facing=north]{{CustomName:"Plain | Efficiency V",Items:[{",".join(tools)}]}}')
kit_sign(14, -60, 53, stand(8), "haste", note="mine faster")
kit_sign(15, -60, 53, stand(8), "mining_fatigue", note="mine slower")
sign(17, -60, 53, stand(8), [("Tools", "black", True, "give @s minecraft:netherite_pickaxe"), "a netherite", "pick; chest:", "Efficiency V"])
sign(18, -60, 53, stand(8), [("Rebuild", "black", True, "fill 13 -60 56 15 -58 56 minecraft:stone"),
                             ("the wall", "black", True, "fill 16 -60 56 18 -58 56 minecraft:deepslate"),
                             ("puts every", None, False, "fill 19 -60 56 21 -58 56 minecraft:obsidian"), ("block back", None, False, "fill 10 -60 56 12 -58 56 minecraft:dirt")])
sign(21, -60, 53, stand(8), [("Rebuild", "black", True, "fill 22 -60 56 24 -58 56 minecraft:oak_log"), ("the logs", "black", True)])
mode_signs(19, -60, 53, stand(8))
lights(10, 54, 24, 54, step=7)

# ---------------------------------------------------------------- boss arena
begin("grounds_arena", "Boss arena x -16..16, z 66..92 (walls 5 high, fence gate at (-2, 66) from the lane x -3..-1): one summon"
      "\n# sign per gem-combo boss on the north wall's outside (z 65, x 1..12), counter kits on the same wall (x -15..-7).")
c("fill -16 -60 66 16 -56 92 minecraft:stone_bricks outline")
c("fill -15 -60 67 15 -56 91 minecraft:air")
c("fill -16 -61 66 16 -61 92 minecraft:coarse_dirt")
c("setblock -2 -60 66 minecraft:oak_fence_gate[facing=north]")
c("setblock -2 -59 66 minecraft:air")
c("setblock 0 -61 80 minecraft:gold_block")
c("setblock 10 -61 86 minecraft:gold_block")
path(-3, 52, -1, 65)
c("fill -15 -61 64 15 -61 65 minecraft:dirt_path replace minecraft:grass_block")


def gemmed(item, effects):
    gems = ",".join(GEM.format(id=eid(e)) for e in effects)
    return f'{{id:"minecraft:{item}",count:1,components:{{"bubbas_infused_gems:socketed_gems":[{gems}]}}}}'


def armor(mat, per_piece):
    """per_piece: the 3 effects socketed in every piece."""
    return ",".join(f"{slot}:{gemmed(f'{mat}_{piece}', per_piece)}" for slot, piece in
                    [("head", "helmet"), ("chest", "chestplate"), ("legs", "leggings"), ("feet", "boots")])


def boss_cmd(mob, name, hp, equipment, x=0.5, z=80.5, extra=""):
    return (f'summon minecraft:{mob} {x} -60 {z} {{Tags:["tw_boss"],CustomName:{{text:"{name}",color:"red"}},CustomNameVisible:1b,'
            f'PersistenceRequired:1b,Health:{hp}f,attributes:[{{id:"minecraft:max_health",base:{hp}d}}],'
            f'drop_chances:{{head:0f,chest:0f,legs:0f,feet:0f,mainhand:0f,offhand:0f}},equipment:{{{equipment}}}{extra}}}')


STALKER_SLEEPERS = ["setblock 8 -60 89 bubbas_reworked_mobs:skeleton_sleeper[facing=north,pose=lying]",
                    "setblock 12 -60 89 bubbas_reworked_mobs:skeleton_sleeper[facing=north,pose=lying]"]
BOSSES = [
    ("Webmaster", "webs you, walks", "through webs", boss_cmd("zombie", "Webmaster", 120,
        armor("netherite", ["weaving"] * 3) + ",mainhand:" + gemmed("netherite_sword", ["weaving", "weaving"]))),
    ("Puppeteer", "lifts you and", "keeps you up", boss_cmd("vindicator", "Puppeteer", 100,
        armor("iron", ["resistance"] * 3) + ",mainhand:" + gemmed("netherite_axe", ["levitation", "slow_falling"]))),
    ("Blinker", "dodges arrows:", "box it in", boss_cmd("skeleton", "Blinker", 80,
        armor("netherite", ["ender"] * 3) + ',mainhand:{id:"minecraft:bow",count:1}')),
    ("Juggernaut", "Turtle Master,", "Absorb, Regen", boss_cmd("zombie", "Juggernaut", 200,
        armor("netherite", ["turtle_master", "absorption", "regeneration"]) + ",mainhand:" + gemmed("netherite_axe", ["slowness", "slowness"]))),
    ("Gale", "Wind Burst mace", "juggles you", boss_cmd("piglin_brute", "Gale", 120,
        armor("golden", ["speed"] * 3) + ",mainhand:" + gemmed("mace", ["wind_charged", "wind_charged"]), extra=",IsImmuneToZombification:1b")),
    ("Plague", "Poison, Hunger", "and Wither", boss_cmd("wither_skeleton", "Plague", 120,
        armor("chainmail", ["resistance"] * 3) + ",mainhand:" + gemmed("netherite_sword", ["poison", "hunger"]))),
    ("Phantom", "invisible, with", "Darkness hits", boss_cmd("zombie", "Phantom", 100,
        armor("netherite", ["invisibility"] * 3) + ",mainhand:" + gemmed("netherite_sword", ["darkness", "darkness"]))),
    ("Stalker", "silent, unseen;", "by 2 sleepers", boss_cmd("husk", "Stalker", 100,
        armor("netherite", ["silence", "silence", "invisibility"]) + ',mainhand:{id:"minecraft:iron_sword",count:1}', x=10.5, z=86.5)),
]
info(0, -59, 65, wall("north"), "Boss Arena", "dark_red", "one boss at a", "time; gate at", "the left")
for i, (name, l2, l3, cmd) in enumerate(BOSSES):
    lines = [(name, "dark_red", True, "kill @e[tag=tw_boss]"), (l2, None, False, cmd), (l3, None, False)]
    if name == "Stalker":
        lines = [(name, "dark_red", True, "kill @e[tag=tw_boss]"), (l2, None, False, cmd), (l3, None, False, STALKER_SLEEPERS[0]),
                 ("", None, False, STALKER_SLEEPERS[1])]
    else:
        lines.append(("click: summon", "gray"))
    sign(1 + i, -59, 65, wall("north"), lines)
for s_ in STALKER_SLEEPERS:
    c(s_)
sign(10, -59, 65, wall("north"), [("Clear arena", "black", True, "kill @e[tag=tw_boss]"),
                                  ("removes the", None, False, "kill @e[type=!minecraft:player,x=-15,y=-60,z=67,dx=30,dy=4,dz=24]"),
                                  "boss and all", "drops inside"])
sign(11, -59, 65, wall("north"), [("Survival", "dark_blue", True, "gamemode survival @s"), "bosses only", "fight back in", "survival"])
sign(12, -59, 65, wall("north"), [("Creative", "dark_blue", True, "gamemode creative @s"), "click to stop", "taking damage"])


def gem_give(item, effects):
    gems = ",".join(GEM.format(id=eid(e)) for e in effects)
    return f"give @s minecraft:{item}[bubbas_infused_gems:socketed_gems=[{gems}]]"


COUNTERS = [
    ("Web Walker", "vs Webmaster", [STRIP, "gems kit weaving", "give @s minecraft:shears"], "Weaving set,", "sword, shears"),
    ("Crowd Ctrl", "vs Puppeteer", [gem_give("netherite_sword", ["levitation", "slow_falling"]),
                                    gem_give("bow", ["levitation", "slowness"]), "give @s minecraft:arrow 64"], "Lev+Slow Fall", "sword, bow"),
    ("Deflector", "vs Blinker", [STRIP, "gems kit breeze", "give @s minecraft:cobblestone 64", SOUND], "perfect Breeze", "+ blocks to box"),
    ("Siege", "vs Juggernaut", [STRIP, "gems kit strength", gem_give("netherite_axe", ["wither", "instant_damage"])], "Strength set,", "Wither axe"),
    ("Anchor", "vs Gale", [STRIP, "gems kit wind_charged", gem_give("netherite_sword", ["slow_falling", "slow_falling"])], "Wind Burst set", "soft landings"),
    ("Attrition", "vs Plague", [STRIP, "gems kit regeneration", "give @s minecraft:milk_bucket", "give @s minecraft:golden_apple 8"],
     "Regen set, milk", "golden apples"),
    ("Lantern", "vs Phantom", [STRIP, "gems kit night_vision", gem_give("netherite_sword", ["glowing", "glowing"])], "Night Vision,", "Glowing sword"),
    ("Hunter", "vs Stalker", [STRIP, "gems kit silence", gem_give("netherite_sword", ["glowing", "slowness"])], "Silence set,", "Glowing sword"),
]
info(-6, -59, 65, wall("north"), "Counter Kits", "dark_green", "<- one per", "boss; replaces", "worn armor")
for i, (title, vs, cmds, l3, l4) in enumerate(COUNTERS):
    texts = [(title, "dark_green", True), (vs, "dark_red"), (l3, None), (l4, None)]
    lines = [t + ((False, cm) if len(t) == 2 else (cm,)) if cm else t for t, cm in zip(texts, cmds + [None] * (4 - len(cmds)))]
    sign(-15 + i, -59, 65, wall("north"), lines)
lights(-13, 69, 13, 90, y=-56, step=7)
lights(-14, 63, 14, 63, y=-56, step=7)

# ---------------------------------------------------------------- beacons and warding (Reworked Beacons, Reworked Mobs)
def gem_item(key, infusion, slot, count=1):
    return (f'{{Slot:{slot}b,id:"bubbas_infused_gems:gem",count:{count},components:{{"bubbas_infused_gems:gem":'
            f'{{effect:"{eid(key)}",infusion:{infusion},tier:3,main:1.0f}}}}}}')


def pyramid(cx, y0, cz, layers, block="minecraft:iron_block", down=False):
    """A beacon pyramid of `layers` with its top layer (3x3) at y0 (down=False: layers go down from y0)."""
    for i in range(layers):
        r = i + 1
        y = y0 - i if not down else y0 + i
        c(f"fill {cx - r} {y} {cz - r} {cx + r} {y} {cz + r} {block}")


begin("grounds_beacons", "Beacon yard x 20..37, z 64..86: sky beacons on 1, 2, 3 and 4-layer pyramids, each with a sign and a chest"
      "\n# of finished gems (perfect and Gem Power V). Poison trap beacon x -41..-39, z 87..89 (loaded; its 20-block area"
      "\n# x -60..-20, z 68..108 is poisonous). Bell lane x 31..46, z 54. Light-calm rings around (43, 80) lit and (43, 90) dark.")
path(27, 52, 28, 68)
GEMS = ["speed", "regeneration", "jump_boost", "night_vision", "strength", "resistance", "fire_resistance", "slow_falling", "haste"]
chest_items = [gem_item(k, 6, i) for i, k in enumerate(GEMS)] + [gem_item(k, 5, 9 + i) for i, k in enumerate(GEMS)]
chest_items += ['{Slot:18b,id:"minecraft:amethyst_shard",count:16}', '{Slot:19b,id:"minecraft:quartz",count:16}',
                '{Slot:20b,id:"minecraft:diamond",count:16}', '{Slot:21b,id:"minecraft:nether_star",count:4}']
CHEST = f'{{CustomName:"Gems: perfect / Power V",Items:[{",".join(chest_items)}]}}'
YARD = [(23, 70, 1, "20"), (31, 70, 2, "30"), (23, 80, 3, "40"), (33, 81, 4, "50")]
CAP = {1: "I", 2: "III", 3: "V", 4: "VI"}
for cx, cz, layers, rng in YARD:
    for i in range(layers):
        r = layers - i
        c(f"fill {cx - r} {-60 + i} {cz - r} {cx + r} {-60 + i} {cz + r} minecraft:iron_block")
    c(f"setblock {cx} {-60 + layers} {cz} minecraft:beacon")
    zs = cz - layers - 2
    sign(cx - 1, -60, zs, stand(8), [(f"{layers}-layer", "dark_aqua", True), "sky beacon:", f"up to Power {CAP[layers]}", f"{rng} blocks out"])
    c(f"setblock {cx + 1} -60 {zs} minecraft:chest[facing=north]{CHEST}")
info(26, -60, 64, stand(8), "Beacon Yard", "dark_aqua", "put a gem in;", "the pyramid", "caps its power")
for x in (21, 29, 37):
    for z in (66, 74, 86):
        c(f"setblock {x} -55 {z} minecraft:light[level=15]")

# poison trap
POISON = '{"bubbas_reworked_beacons:item":{id:"bubbas_infused_gems:gem",count:1,components:{"bubbas_infused_gems:gem":{effect:"minecraft:poison",infusion:6,tier:3,main:1.0f}}}}'
c("fill -41 -60 87 -39 -60 89 minecraft:iron_block")
c(f"setblock -40 -59 88 minecraft:beacon{POISON}")
path(-36, 52, -35, 66)
sign(-36, -60, 67, stand(8), [("DANGER", "dark_red", True), "Poison beacon:", "past this sign", "all is poisoned"])
sign(-19, -60, 84, stand(12), [("DANGER", "dark_red", True), "Poison beacon", "zone to the", "west"])
c("fill -44 -60 79 -36 -60 83 minecraft:oak_fence outline")
c('summon minecraft:pig -42.5 -60 81.5 {Tags:["tw_trap"],NoAI:1b,Silent:1b,PersistenceRequired:1b}')
c('summon minecraft:cow -40.5 -60 81.5 {Tags:["tw_trap"],NoAI:1b,Silent:1b,PersistenceRequired:1b}')
c('summon minecraft:zombie -38.5 -60 81.5 {Tags:["tw_trap"],NoAI:1b,Silent:1b,PersistenceRequired:1b}')
info(-40, -60, 78, stand(8), "Trap Zone", "dark_red", "Poison hits all", "but the zombie", "(undead)")

# bell lane
c("setblock 31 -60 54 minecraft:bell[attachment=floor,facing=east]")
BELL_SLEEPERS = [f"setblock {x} -60 54 bubbas_reworked_mobs:zombie_sleeper[facing=west,pose=lying]" for x in (34, 40, 46)]
for b_ in BELL_SLEEPERS:
    c(b_)
info(30, -60, 52, stand(8), "Bell", "dark_aqua", "ring to wake:", "sleepers 3, 9", "and 15 away")
sign(32, -60, 52, stand(8), [("Reset", "dark_aqua", True, "kill @e[type=bubbas_reworked_mobs:woken_zombie,x=40,y=-60,z=54,distance=..24]"),
                             ("sleepers", "dark_aqua", True, BELL_SLEEPERS[0]), ("puts all 3", None, False, BELL_SLEEPERS[1]),
                             ("back", None, False, BELL_SLEEPERS[2])])
c("setblock 38 -56 52 minecraft:light[level=8]")

# light-calm rings
RINGS = {}
for name, cz, lit in (("lit", 80, True), ("dark", 90, False)):
    cmds = []
    for dx, dz, facing in ((0, -2, "north"), (0, 2, "south"), (-2, 0, "west"), (2, 0, "east")):
        cmd = f"setblock {43 + dx} -60 {cz + dz} bubbas_reworked_mobs:skeleton_sleeper[facing={facing},pose=sitting]"
        c(cmd)
        cmds.append(cmd)
        if lit:
            c(f"setblock {43 + 2 * dx // 2 + (dx > 0) - (dx < 0)} -60 {cz + dz + (dz > 0) - (dz < 0)} minecraft:torch")
    RINGS[name] = cmds
info(39, -60, 80, stand(4), "Lit Ring", "dark_aqua", "torches: light", "12+ makes them", "harder to wake")
info(39, -60, 90, stand(4), "Dark Ring", "dark_aqua", "no light: wakes", "as easily as", "ever")
sign(39, -60, 84, stand(4), [("Reset rings", "dark_aqua", True, "kill @e[type=bubbas_reworked_mobs:woken_skeleton,x=43,y=-60,z=85,distance=..16]"),
                             ("puts the dark", None, False, RINGS["dark"][0]), ("ring back", None, False, RINGS["dark"][1]),
                             ("(lit: by hand)", "gray", False, RINGS["dark"][2])])

# paired beacons room, high above the boss arena so its ward box (y 10..44, 150 blocks out) misses the ground
begin("grounds_paired", "Paired beacons room x -9..9, y 23..34, z 70..88 above the boss arena: a floor beacon (0, 24, 79) on a"
      "\n# 4-layer iron pyramid (y 20..23) and an upside-down beacon (0, 30, 79) under a 4-layer gold pyramid (y 31..34). The"
      "\n# pair's ward box is y 10..44; spawn test boxes inside it (interior y 12..14) and above it (control, y 48..50).")
c("fill -9 23 70 9 34 88 minecraft:deepslate_bricks outline")
for i in range(4):
    r = 4 - i
    c(f"fill {-r} {20 + i} {79 - r} {r} {20 + i} {79 + r} minecraft:iron_block")
c("setblock 0 24 79 minecraft:beacon")
for i in range(4):
    r = i + 1
    c(f"fill {-r} {31 + i} {79 - r} {r} {31 + i} {79 + r} minecraft:gold_block")
c("setblock 0 30 79 minecraft:beacon[upside_down=true]")
c('setblock -5 24 73 minecraft:chest[facing=south]{CustomName:"Totems of Warding",Items:[{Slot:0b,id:"bubbas_totems_artifacts:totem_of_warding",count:1},{Slot:1b,id:"bubbas_totems_artifacts:totem_of_warding",count:1},{Slot:2b,id:"bubbas_totems_artifacts:totem_of_warding",count:1}]}')
PAIR_SLEEPERS = ["setblock -6 24 85 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]",
                 "setblock -3 24 86 bubbas_reworked_mobs:skeleton_sleeper[facing=north,pose=sitting]",
                 "setblock 6 24 85 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]"]
for p_ in PAIR_SLEEPERS:
    c(p_)
c("setblock 4 24 82 minecraft:bell[attachment=floor,facing=north]")
# Light 11 at most (sleepers calm at block light 12+, which would confound the ward test).
for x, z in ((-5, 74), (5, 74), (0, 73), (-5, 84), (5, 84), (0, 86)):
    c(f"setblock {x} 26 {z} minecraft:light[level=11]")
for y0 in (11, 47):
    c(f"fill -5 {y0} 74 5 {y0 + 4} 84 minecraft:deepslate_tiles outline")
INSIDE = "x=-4,y=12,z=75,dx=8,dy=2,dz=8"
OUTSIDE = "x=-4,y=48,z=75,dx=8,dy=2,dz=8"
COUNT = [f"execute store result score #in tw_hp if entity @e[type=!minecraft:player,{INSIDE}]",
         f"execute store result score #out tw_hp if entity @e[type=!minecraft:player,{OUTSIDE}]",
         'tellraw @s [{text:"Warded box: ",color:"gold"},{score:{name:"#in",objective:"tw_hp"}},{text:" mobs. Control box: "},{score:{name:"#out",objective:"tw_hp"}},{text:" mobs."}]']
SPAWN_SIGNS = [
    ([("Spawns on", "dark_red", True, "gamerule spawn_mobs true"), "natural spawns", "in all dark;", "turn off after"]),
    ([("Spawns off", "dark_green", True, "gamerule spawn_mobs false"), "back to no", "natural spawns"]),
    ([("Count", "black", True, COUNT[0]), ("spawns", "black", True, COUNT[1]), ("in the two dark", None, False, COUNT[2]), "test boxes"]),
    ([("Clear boxes", "black", True, f"kill @e[type=!minecraft:player,{INSIDE}]"), ("empties both", None, False, f"kill @e[type=!minecraft:player,{OUTSIDE}]"),
      "test boxes"]),
]
sign(-2, 25, 71, wall("south"), [("Way down", "dark_blue", True, "tp @s 15.5 -60 60.5 180 0"), "back to the", "ground"])
info(-1, 25, 71, wall("south"), "Paired Room", "dark_aqua", "totem in the", "floor beacon:", "wards 150 out")
for i, lines in enumerate(SPAWN_SIGNS):
    sign(1 + i, 25, 71, wall("south"), lines)
info(-1, 24, 76, stand(8), "Sleepers", "dark_aqua", "4 layers + a", "totem: the bell", "wakes nobody")
# ground signs south of the mining wall
sign(13, -60, 62, stand(8), [("Paired Room", "dark_aqua", True, "tp @s 0.5 24 73.5 180 0"), "click: go up to", "the paired", "beacons"])
for i, lines in enumerate(SPAWN_SIGNS):
    sign(14 + i, -60, 62, stand(8), lines)
c("setblock 15 -56 62 minecraft:light[level=15]")

ORDER = ["grounds", "grounds_wardrobe", "grounds_stands", "grounds_gallery", "grounds_range", "grounds_drops", "grounds_webs",
         "grounds_pool", "grounds_pen", "grounds_dummies", "grounds_villagers", "grounds_sculk", "grounds_lava", "grounds_dark",
         "grounds_food", "grounds_mining", "grounds_arena", "grounds_beacons", "grounds_paired"]
assert set(ORDER) == set(FILES)
for name in ORDER:
    lines = [l for l in FILES[name] if l is not None]
    if name == "grounds":
        lines += [f"function bubba_testworld:{n}" for n in ORDER[1:]]
    (OUT / f"{name}.mcfunction").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(name, len(lines))
