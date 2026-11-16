# Enderman cairn preview (specs/endermen.md): design only, inert. Chorus-like: a twisting, branching spire.
# Blocks that never generate in the overworld; never obsidian. Stronghold pointer for this preview: east (+x):
# the longest branch and the slab tail at the base point east.
# Footing (y -60): a small irregular pad of end stone bricks and end stone.
fill 11 -60 -8 13 -60 -6 minecraft:end_stone_bricks
setblock 11 -60 -8 minecraft:end_stone
setblock 13 -60 -6 minecraft:end_stone
setblock 10 -60 -7 minecraft:end_stone_brick_slab
setblock 12 -60 -9 minecraft:end_stone_brick_slab
# Trunk: purpur pillars climbing and stepping sideways like a chorus stalk.
setblock 12 -59 -7 minecraft:purpur_pillar
setblock 12 -58 -7 minecraft:purpur_pillar
setblock 12 -57 -7 minecraft:purpur_block
setblock 11 -57 -7 minecraft:purpur_pillar[axis=x]
setblock 11 -56 -7 minecraft:purpur_pillar
setblock 11 -55 -7 minecraft:purpur_block
setblock 11 -55 -6 minecraft:purpur_pillar[axis=z]
setblock 11 -54 -6 minecraft:purpur_pillar
setblock 11 -53 -6 minecraft:purpur_block
setblock 12 -53 -6 minecraft:purpur_pillar[axis=x]
setblock 12 -52 -6 minecraft:purpur_pillar
setblock 12 -51 -6 minecraft:purpur_block
setblock 12 -50 -6 minecraft:end_rod[facing=up]
# Branch 1, low: a short arm ending in a bud.
setblock 13 -57 -7 minecraft:purpur_pillar[axis=x]
setblock 14 -57 -7 minecraft:purpur_block
setblock 14 -56 -7 minecraft:end_rod[facing=up]
# Branch 2, middle, north: angled up.
setblock 11 -55 -8 minecraft:purpur_pillar[axis=z]
setblock 11 -55 -9 minecraft:purpur_block
setblock 11 -54 -9 minecraft:purpur_pillar
setblock 11 -53 -9 minecraft:end_rod[facing=up]
# Branch 3, high, the pointer: the longest arm, reaching east.
setblock 13 -53 -6 minecraft:purpur_pillar[axis=x]
setblock 14 -53 -6 minecraft:purpur_pillar[axis=x]
setblock 15 -53 -6 minecraft:purpur_block
setblock 16 -53 -6 minecraft:end_rod[facing=east]
# Slab tail along the ground, also pointing east.
setblock 15 -60 -7 minecraft:end_stone_brick_slab
setblock 16 -60 -7 minecraft:end_stone_brick_slab
setblock 18 -60 -7 minecraft:end_stone_brick_slab
# A sign so the owner knows what it is.
setblock 10 -60 -3 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"enderman design","preview: east =","to stronghold"]}}
setblock 12 -56 -3 minecraft:light[level=12]
