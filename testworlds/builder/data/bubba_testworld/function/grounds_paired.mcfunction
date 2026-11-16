# Paired beacons room x -9..9, y 23..34, z 70..88 above the boss arena: a floor beacon (0, 24, 79) on a
# 4-layer iron pyramid (y 20..23) and an upside-down beacon (0, 30, 79) under a 4-layer gold pyramid (y 31..34). The
# pair's ward box is y 10..44; spawn test boxes inside it (interior y 12..14) and above it (control, y 48..50).
fill -9 23 70 9 34 88 minecraft:deepslate_bricks outline
fill -4 20 75 4 20 83 minecraft:iron_block
fill -3 21 76 3 21 82 minecraft:iron_block
fill -2 22 77 2 22 81 minecraft:iron_block
fill -1 23 78 1 23 80 minecraft:iron_block
setblock 0 24 79 minecraft:beacon
fill -1 31 78 1 31 80 minecraft:gold_block
fill -2 32 77 2 32 81 minecraft:gold_block
fill -3 33 76 3 33 82 minecraft:gold_block
fill -4 34 75 4 34 83 minecraft:gold_block
setblock 0 30 79 minecraft:beacon[upside_down=true]
setblock -5 24 73 minecraft:chest[facing=south]{CustomName:"Totems of Warding",Items:[{Slot:0b,id:"bubbas_totems_artifacts:totem_of_warding",count:1},{Slot:1b,id:"bubbas_totems_artifacts:totem_of_warding",count:1},{Slot:2b,id:"bubbas_totems_artifacts:totem_of_warding",count:1}]}
setblock -6 24 85 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]
setblock -3 24 86 bubbas_reworked_mobs:skeleton_sleeper[facing=north,pose=sitting]
setblock 6 24 85 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]
setblock 4 24 82 minecraft:bell[attachment=floor,facing=north]
setblock -5 26 74 minecraft:light[level=11]
setblock 5 26 74 minecraft:light[level=11]
setblock 0 26 73 minecraft:light[level=11]
setblock -5 26 84 minecraft:light[level=11]
setblock 5 26 84 minecraft:light[level=11]
setblock 0 26 86 minecraft:light[level=11]
fill -5 11 74 5 15 84 minecraft:deepslate_tiles outline
fill -5 47 74 5 51 84 minecraft:deepslate_tiles outline
setblock -2 25 71 minecraft:oak_wall_sign[facing=south]{allow_op_features:1b,front_text:{messages:[{text:"Way down",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"tp @s 15.5 -60 60.5 180 0"}},{text:"back to the"},{text:"ground"},{text:""}]}}
setblock -1 25 71 minecraft:oak_wall_sign[facing=south]{is_waxed:1b,front_text:{messages:[{text:"Paired Room",bold:true,color:"dark_aqua"},{text:"totem in the"},{text:"floor beacon:"},{text:"wards 150 out"}]}}
setblock 1 25 71 minecraft:oak_wall_sign[facing=south]{allow_op_features:1b,front_text:{messages:[{text:"Spawns on",bold:true,color:"dark_red",click_event:{action:"run_command",command:"gamerule spawn_mobs true"}},{text:"natural spawns"},{text:"in all dark;"},{text:"turn off after"}]}}
setblock 2 25 71 minecraft:oak_wall_sign[facing=south]{allow_op_features:1b,front_text:{messages:[{text:"Spawns off",bold:true,color:"dark_green",click_event:{action:"run_command",command:"gamerule spawn_mobs false"}},{text:"back to no"},{text:"natural spawns"},{text:""}]}}
setblock 3 25 71 minecraft:oak_wall_sign[facing=south]{allow_op_features:1b,front_text:{messages:[{text:"Count",bold:true,color:"black",click_event:{action:"run_command",command:"execute store result score #in tw_hp if entity @e[type=!minecraft:player,x=-4,y=12,z=75,dx=8,dy=2,dz=8]"}},{text:"spawns",bold:true,color:"black",click_event:{action:"run_command",command:"execute store result score #out tw_hp if entity @e[type=!minecraft:player,x=-4,y=48,z=75,dx=8,dy=2,dz=8]"}},{text:"in the two dark",click_event:{action:"run_command",command:'tellraw @s [{text:"Warded box: ",color:"gold"},{score:{name:"#in",objective:"tw_hp"}},{text:" mobs. Control box: "},{score:{name:"#out",objective:"tw_hp"}},{text:" mobs."}]'}},{text:"test boxes"}]}}
setblock 4 25 71 minecraft:oak_wall_sign[facing=south]{allow_op_features:1b,front_text:{messages:[{text:"Clear boxes",bold:true,color:"black",click_event:{action:"run_command",command:"kill @e[type=!minecraft:player,x=-4,y=12,z=75,dx=8,dy=2,dz=8]"}},{text:"empties both",click_event:{action:"run_command",command:"kill @e[type=!minecraft:player,x=-4,y=48,z=75,dx=8,dy=2,dz=8]"}},{text:"test boxes"},{text:""}]}}
setblock -1 24 76 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Sleepers",bold:true,color:"dark_aqua"},{text:"4 layers + a"},{text:"totem: the bell"},{text:"wakes nobody"}]}}
setblock 13 -60 62 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Paired Room",bold:true,color:"dark_aqua",click_event:{action:"run_command",command:"tp @s 0.5 24 73.5 180 0"}},{text:"click: go up to"},{text:"the paired"},{text:"beacons"}]}}
setblock 14 -60 62 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Spawns on",bold:true,color:"dark_red",click_event:{action:"run_command",command:"gamerule spawn_mobs true"}},{text:"natural spawns"},{text:"in all dark;"},{text:"turn off after"}]}}
setblock 15 -60 62 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Spawns off",bold:true,color:"dark_green",click_event:{action:"run_command",command:"gamerule spawn_mobs false"}},{text:"back to no"},{text:"natural spawns"},{text:""}]}}
setblock 16 -60 62 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Count",bold:true,color:"black",click_event:{action:"run_command",command:"execute store result score #in tw_hp if entity @e[type=!minecraft:player,x=-4,y=12,z=75,dx=8,dy=2,dz=8]"}},{text:"spawns",bold:true,color:"black",click_event:{action:"run_command",command:"execute store result score #out tw_hp if entity @e[type=!minecraft:player,x=-4,y=48,z=75,dx=8,dy=2,dz=8]"}},{text:"in the two dark",click_event:{action:"run_command",command:'tellraw @s [{text:"Warded box: ",color:"gold"},{score:{name:"#in",objective:"tw_hp"}},{text:" mobs. Control box: "},{score:{name:"#out",objective:"tw_hp"}},{text:" mobs."}]'}},{text:"test boxes"}]}}
setblock 17 -60 62 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Clear boxes",bold:true,color:"black",click_event:{action:"run_command",command:"kill @e[type=!minecraft:player,x=-4,y=12,z=75,dx=8,dy=2,dz=8]"}},{text:"empties both",click_event:{action:"run_command",command:"kill @e[type=!minecraft:player,x=-4,y=48,z=75,dx=8,dy=2,dz=8]"}},{text:"test boxes"},{text:""}]}}
setblock 15 -56 62 minecraft:light[level=15]
