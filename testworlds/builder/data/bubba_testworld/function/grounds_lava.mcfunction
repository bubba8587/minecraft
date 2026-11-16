# Lava moat x -28..-20, z 54..61: lava 2 deep (y -62..-61) in a deepslate brick rim, stairs out at the south
# end (x -24). Lava sources in a closed pit never flow; nothing flammable is within reach (warped signs).
fill -28 -61 54 -20 -61 61 minecraft:deepslate_bricks
fill -27 -62 55 -21 -61 60 minecraft:lava
setblock -24 -62 59 minecraft:deepslate_brick_stairs[facing=south]
setblock -24 -61 60 minecraft:deepslate_brick_stairs[facing=south]
setblock -26 -60 53 minecraft:warped_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Lava Moat",bold:true,color:"dark_red"},{text:"walk in from"},{text:"here; stairs"},{text:"out at the end"}]}}
setblock -25 -60 53 minecraft:warped_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Fire",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Resistance",bold:true,color:"dark_green",click_event:{action:"run_command",command:"gems kit fire_resistance"}},{text:"perfect set +",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"sword"}]}}
setblock -24 -60 53 minecraft:warped_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Fire",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Resistance",bold:true,color:"dark_green",click_event:{action:"run_command",command:"gems kit fire_resistance netherite 5"}},{text:"Power V 88% +",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"sword"}]}}
setblock -23 -60 53 minecraft:warped_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Get me out",bold:true,color:"dark_red",click_event:{action:"run_command",command:"tp @s -23.5 -60 62.5 180 0"}},{text:"puts you past",click_event:{action:"run_command",command:"effect give @s minecraft:fire_resistance 10 0 true"}},{text:"the moat with"},{text:"fire protection"}]}}
setblock -22 -60 53 minecraft:warped_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Survival",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode survival @s"}},{text:"click to take"},{text:"damage here"},{text:""}]}}
setblock -21 -60 53 minecraft:warped_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Creative",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode creative @s"}},{text:"click to stop"},{text:"taking damage"},{text:""}]}}
setblock -27 -55 53 minecraft:light[level=15]
setblock -27 -55 59 minecraft:light[level=15]
setblock -21 -55 53 minecraft:light[level=15]
setblock -21 -55 59 minecraft:light[level=15]
