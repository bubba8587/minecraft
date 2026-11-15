# Mob pen x 30..44, z 19..31: glass box 4 high with a glass roof, oak door in the west wall at z 25; spawn
# signs at z 18 summon one mob each into it; Clear pen removes everything inside.
fill 30 -60 19 44 -56 31 minecraft:glass outline
fill 31 -60 20 43 -57 30 minecraft:air
fill 30 -61 19 44 -61 31 minecraft:coarse_dirt
setblock 30 -60 25 minecraft:oak_door[facing=east,half=lower,hinge=left,open=false] strict
setblock 30 -59 25 minecraft:oak_door[facing=east,half=upper,hinge=left,open=false] strict
setblock 31 -60 18 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Mob Pen",bold:true,color:"dark_red"},{text:"click to spawn"},{text:"one; fight"},{text:"inside"}]}}
setblock 32 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Pig",bold:true,color:"dark_red",click_event:{action:"run_command",command:"summon minecraft:pig 34.5 -60 27.5"}},{text:"click to spawn"},{text:""},{text:""}]}}
setblock 33 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Cow",bold:true,color:"dark_red",click_event:{action:"run_command",command:"summon minecraft:cow 35.5 -60 27.5"}},{text:"click to spawn"},{text:""},{text:""}]}}
setblock 34 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Zombie",bold:true,color:"dark_red",click_event:{action:"run_command",command:"summon minecraft:zombie 36.5 -60 27.5"}},{text:"click to spawn"},{text:""},{text:""}]}}
setblock 35 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Skeleton",bold:true,color:"dark_red",click_event:{action:"run_command",command:"summon minecraft:skeleton 37.5 -60 27.5"}},{text:"click to spawn"},{text:""},{text:""}]}}
setblock 36 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Spider",bold:true,color:"dark_red",click_event:{action:"run_command",command:"summon minecraft:spider 38.5 -60 27.5"}},{text:"click to spawn"},{text:""},{text:""}]}}
setblock 37 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Wither",bold:true,color:"dark_red",click_event:{action:"run_command",command:"summon minecraft:wither_skeleton 39.5 -60 27.5"}},{text:"Skeleton",bold:true,color:"dark_red"},{text:"click to spawn"},{text:""}]}}
setblock 38 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Husk",bold:true,color:"dark_red",click_event:{action:"run_command",command:"summon minecraft:husk 40.5 -60 27.5"}},{text:"click to spawn"},{text:""},{text:""}]}}
setblock 39 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Slime",bold:true,color:"dark_red",click_event:{action:"run_command",command:"summon minecraft:slime 41.5 -60 27.5 {Size:1}"}},{text:"click to spawn"},{text:""},{text:""}]}}
setblock 40 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Clear pen",bold:true,color:"black",click_event:{action:"run_command",command:"kill @e[type=!minecraft:player,x=31,y=-60,z=20,dx=12,dy=3,dz=10]"}},{text:"removes every"},{text:"mob and item"},{text:"inside"}]}}
setblock 41 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Invisibility",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"perfect set",click_event:{action:"run_command",command:"gems kit invisibility"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"sneak up",color:"gray"}]}}
setblock 45 -60 18 minecraft:birch_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Invisibility",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Infusion V 88%",click_event:{action:"run_command",command:"gems kit invisibility netherite 5"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"sneak up",color:"dark_gray"}]}}
setblock 42 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Oozing",bold:true,color:"dark_green",click_event:{action:"run_command",command:'give @s minecraft:netherite_sword[bubbas_infused_gems:socketed_gems=[{effect:"minecraft:oozing",infusion:6,tier:3,main:1.0f},{effect:"minecraft:oozing",infusion:6,tier:3,main:1.0f}]]'}},{text:"sword, 2 gems"},{text:"click to get",color:"gray"},{text:""}]}}
setblock 43 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Weaving",bold:true,color:"dark_green",click_event:{action:"run_command",command:'give @s minecraft:netherite_sword[bubbas_infused_gems:socketed_gems=[{effect:"minecraft:weaving",infusion:6,tier:3,main:1.0f},{effect:"minecraft:weaving",infusion:6,tier:3,main:1.0f}]]'}},{text:"sword, 2 gems"},{text:"click to get",color:"gray"},{text:""}]}}
setblock 44 -60 18 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Wind Burst",bold:true,color:"dark_green",click_event:{action:"run_command",command:'give @s minecraft:mace[bubbas_infused_gems:socketed_gems=[{effect:"minecraft:wind_charged",infusion:6,tier:3,main:1.0f},{effect:"minecraft:wind_charged",infusion:6,tier:3,main:1.0f}]]'}},{text:"mace, 2 gems"},{text:"click to get",color:"gray"},{text:""}]}}
setblock 32 -57 21 minecraft:light[level=15]
setblock 32 -57 26 minecraft:light[level=15]
setblock 37 -57 21 minecraft:light[level=15]
setblock 37 -57 26 minecraft:light[level=15]
setblock 42 -57 21 minecraft:light[level=15]
setblock 42 -57 26 minecraft:light[level=15]
setblock 31 -56 18 minecraft:light[level=15]
setblock 37 -56 18 minecraft:light[level=15]
setblock 43 -56 18 minecraft:light[level=15]
