# Enderman cairn preview (specs/endermen.md): design only, inert. A chorus-like spire grown out of a small natural
# hill, with end-city touches and overworld offerings. Never obsidian. Stronghold pointer for this preview: east
# (+x): the tallest end rod at the very top points east.
# The landscape: a low grassy mound with a stone outcrop and a small oak tree.
fill 8 -60 -11 16 -60 -3 minecraft:grass_block
setblock 8 -60 -11 minecraft:air
setblock 16 -60 -11 minecraft:air
setblock 8 -60 -3 minecraft:air
setblock 16 -60 -3 minecraft:air
fill 9 -59 -10 15 -59 -4 minecraft:grass_block
setblock 9 -59 -10 minecraft:air
setblock 15 -59 -4 minecraft:air
fill 10 -58 -9 14 -58 -5 minecraft:grass_block
setblock 14 -58 -9 minecraft:air
setblock 15 -60 -10 minecraft:stone
setblock 15 -59 -9 minecraft:andesite
setblock 16 -60 -9 minecraft:andesite
setblock 14 -60 -11 minecraft:cobblestone
setblock 7 -60 -6 minecraft:oak_log
setblock 7 -59 -6 minecraft:oak_log
setblock 7 -58 -6 minecraft:oak_log
fill 6 -57 -7 8 -57 -5 minecraft:oak_leaves[persistent=true]
setblock 7 -56 -6 minecraft:oak_leaves[persistent=true]
setblock 7 -57 -6 minecraft:oak_log
# Footing sunk into the hilltop: end stone bricks and end stone, a purpur slab step.
fill 11 -58 -8 13 -58 -6 minecraft:end_stone_bricks
setblock 11 -58 -8 minecraft:end_stone
setblock 13 -58 -6 minecraft:end_stone
setblock 10 -58 -7 minecraft:purpur_slab
setblock 12 -58 -9 minecraft:end_stone_brick_slab
# Trunk: purpur pillars that climb and step sideways like a chorus stalk; purpur stairs as knuckles.
setblock 12 -57 -7 minecraft:purpur_pillar
setblock 12 -56 -7 minecraft:purpur_pillar
setblock 12 -55 -7 minecraft:purpur_block
setblock 11 -55 -7 minecraft:purpur_stairs[facing=east,half=top]
setblock 11 -54 -7 minecraft:purpur_pillar
setblock 11 -53 -7 minecraft:purpur_block
setblock 11 -53 -6 minecraft:purpur_pillar[axis=z]
setblock 11 -52 -6 minecraft:purpur_pillar
setblock 11 -51 -6 minecraft:purpur_block
setblock 12 -51 -6 minecraft:purpur_stairs[facing=west,half=top]
setblock 12 -50 -6 minecraft:purpur_pillar
setblock 12 -49 -6 minecraft:purpur_block
# The tallest end rod, at the very top, points toward the stronghold (east here).
setblock 13 -49 -6 minecraft:end_rod[facing=east]
# Branch 1, low: a short arm ending in an end-rod flower.
setblock 13 -55 -7 minecraft:purpur_pillar[axis=x]
setblock 14 -55 -7 minecraft:purpur_block
setblock 14 -54 -7 minecraft:end_rod[facing=up]
# Branch 2, middle, north: angled up, an end-city window pane hanging under it.
setblock 11 -53 -8 minecraft:purpur_pillar[axis=z]
setblock 11 -53 -9 minecraft:purpur_block
setblock 11 -52 -9 minecraft:purpur_pillar
setblock 11 -51 -9 minecraft:end_rod[facing=up]
setblock 11 -54 -9 minecraft:magenta_stained_glass
# Branch 3, high, south: an end-ship banner hanging from it.
setblock 12 -51 -5 minecraft:purpur_pillar[axis=z]
setblock 12 -51 -4 minecraft:purpur_block
setblock 12 -50 -4 minecraft:end_rod[facing=up]
setblock 12 -52 -4 minecraft:magenta_banner
# Offerings at the foot (what endermen carry): flowers, mushrooms, a pumpkin, moss, clay, a melon, and rare TNT.
setblock 10 -57 -8 minecraft:poppy
setblock 14 -57 -6 minecraft:dandelion
setblock 13 -57 -5 minecraft:cornflower
setblock 10 -57 -5 minecraft:red_mushroom
setblock 14 -57 -8 minecraft:brown_mushroom
setblock 10 -58 -9 minecraft:pumpkin
setblock 15 -59 -6 minecraft:carved_pumpkin[facing=east]
setblock 9 -59 -7 minecraft:moss_block
setblock 9 -59 -6 minecraft:moss_block
setblock 9 -58 -6 minecraft:moss_carpet
setblock 13 -59 -10 minecraft:clay
setblock 15 -59 -5 minecraft:melon
setblock 10 -59 -4 minecraft:tnt
# A sign so the owner knows what it is.
setblock 12 -60 -1 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"enderman design","top rod points","to stronghold"]}}
setblock 12 -54 -2 minecraft:light[level=12]
setblock 12 -54 -12 minecraft:light[level=10]
