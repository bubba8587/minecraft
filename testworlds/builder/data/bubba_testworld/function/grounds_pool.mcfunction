# Deep pool x 8..18, z 20..30: stone brick tank, water 7 deep (y -60..-54), windows in the north wall, stairs up
# the west side (x 7). Sky column x 21..25, z 20..25: a 3x3 water pit and a 40-block height pole.
fill 8 -61 20 18 -54 30 minecraft:stone_bricks
fill 9 -60 21 17 -54 29 minecraft:water
fill 10 -59 20 16 -56 20 minecraft:glass
fill 10 -60 28 12 -60 28 minecraft:deepslate
setblock 7 -60 22 minecraft:stone_brick_stairs[facing=south]
fill 7 -60 23 7 -60 23 minecraft:stone_bricks
setblock 7 -59 23 minecraft:stone_brick_stairs[facing=south]
fill 7 -60 24 7 -59 24 minecraft:stone_bricks
setblock 7 -58 24 minecraft:stone_brick_stairs[facing=south]
fill 7 -60 25 7 -58 25 minecraft:stone_bricks
setblock 7 -57 25 minecraft:stone_brick_stairs[facing=south]
fill 7 -60 26 7 -57 26 minecraft:stone_bricks
setblock 7 -56 26 minecraft:stone_brick_stairs[facing=south]
fill 7 -60 27 7 -56 27 minecraft:stone_bricks
setblock 7 -55 27 minecraft:stone_brick_stairs[facing=south]
fill 7 -60 28 7 -55 28 minecraft:stone_bricks
setblock 7 -54 28 minecraft:stone_brick_stairs[facing=south]
setblock 5 -60 19 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Deep Pool",bold:true,color:"dark_aqua"},{text:"7 deep; stairs"},{text:"on the west"},{text:"side"}]}}
setblock 6 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Water",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Breathing",bold:true,color:"dark_green",click_event:{action:"run_command",command:"gems kit water_breathing"}},{text:"netherite set +",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"sword"}]}}
setblock 7 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Dolphin's",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Grace",bold:true,color:"dark_green",click_event:{action:"run_command",command:"gems kit dolphins_grace"}},{text:"netherite set +",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"sword"}]}}
setblock 9 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Conduit Power",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit conduit_power"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"mine the floor",color:"gray"}]}}
setblock 10 -60 19 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Pool Floor",bold:true,color:"dark_aqua"},{text:"deepslate at"},{text:"the bottom to"},{text:"mine in water"}]}}
setblock 9 -50 22 minecraft:light[level=15]
setblock 9 -50 28 minecraft:light[level=15]
setblock 15 -50 22 minecraft:light[level=15]
setblock 15 -50 28 minecraft:light[level=15]
fill 22 -62 22 24 -61 24 minecraft:water
fill 25 -60 23 25 -21 23 minecraft:white_concrete
setblock 25 -56 23 minecraft:red_concrete
setblock 25 -51 23 minecraft:red_concrete
setblock 25 -46 23 minecraft:red_concrete
setblock 25 -41 23 minecraft:red_concrete
setblock 25 -36 23 minecraft:red_concrete
setblock 25 -31 23 minecraft:red_concrete
setblock 25 -26 23 minecraft:red_concrete
setblock 25 -21 23 minecraft:red_concrete
setblock 25 -51 22 minecraft:oak_wall_sign[facing=north]{is_waxed:1b,front_text:{messages:[{text:"10 up",bold:true,color:"dark_red"},{text:"red block every"},{text:"5 blocks"},{text:""}]}}
setblock 25 -41 22 minecraft:oak_wall_sign[facing=north]{is_waxed:1b,front_text:{messages:[{text:"20 up",bold:true,color:"dark_red"},{text:"red block every"},{text:"5 blocks"},{text:""}]}}
setblock 25 -31 22 minecraft:oak_wall_sign[facing=north]{is_waxed:1b,front_text:{messages:[{text:"30 up",bold:true,color:"dark_red"},{text:"red block every"},{text:"5 blocks"},{text:""}]}}
setblock 25 -21 22 minecraft:oak_wall_sign[facing=north]{is_waxed:1b,front_text:{messages:[{text:"40 up",bold:true,color:"dark_red"},{text:"red block every"},{text:"5 blocks"},{text:""}]}}
setblock 23 -60 20 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Sky Column",bold:true,color:"dark_aqua"},{text:"stand in the"},{text:"water and"},{text:"float up"}]}}
setblock 22 -60 20 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Levitation",bold:true,color:"dark_red",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit levitation"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"you will rise!",color:"gray"}]}}
setblock 24 -60 20 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Take off",bold:true,color:"black",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"armor",bold:true,color:"black"},{text:"to drop back"},{text:"into the water"}]}}
setblock 21 -56 21 minecraft:light[level=15]
setblock 21 -46 21 minecraft:light[level=15]
setblock 21 -36 21 minecraft:light[level=15]
setblock 21 -26 21 minecraft:light[level=15]
