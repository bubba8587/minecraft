# Sculk corridor x -42..-38, z 53..62: walk the 3-wide hall (x -41..-39) past sculk sensors (west wall) and
# calibrated sensors (east wall) with lamps on top; two zombie sleepers lie at the far end (z 61).
fill -42 -61 52 -38 -61 63 minecraft:deepslate_tiles
fill -42 -60 53 -42 -58 62 minecraft:deepslate_tiles
fill -38 -60 53 -38 -58 62 minecraft:deepslate_tiles
fill -41 -60 63 -39 -58 63 minecraft:deepslate_tiles
setblock -42 -60 54 minecraft:sculk_sensor
setblock -42 -59 54 minecraft:redstone_lamp
setblock -38 -60 54 minecraft:calibrated_sculk_sensor[facing=north]
setblock -38 -59 54 minecraft:redstone_lamp
setblock -42 -60 56 minecraft:sculk_sensor
setblock -42 -59 56 minecraft:redstone_lamp
setblock -38 -60 56 minecraft:calibrated_sculk_sensor[facing=north]
setblock -38 -59 56 minecraft:redstone_lamp
setblock -42 -60 58 minecraft:sculk_sensor
setblock -42 -59 58 minecraft:redstone_lamp
setblock -38 -60 58 minecraft:calibrated_sculk_sensor[facing=north]
setblock -38 -59 58 minecraft:redstone_lamp
setblock -42 -60 60 minecraft:sculk_sensor
setblock -42 -59 60 minecraft:redstone_lamp
setblock -38 -60 60 minecraft:calibrated_sculk_sensor[facing=north]
setblock -38 -59 60 minecraft:redstone_lamp
setblock -41 -60 61 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]
setblock -39 -60 61 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]
setblock -43 -60 52 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Sculk Hall",bold:true,color:"dark_aqua"},{text:"walk in: lamps"},{text:"light and the"},{text:"sleepers wake"}]}}
setblock -44 -60 52 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Silence",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"netherite set",click_event:{action:"run_command",command:"gems kit silence"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"walk silently",color:"gray"}]}}
setblock -37 -60 52 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Reset",bold:true,color:"dark_aqua",click_event:{action:"run_command",command:"kill @e[type=bubbas_reworked_mobs:woken_zombie,x=-40,y=-60,z=58,distance=..24]"}},{text:"sleepers",bold:true,color:"dark_aqua",click_event:{action:"run_command",command:"setblock -41 -60 61 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]"}},{text:"puts both back",click_event:{action:"run_command",command:"setblock -39 -60 61 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]"}},{text:"to sleep"}]}}
setblock -40 -56 53 minecraft:light[level=15]
setblock -40 -56 59 minecraft:light[level=15]
