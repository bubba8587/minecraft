# Cairns and endermen (specs/endermen.md, Reworked Mobs), far west of everything (x -132..-82) so the caretakers
# have nothing else within their ~16-block reach: endermen fix any stray dirt, gravel or path they can reach.
# Reached by click signs at the hub and back. Row of three finished cairns (seeds 11, 22, 33) at z 42, south of the arrival spot; an iron bar
# yard (endermen can't lift bars) with an unfinished cairn and builders, and a repair corner with a dirt pillar, a
# cobblestone heap, a pit and farmland for the caretakers. Sites from `cairn here` are saved with the world.
# Endermen are persistent; Reset removes every enderman in the yard. A backup of the yard's ground (y -63..-56) sits at y 198..205 for
# the Reset sign.
forceload add -136 -12 -80 56
# Three finished cairns.
execute positioned -122 -60 42 run bubbas_reworked_mobs cairn here 11
execute positioned -107 -60 42 run bubbas_reworked_mobs cairn here 22
execute positioned -92 -60 42 run bubbas_reworked_mobs cairn here 33
setblock -122 -60 32 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"seed 11","top rod points","to a stronghold"]}}
setblock -107 -60 32 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"seed 22","each design is","fixed by seed"]}}
setblock -92 -60 32 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cairn",bold:true,color:"dark_purple"},"seed 33","offerings vary","by biome"]}}
# The yard: iron bars 4 high around x -130..-84, z -8..20, gate gap at x -108..-106 on the south side (bars, so
# endermen walk but can't pass; players step over the 1-high gap below).
fill -130 -60 -8 -84 -57 -8 minecraft:iron_bars
fill -130 -60 20 -84 -57 20 minecraft:iron_bars
fill -130 -60 -7 -130 -57 19 minecraft:iron_bars
fill -84 -60 -7 -84 -57 19 minecraft:iron_bars
# Unfinished cairn (seed 44): built whole, then everything above its footing and first trunk blocks taken off.
# Its builders come from `cairn builders` (one enderman per missing block, each carrying it).
execute positioned -118 -60 6 run bubbas_reworked_mobs cairn here 44
fill -126 -58 -2 -110 -30 14 minecraft:air
# Repair corner: a dirt pillar, a cobblestone heap, a 3x3 pit two deep, and farmland kept wet by a water block.
fill -96 -60 0 -96 -57 0 minecraft:dirt
fill -92 -60 -2 -91 -60 -1 minecraft:cobblestone
setblock -92 -59 -2 minecraft:cobblestone
fill -101 -62 9 -99 -61 11 minecraft:air
fill -94 -61 10 -90 -61 14 minecraft:farmland[moisture=7]
setblock -92 -61 12 minecraft:water
# Lights over the yard and the cairn row.
setblock -122 -54 34 minecraft:light[level=15]
setblock -107 -54 34 minecraft:light[level=15]
setblock -92 -54 34 minecraft:light[level=15]
setblock -128 -56 -6 minecraft:light[level=15]
setblock -112 -56 17 minecraft:light[level=15]
setblock -124 -56 16 minecraft:light[level=15]
setblock -100 -56 0 minecraft:light[level=15]
setblock -92 -56 6 minecraft:light[level=15]
setblock -107 -56 24 minecraft:light[level=15]
# Signs south of the yard (facing south, read from the arrival spot at -106.5, -60, 26.5).
setblock -111 -60 22 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Endermen",bold:true,color:"dark_purple"},"mend the land","and build","cairns"]}}
setblock -110 -60 22 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Builders",bold:true,color:"dark_purple"},"west: each","brings a block","the cairn lacks"]}}
setblock -109 -60 22 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Caretakers",bold:true,color:"dark_purple"},"east: pillar,","heap, pit and","farmland"]}}
setblock -105 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Reset yard",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"clone -132 198 -10 -82 205 22 -132 -63 -10"}},{text:"puts it back,",click_event:{action:"run_command",command:"fill -129 -55 -7 -85 -30 19 minecraft:air"}},{text:"no endermen",click_event:{action:"run_command",command:"kill @e[type=minecraft:enderman,x=-132,y=-64,z=-10,dx=50,dy=60,dz=32]"}},{text:"click",color:"gray",click_event:{action:"run_command",command:"kill @e[type=minecraft:item,x=-132,y=-64,z=-10,dx=50,dy=40,dz=32]"}}]}}
setblock -104 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Endermen",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"execute positioned -115.5 -60 16.5 run bubbas_reworked_mobs cairn builders"}},{text:"builders and",click_event:{action:"run_command",command:"summon minecraft:enderman -97.5 -60 5.5 {Tags:[\"tw_enderman\"],PersistenceRequired:1b}"}},{text:"2 caretakers",click_event:{action:"run_command",command:"summon minecraft:enderman -89.5 -60 4.5 {Tags:[\"tw_enderman\"],PersistenceRequired:1b}"}},{text:"click",color:"gray"}]}}
setblock -103 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Survival",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode survival @s"}},{text:"stare at one:"},{text:"idle ones get"},{text:"angry"}]}}
setblock -102 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Creative",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"gamemode creative @s"}},{text:"click to stop"},{text:"taking damage"},{text:""}]}}
setblock -101 -60 22 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Back to hub",bold:true,color:"dark_blue",click_event:{action:"run_command",command:"tp @s 0.5 -60 7.5 180 0"}},{text:"click to go"},{text:""},{text:""}]}}
# Backup of the yard ground for Reset, then the endermen.
clone -132 -63 -10 -82 -56 22 -132 198 -10
execute positioned -115.5 -60 16.5 run bubbas_reworked_mobs cairn builders
summon minecraft:enderman -97.5 -60 5.5 {Tags:["tw_enderman"],PersistenceRequired:1b}
summon minecraft:enderman -89.5 -60 4.5 {Tags:["tw_enderman"],PersistenceRequired:1b}
execute positioned -106 -60 26 run bubbas_reworked_mobs cairn locate
