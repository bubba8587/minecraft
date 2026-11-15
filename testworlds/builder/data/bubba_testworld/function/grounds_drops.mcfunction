# Drop pads x -22..-10, z 19..31: stone brick pillars at z 23 (ladder on the north side), each top running
# 2 blocks south; step off the south end onto the landing (z 26..31). Jump steps x -13..-7, z 40.
fill -22 -60 23 -22 -57 23 minecraft:stone_bricks
fill -22 -57 24 -22 -57 25 minecraft:stone_bricks
fill -22 -60 22 -22 -57 22 minecraft:ladder[facing=north]
setblock -21 -60 21 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Drop 4",bold:true,color:"dark_aqua"},{text:"step off: 1"},{text:"damage"},{text:"(0.5 hearts)"}]}}
setblock -22 -54 24 minecraft:light[level=15]
fill -19 -60 23 -19 -53 23 minecraft:stone_bricks
fill -19 -53 24 -19 -53 25 minecraft:stone_bricks
fill -19 -60 22 -19 -53 22 minecraft:ladder[facing=north]
setblock -18 -60 21 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Drop 8",bold:true,color:"dark_aqua"},{text:"step off: 5"},{text:"damage"},{text:"(2.5 hearts)"}]}}
setblock -19 -50 24 minecraft:light[level=15]
fill -16 -60 23 -16 -49 23 minecraft:stone_bricks
fill -16 -49 24 -16 -49 25 minecraft:stone_bricks
fill -16 -60 22 -16 -49 22 minecraft:ladder[facing=north]
setblock -15 -60 21 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Drop 12",bold:true,color:"dark_aqua"},{text:"step off: 9"},{text:"damage"},{text:"(4.5 hearts)"}]}}
setblock -16 -46 24 minecraft:light[level=15]
fill -13 -60 23 -13 -45 23 minecraft:stone_bricks
fill -13 -45 24 -13 -45 25 minecraft:stone_bricks
fill -13 -60 22 -13 -45 22 minecraft:ladder[facing=north]
setblock -12 -60 21 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Drop 16",bold:true,color:"dark_aqua"},{text:"step off: 13"},{text:"damage"},{text:"(6.5 hearts)"}]}}
setblock -13 -42 24 minecraft:light[level=15]
fill -10 -60 23 -10 -37 23 minecraft:stone_bricks
fill -10 -37 24 -10 -37 25 minecraft:stone_bricks
fill -10 -60 22 -10 -37 22 minecraft:ladder[facing=north]
setblock -9 -60 21 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Drop 24",bold:true,color:"dark_red"},{text:"step off: 21"},{text:"damage, kills"},{text:"at full health"}]}}
setblock -10 -34 24 minecraft:light[level=15]
setblock -23 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Wind Burst",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit wind_charged"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"bounces you",color:"gray"}]}}
setblock -22 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Oozing",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit oozing"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"slime landing",color:"gray"}]}}
setblock -21 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Slow Falling",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit slow_falling"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"click to wear",color:"gray"}]}}
setblock -20 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Jump Boost",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit jump_boost"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"click to wear",color:"gray"}]}}
setblock -19 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Healing",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit instant_health"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"heals falls",color:"gray"}]}}
setblock -17 -60 19 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Drop Pads",bold:true,color:"dark_aqua"},{text:"climb a ladder,"},{text:"step off the"},{text:"south end"}]}}
setblock -16 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Heal me",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"effect give @s minecraft:instant_health 1 5 true"}},{text:"click for"},{text:"full health"},{text:""}]}}
setblock -15 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Survival",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode survival @s"}},{text:"click to take"},{text:"damage here"},{text:""}]}}
setblock -14 -60 19 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Creative",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode creative @s"}},{text:"click to stop"},{text:"taking damage"},{text:""}]}}
setblock -22 -56 20 minecraft:light[level=15]
setblock -22 -56 26 minecraft:light[level=15]
setblock -16 -56 20 minecraft:light[level=15]
setblock -16 -56 26 minecraft:light[level=15]
setblock -10 -56 20 minecraft:light[level=15]
setblock -10 -56 26 minecraft:light[level=15]
fill -13 -60 40 -13 -60 40 minecraft:polished_andesite
setblock -13 -60 39 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"1 high",bold:true,color:"dark_aqua"},{text:"vanilla jumps 1"},{text:""},{text:""}]}}
fill -11 -60 40 -11 -59 40 minecraft:polished_andesite
setblock -11 -60 39 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"2 high",bold:true,color:"dark_aqua"},{text:"Jump Boost?"},{text:""},{text:""}]}}
fill -9 -60 40 -9 -58 40 minecraft:polished_andesite
setblock -9 -60 39 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"3 high",bold:true,color:"dark_aqua"},{text:"Jump Boost?"},{text:""},{text:""}]}}
fill -7 -60 40 -7 -57 40 minecraft:polished_andesite
setblock -7 -60 39 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"4 high",bold:true,color:"dark_aqua"},{text:"Jump Boost?"},{text:""},{text:""}]}}
setblock -12 -60 36 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Jump Boost",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit jump_boost"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"jump higher",color:"gray"}]}}
setblock -10 -60 36 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Jump Steps",bold:true,color:"dark_aqua"},{text:"1 to 4 blocks;"},{text:"how high can"},{text:"you jump?"}]}}
setblock -13 -56 38 minecraft:light[level=15]
setblock -7 -56 38 minecraft:light[level=15]
