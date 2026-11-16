# Creeper grove (specs/creepers.md, Reworked Mobs): a lit clearing far east of the gems station with one-log trees,
# a stone wall and boulders to peek around, one creeper among the trees and one on open ground to the east.
# Creepers are persistent and tagged tw_creeper. A backup copy of the grove sits at y 198..206 so the Reset sign can
# clone it back after an explosion (the builder pack is disabled in the finished world, so signs can't call functions).
forceload add 48 -16 82 16
# Path from the gems station's path (x 29..38, z -1..1) east to the grove, and a gravel viewing strip.
fill 39 -61 -1 55 -61 0 minecraft:dirt_path replace minecraft:grass_block
fill 52 -61 -3 55 -61 3 minecraft:gravel
# Trees: oak trunks 3 high with a small crown, like the creepers spec's tests.
fill 63 -60 -6 63 -58 -6 minecraft:oak_log
fill 62 -57 -7 64 -57 -5 minecraft:oak_leaves[persistent=true]
setblock 63 -56 -6 minecraft:oak_leaves[persistent=true]
fill 67 -60 4 67 -58 4 minecraft:oak_log
fill 66 -57 3 68 -57 5 minecraft:oak_leaves[persistent=true]
setblock 67 -56 4 minecraft:oak_leaves[persistent=true]
fill 62 -60 8 62 -58 8 minecraft:birch_log
fill 61 -57 7 63 -57 9 minecraft:birch_leaves[persistent=true]
setblock 62 -56 8 minecraft:birch_leaves[persistent=true]
fill 71 -60 -2 71 -58 -2 minecraft:spruce_log
fill 70 -57 -3 72 -57 -1 minecraft:spruce_leaves[persistent=true]
setblock 71 -56 -2 minecraft:spruce_leaves[persistent=true]
# A stone wall segment and two boulders.
fill 66 -60 -11 69 -58 -11 minecraft:cobblestone
setblock 66 -58 -11 minecraft:mossy_cobblestone
fill 59 -60 1 60 -59 2 minecraft:mossy_cobblestone
setblock 60 -59 2 minecraft:air
fill 72 -60 9 73 -59 9 minecraft:andesite
# Open ground east of x 76: nothing to hide behind within the creeper's 5-block reach.
# Lights over the grove (4 above the floor, every 6 blocks).
setblock 58 -56 -8 minecraft:light[level=15]
setblock 58 -56 4 minecraft:light[level=15]
setblock 66 -55 -2 minecraft:light[level=15]
setblock 66 -55 10 minecraft:light[level=15]
setblock 74 -55 -8 minecraft:light[level=15]
setblock 74 -55 4 minecraft:light[level=15]
setblock 80 -56 11 minecraft:light[level=15]
setblock 54 -56 0 minecraft:light[level=15]
# Signs on the viewing strip, readable from the path (facing south).
setblock 52 -60 -3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Creeper Grove",bold:true,color:"dark_green"},"Walk around:","it peeks out.","Look: it hides"]}}
setblock 53 -60 -3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Startle it",bold:true,color:"dark_red"},"Hit it, sprint","near it, throw","things past it"]}}
setblock 54 -60 -3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Hissing?",bold:true,color:"dark_red"},"Crouch, or back","off 7 blocks,","and it calms"]}}
setblock 55 -60 -3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Open ground",bold:true,color:"dark_green"},"East creeper:","no cover, it","stays 6 away"]}}
setblock 52 -60 3 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Reset grove",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"clone 56 198 -14 82 206 16 56 -63 -14"}},{text:"rebuilds it,",click_event:{action:"run_command",command:"kill @e[type=minecraft:creeper,tag=tw_creeper]"}},{text:"new creepers",click_event:{action:"run_command",command:"summon minecraft:creeper 64.5 -60 -7.5 {Tags:[\"tw_creeper\"],PersistenceRequired:1b,Rotation:[90f,0f]}"}},{text:"click",color:"gray",click_event:{action:"run_command",command:"summon minecraft:creeper 80.5 -60 11.5 {Tags:[\"tw_creeper\"],PersistenceRequired:1b,Rotation:[90f,0f]}"}}]}}
setblock 53 -60 3 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Throwables",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"give @s minecraft:snowball 16"}},{text:"snowballs, bow",click_event:{action:"run_command",command:"give @s minecraft:bow"}},{text:"and arrows",click_event:{action:"run_command",command:"give @s minecraft:arrow 64"}},{text:"click to get",color:"gray"}]}}
setblock 54 -60 3 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Survival",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode survival @s"}},{text:"click to take"},{text:"damage here"},{text:""}]}}
setblock 55 -60 3 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Creative",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode creative @s"}},{text:"click to stop"},{text:"taking damage"},{text:""}]}}
setblock 56 -60 3 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Tidy up",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"kill @e[type=minecraft:item,x=56,y=-64,z=-14,dx=26,dy=12,dz=30]"}},{text:"clears dropped"},{text:"items"},{text:"click",color:"gray"}]}}
# Backup copy for the Reset sign (ground y -63..-55 to sky y 198..206), then the creepers.
clone 56 -63 -14 82 -55 16 56 198 -14
summon minecraft:creeper 64.5 -60 -7.5 {Tags:["tw_creeper"],PersistenceRequired:1b,Rotation:[90f,0f]}
summon minecraft:creeper 80.5 -60 11.5 {Tags:["tw_creeper"],PersistenceRequired:1b,Rotation:[90f,0f]}
