# Arrow gallery x -44..-30, z 19..31: stand on the red block (-37, 22); a lever (-35, -60, 20) runs a
# command block clock under the floor (y -62) that fires the three dispensers in turn and refills them.
fill -44 -61 20 -30 -61 31 minecraft:smooth_stone
setblock -37 -61 22 minecraft:red_concrete
fill -40 -60 19 -34 -58 19 minecraft:stone_bricks
setblock -37 -60 30 minecraft:stone_bricks
setblock -37 -60 31 minecraft:stone_bricks
setblock -37 -59 31 minecraft:stone_bricks
setblock -37 -59 30 minecraft:dispenser[facing=north]{Items:[{Slot:0b,id:"minecraft:arrow",count:64}]}
setblock -43 -60 22 minecraft:stone_bricks
setblock -44 -60 22 minecraft:stone_bricks
setblock -44 -59 22 minecraft:stone_bricks
setblock -43 -59 22 minecraft:dispenser[facing=east]{Items:[{Slot:0b,id:"minecraft:arrow",count:64}]}
setblock -31 -60 22 minecraft:stone_bricks
setblock -30 -60 22 minecraft:stone_bricks
setblock -30 -59 22 minecraft:stone_bricks
setblock -31 -59 22 minecraft:dispenser[facing=west]{Items:[{Slot:0b,id:"minecraft:arrow",count:64}]}
setblock -35 -60 20 minecraft:lever[face=floor,facing=south,powered=false]
setblock -35 -62 20 minecraft:repeating_command_block[facing=west]{Command:"scoreboard players add #gallery tw_clock 1",auto:0b,TrackOutput:0b}
setblock -36 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 20 run setblock -37 -59 31 minecraft:redstone_block",auto:1b,TrackOutput:0b}
setblock -37 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 22 run setblock -37 -59 31 minecraft:stone_bricks",auto:1b,TrackOutput:0b}
setblock -38 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 40 run setblock -44 -59 22 minecraft:redstone_block",auto:1b,TrackOutput:0b}
setblock -39 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 42 run setblock -44 -59 22 minecraft:stone_bricks",auto:1b,TrackOutput:0b}
setblock -40 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 60 run setblock -30 -59 22 minecraft:redstone_block",auto:1b,TrackOutput:0b}
setblock -41 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 62 run setblock -30 -59 22 minecraft:stone_bricks",auto:1b,TrackOutput:0b}
setblock -42 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 62.. run item replace block -37 -59 30 container.0 with minecraft:arrow 64",auto:1b,TrackOutput:0b}
setblock -43 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 62.. run item replace block -43 -59 22 container.0 with minecraft:arrow 64",auto:1b,TrackOutput:0b}
setblock -44 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 62.. run item replace block -31 -59 22 container.0 with minecraft:arrow 64",auto:1b,TrackOutput:0b}
setblock -45 -62 20 minecraft:chain_command_block[facing=west]{Command:"execute if score #gallery tw_clock matches 62.. run scoreboard players set #gallery tw_clock 0",auto:1b,TrackOutput:0b}
setblock -36 -59 20 minecraft:oak_wall_sign[facing=south]{is_waxed:1b,front_text:{messages:[{text:"Lever",bold:true,color:"dark_red"},{text:"turns arrows"},{text:"on and off:"},{text:"1 a second"}]}}
setblock -38 -59 20 minecraft:oak_wall_sign[facing=south]{allow_op_features:1b,front_text:{messages:[{text:"Survival",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode survival @s"}},{text:"click to take"},{text:"damage here"},{text:""}]}}
setblock -39 -59 20 minecraft:oak_wall_sign[facing=south]{allow_op_features:1b,front_text:{messages:[{text:"Creative",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode creative @s"}},{text:"click to stop"},{text:"taking damage"},{text:""}]}}
setblock -37 -59 18 minecraft:oak_wall_sign[facing=north]{is_waxed:1b,front_text:{messages:[{text:"Arrow Gallery",bold:true,color:"dark_red"},{text:"stand on red,"},{text:"flip the lever"},{text:"behind you"}]}}
setblock -36 -59 18 minecraft:oak_wall_sign[facing=north]{allow_op_features:1b,front_text:{messages:[{text:"Ender",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"perfect set",click_event:{action:"run_command",command:"gems kit ender"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"blinks you",color:"gray"}]}}
setblock -35 -59 18 minecraft:birch_wall_sign[facing=north]{allow_op_features:1b,front_text:{messages:[{text:"Ender",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Gem Power V 88%",click_event:{action:"run_command",command:"gems kit ender netherite 5"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"blinks you",color:"dark_gray"}]}}
setblock -38 -59 18 minecraft:oak_wall_sign[facing=north]{allow_op_features:1b,front_text:{messages:[{text:"Breeze",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"perfect set",click_event:{action:"run_command",command:"gems kit breeze"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"deflects",color:"gray"}]}}
setblock -39 -59 18 minecraft:birch_wall_sign[facing=north]{allow_op_features:1b,front_text:{messages:[{text:"Breeze",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Gem Power V 88%",click_event:{action:"run_command",command:"gems kit breeze netherite 5"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"deflects",color:"dark_gray"}]}}
setblock -34 -59 18 minecraft:oak_wall_sign[facing=north]{allow_op_features:1b,front_text:{messages:[{text:"Turtle Master",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"perfect set",click_event:{action:"run_command",command:"gems kit turtle_master"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"takes hits",color:"gray"}]}}
setblock -40 -59 18 minecraft:oak_wall_sign[facing=north]{allow_op_features:1b,front_text:{messages:[{text:"Resistance",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"perfect set",click_event:{action:"run_command",command:"gems kit resistance"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"takes hits",color:"gray"}]}}
setblock -43 -56 20 minecraft:light[level=15]
setblock -43 -56 26 minecraft:light[level=15]
setblock -37 -56 20 minecraft:light[level=15]
setblock -37 -56 26 minecraft:light[level=15]
setblock -31 -56 20 minecraft:light[level=15]
setblock -31 -56 26 minecraft:light[level=15]
