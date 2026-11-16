# Cairns and endermen (specs/endermen.md, Reworked Mobs), far west of everything (x -232..-182): endermen roam up to
# 64 blocks around a cairn and fix any stray dirt, gravel or path within ~16 of where they stand, so every other
# station is kept more than 80 blocks from these cairns.
# Reached by click signs at the hub and back. Row of three finished cairns (seeds 11, 22, 33) at z 42, south of the arrival spot; a yard
# (unfenced: roaming endermen would hop any fence) with an unfinished cairn and builders, and a repair corner with a dirt pillar, a
# cobblestone heap, a pit and farmland for the caretakers. Sites from `cairn here` are saved with the world.
# Endermen are persistent; Reset removes every enderman in the yard. A backup of the yard's ground (y -63..-56) sits at y 198..205 for
# the Reset sign.
forceload add -236 -12 -180 56
# Three finished cairns.
execute positioned -222 -60 42 run bubbas_reworked_mobs cairn here 11
execute positioned -207 -60 42 run bubbas_reworked_mobs cairn here 22
execute positioned -192 -60 42 run bubbas_reworked_mobs cairn here 33
setblock -222 -60 32 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"seed 11","top rod points","to a stronghold"]}}
setblock -207 -60 32 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"seed 22","each design is","fixed by seed"]}}
setblock -192 -60 32 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"seed 33","offerings vary","by biome"]}}
# Unfinished cairn (seed 44): built whole, then everything above its footing and first trunk blocks taken off.
# Its builders come from `cairn builders` (one enderman per missing block, each carrying it).
execute positioned -218 -60 6 run bubbas_reworked_mobs cairn here 44
fill -226 -58 -2 -210 -30 14 minecraft:air
# Repair corner: a dirt pillar, a cobblestone heap, a 3x3 pit two deep, and farmland kept wet by a water block.
fill -196 -60 0 -196 -57 0 minecraft:dirt
fill -192 -60 -2 -191 -60 -1 minecraft:cobblestone
setblock -192 -59 -2 minecraft:cobblestone
fill -201 -62 9 -199 -61 11 minecraft:air
fill -194 -61 10 -190 -61 14 minecraft:farmland[moisture=7]
setblock -192 -61 12 minecraft:water
# Lights over the yard and the cairn row.
setblock -222 -54 34 minecraft:light[level=15]
setblock -207 -54 34 minecraft:light[level=15]
setblock -192 -54 34 minecraft:light[level=15]
setblock -228 -56 -6 minecraft:light[level=15]
setblock -212 -56 17 minecraft:light[level=15]
setblock -224 -56 16 minecraft:light[level=15]
setblock -200 -56 0 minecraft:light[level=15]
setblock -192 -56 6 minecraft:light[level=15]
setblock -207 -56 24 minecraft:light[level=15]
# Signs south of the yard (facing south, read from the arrival spot at -106.5, -60, 26.5).
setblock -211 -60 22 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Endermen",bold:true,color:"dark_purple"},"mend the land","and build","cairns"]}}
setblock -210 -60 22 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Builders",bold:true,color:"dark_purple"},"west: each","brings a block","the cairn lacks"]}}
setblock -209 -60 22 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Caretakers",bold:true,color:"dark_purple"},"east: pillar,","heap, pit and","farmland"]}}
setblock -205 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Reset yard",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"clone -232 198 -10 -182 205 22 -232 -63 -10"}},{text:"puts it back,",click_event:{action:"run_command",command:"fill -229 -55 -7 -185 -30 19 minecraft:air"}},{text:"no endermen",click_event:{action:"run_command",command:"kill @e[type=minecraft:enderman,x=-300,y=-64,z=-70,dx=180,dy=90,dz=190]"}},{text:"click",color:"gray",click_event:{action:"run_command",command:"kill @e[type=minecraft:item,x=-232,y=-64,z=-10,dx=50,dy=40,dz=32]"}}]}}
setblock -204 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Endermen",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"execute positioned -215.5 -60 16.5 run bubbas_reworked_mobs cairn builders"}},{text:"builders and",click_event:{action:"run_command",command:"summon minecraft:enderman -197.5 -60 5.5 {Tags:[\"tw_enderman\"],PersistenceRequired:1b}"}},{text:"2 caretakers",click_event:{action:"run_command",command:"summon minecraft:enderman -189.5 -60 4.5 {Tags:[\"tw_enderman\"],PersistenceRequired:1b}"}},{text:"click",color:"gray"}]}}
setblock -203 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Survival",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode survival @s"}},{text:"stare at one:"},{text:"idle ones get"},{text:"angry"}]}}
setblock -202 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Creative",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode creative @s"}},{text:"click to stop"},{text:"taking damage"},{text:""}]}}
setblock -201 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Back to hub",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"tp @s 0.5 -60 7.5 180 0"}},{text:"click to go"},{text:""},{text:""}]}}
# Backup of the yard ground for Reset, then the endermen.
clone -232 -63 -10 -182 -56 22 -232 198 -10
execute positioned -215.5 -60 16.5 run bubbas_reworked_mobs cairn builders
summon minecraft:enderman -197.5 -60 5.5 {Tags:["tw_enderman"],PersistenceRequired:1b}
summon minecraft:enderman -189.5 -60 4.5 {Tags:["tw_enderman"],PersistenceRequired:1b}
execute positioned -206 -60 26 run bubbas_reworked_mobs cairn locate
