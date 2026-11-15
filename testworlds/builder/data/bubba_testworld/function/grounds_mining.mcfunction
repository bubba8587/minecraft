# Mining wall x 13..21, z 56: stone, deepslate and obsidian, 3 high, for Haste and Mining Fatigue.
fill 12 -61 52 22 -61 57 minecraft:gravel
fill 13 -60 56 15 -58 56 minecraft:stone
fill 16 -60 56 18 -58 56 minecraft:deepslate
fill 19 -60 56 21 -58 56 minecraft:obsidian
setblock 13 -60 53 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Mining Wall",bold:true,color:"black"},{text:"stone, deep-"},{text:"slate,obsidian;"},{text:"survival only"}]}}
setblock 14 -60 53 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Haste",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit haste"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"mine faster",color:"gray"}]}}
setblock 15 -60 53 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Mining",bold:true,color:"dark_red",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Fatigue",bold:true,color:"dark_red",click_event:{action:"run_command",command:"gems kit mining_fatigue"}},{text:"netherite set +",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"sword"}]}}
setblock 17 -60 53 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Pickaxe",bold:true,color:"black",click_event:{action:"run_command",command:"give @s minecraft:diamond_pickaxe"}},{text:"a plain"},{text:"diamond pick"},{text:""}]}}
setblock 18 -60 53 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Rebuild",bold:true,color:"black",click_event:{action:"run_command",command:"fill 13 -60 56 15 -58 56 minecraft:stone"}},{text:"the wall",bold:true,color:"black",click_event:{action:"run_command",command:"fill 16 -60 56 18 -58 56 minecraft:deepslate"}},{text:"puts every",click_event:{action:"run_command",command:"fill 19 -60 56 21 -58 56 minecraft:obsidian"}},{text:"block back"}]}}
setblock 19 -60 53 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Survival",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode survival @s"}},{text:"click to take"},{text:"damage here"},{text:""}]}}
setblock 20 -60 53 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Creative",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode creative @s"}},{text:"click to stop"},{text:"taking damage"},{text:""}]}}
setblock 13 -56 54 minecraft:light[level=15]
setblock 17 -56 54 minecraft:light[level=15]
setblock 21 -56 54 minecraft:light[level=15]
