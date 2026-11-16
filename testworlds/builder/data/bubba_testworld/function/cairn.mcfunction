# Enderman cairn preview (specs/endermen.md): design only, inert. Blocks that never generate in the overworld.
# Stronghold pointer for this preview: east (+x): the leaning top stone and the slab tail at the base.
# Base (y -60): rough 5x5, corners missing, mixed end stone and end stone bricks.
fill 10 -60 -9 14 -60 -5 minecraft:end_stone
setblock 10 -60 -9 minecraft:air
setblock 14 -60 -9 minecraft:air
setblock 10 -60 -5 minecraft:air
setblock 14 -60 -5 minecraft:air
setblock 11 -60 -9 minecraft:end_stone_bricks
setblock 13 -60 -5 minecraft:end_stone_bricks
setblock 10 -60 -7 minecraft:end_stone_bricks
setblock 14 -60 -6 minecraft:purpur_block
# Second course (y -59): 3x3 with a purpur heart.
fill 11 -59 -8 13 -59 -6 minecraft:end_stone_bricks
setblock 12 -59 -7 minecraft:purpur_block
setblock 11 -59 -8 minecraft:end_stone
setblock 13 -59 -6 minecraft:end_stone
# Third course (y -58): a cross.
setblock 12 -58 -7 minecraft:purpur_pillar
setblock 11 -58 -7 minecraft:end_stone_brick_stairs[facing=east]
setblock 13 -58 -7 minecraft:end_stone_brick_stairs[facing=west]
setblock 12 -58 -8 minecraft:end_stone_brick_stairs[facing=south]
setblock 12 -58 -6 minecraft:end_stone_brick_stairs[facing=north]
# Column (y -57, -56).
setblock 12 -57 -7 minecraft:purpur_pillar
setblock 12 -56 -7 minecraft:end_stone_bricks
# Leaning top stone, offset one block east (the pointer), with an end rod.
setblock 13 -55 -7 minecraft:end_stone
setblock 13 -54 -7 minecraft:end_rod[facing=up]
# Slab tail along the ground, also pointing east.
setblock 15 -60 -7 minecraft:end_stone_brick_slab
setblock 16 -60 -7 minecraft:end_stone_brick_slab
setblock 18 -60 -7 minecraft:end_stone_brick_slab
# A sign so the owner knows what it is.
setblock 10 -60 -3 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"enderman design","preview: east =","to stronghold"]}}
setblock 12 -56 -3 minecraft:light[level=12]
