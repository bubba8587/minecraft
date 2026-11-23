# Showcase, south of the boss arena (2026-11-22): newer features built with the mods' own commands, for playtesting.
# The yards, camps and spiders stand on a stone plateau (ground y -52): superflat ground is too shallow for crypts and camp rooms.
forceload add -48 96 47 259
# Flat strip z 96..105: every totem, a Herding pen, World Tweaks lava and a sulfur geyser
setblock 7 -60 100 minecraft:chest[facing=north]{CustomName:"Every totem",Items:[{Slot:0b,id:"bubbas_totems_artifacts:totem_of_herding",count:1},{Slot:1b,id:"bubbas_totems_artifacts:totem_of_clear_skies",count:1},{Slot:2b,id:"bubbas_totems_artifacts:totem_of_gathering",count:1},{Slot:3b,id:"bubbas_totems_artifacts:totem_of_growth",count:1},{Slot:4b,id:"bubbas_totems_artifacts:totem_of_keeping",count:1},{Slot:5b,id:"bubbas_totems_artifacts:totem_of_seeking",count:1},{Slot:6b,id:"bubbas_totems_artifacts:totem_of_fortification",count:1},{Slot:7b,id:"bubbas_totems_artifacts:totem_of_warding",count:1},{Slot:8b,id:"bubbas_totems_artifacts:totem_of_the_beginning",count:1},{Slot:9b,id:"bubbas_totems_artifacts:totem_of_the_end",count:1},{Slot:10b,id:"bubbas_totems_artifacts:grave_talisman",count:1},{Slot:18b,id:"minecraft:wheat_seeds",count:32},{Slot:19b,id:"minecraft:oak_sapling",count:16},{Slot:20b,id:"minecraft:sugar_cane",count:16}]}
setblock 7 -60 99 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Showcase",bold:true,color:"dark_purple"},"totems here;","pen W, lava E,","yards S on hill"]}}
fill -46 -60 97 -36 -60 105 minecraft:oak_fence hollow
fill -45 -60 98 -37 -60 104 minecraft:air
summon minecraft:sheep -43 -60 100
summon minecraft:sheep -41 -60 102
summon minecraft:sheep -39 -60 100
summon minecraft:cow -41 -60 99
setblock -41 -60 96 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Herding pen",bold:true,color:"dark_purple"},"hold a Totem","of Herding",""]}}
fill 13 -61 96 23 -61 105 minecraft:stone_bricks
fill 14 -63 97 22 -63 104 minecraft:magma_block
fill 14 -62 97 22 -61 104 minecraft:lava
setblock 18 -60 95 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Lava pool",bold:true,color:"dark_purple"},"magma under it:","bubbles and","smoke geysers"]}}
fill 30 -61 96 38 -61 104 minecraft:stone_bricks
fill 31 -63 97 37 -63 103 minecraft:sulfur
fill 31 -62 97 37 -61 103 minecraft:water
setblock 34 -63 100 minecraft:potent_sulfur[potent_sulfur_state=wet]
setblock 34 -60 95 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Sulfur geyser",bold:true,color:"dark_purple"},"stand or glide","over it: lift","and speed"]}}
# Plateau x -46..46, z 108..256, ground y -52
fill -46 -61 108 46 -53 137 minecraft:stone
fill -46 -61 138 46 -53 167 minecraft:stone
fill -46 -61 168 46 -53 197 minecraft:stone
fill -46 -61 198 46 -53 227 minecraft:stone
fill -46 -61 228 46 -53 256 minecraft:stone
fill -46 -52 108 46 -52 167 minecraft:grass_block
fill -46 -52 168 46 -52 227 minecraft:grass_block
fill -46 -52 228 46 -52 256 minecraft:grass_block
# A stair up the plateau's north face at x -2..2
fill -2 -60 100 2 -60 100 minecraft:stone_brick_stairs[facing=south]
fill -2 -61 100 2 -61 100 minecraft:stone_bricks
fill -2 -59 101 2 -59 101 minecraft:stone_brick_stairs[facing=south]
fill -2 -61 101 2 -60 101 minecraft:stone_bricks
fill -2 -58 102 2 -58 102 minecraft:stone_brick_stairs[facing=south]
fill -2 -61 102 2 -59 102 minecraft:stone_bricks
fill -2 -57 103 2 -57 103 minecraft:stone_brick_stairs[facing=south]
fill -2 -61 103 2 -58 103 minecraft:stone_bricks
fill -2 -56 104 2 -56 104 minecraft:stone_brick_stairs[facing=south]
fill -2 -61 104 2 -57 104 minecraft:stone_bricks
fill -2 -55 105 2 -55 105 minecraft:stone_brick_stairs[facing=south]
fill -2 -61 105 2 -56 105 minecraft:stone_bricks
fill -2 -54 106 2 -54 106 minecraft:stone_brick_stairs[facing=south]
fill -2 -61 106 2 -55 106 minecraft:stone_bricks
fill -2 -53 107 2 -53 107 minecraft:stone_brick_stairs[facing=south]
fill -2 -61 107 2 -54 107 minecraft:stone_bricks
# Row A: a yard with each crypt wealth (gates at z 112)
execute positioned -32 -51 112 run bubbas_reworked_mobs graveyard build small kept pauper
setblock -32 -51 109 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Pauper crypt",bold:true,color:"dark_purple"},"small yard,","kept;","crypt below"]}}
execute positioned 0 -51 112 run bubbas_reworked_mobs graveyard build medium overgrown family
setblock 4 -51 109 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Family crypt",bold:true,color:"dark_purple"},"medium yard,","overgrown;","crypt below"]}}
execute positioned 30 -51 112 run bubbas_reworked_mobs graveyard build large kept noble
setblock 30 -51 109 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Noble crypt",bold:true,color:"dark_purple"},"large yard,","kept;","crypt below"]}}
# Row B: yard states without crypts (gates at z 172)
execute positioned -36 -51 172 run bubbas_reworked_mobs graveyard build lone nearly_gone none
setblock -36 -51 169 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Crypt yard",bold:true,color:"dark_purple"},"lone","nearly gone","no crypt"]}}
execute positioned -16 -51 172 run bubbas_reworked_mobs graveyard build small collapsed none
setblock -16 -51 169 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Crypt yard",bold:true,color:"dark_purple"},"small","collapsed","no crypt"]}}
execute positioned 8 -51 172 run bubbas_reworked_mobs graveyard build medium sunken none
setblock 8 -51 169 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Crypt yard",bold:true,color:"dark_purple"},"medium","sunken","no crypt"]}}
execute positioned 34 -51 172 run bubbas_reworked_mobs graveyard build small overgrown none
setblock 34 -51 169 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Crypt yard",bold:true,color:"dark_purple"},"small","overgrown","no crypt"]}}
# Row C: a camp of each kind (centres at z 212); rosters appear when a player comes near
execute positioned -34 -51 212 run bubbas_reworked_mobs camp build skeleton medium 20261122
setblock -34 -51 201 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Skeleton camp",bold:true,color:"dark_purple"},"medium; break","every banner","to clear it"]}}
execute positioned -14 -51 212 run bubbas_reworked_mobs camp build zombie small 20261123
setblock -14 -51 201 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Zombie camp",bold:true,color:"dark_purple"},"small; break","every banner","to clear it"]}}
execute positioned 4 -51 212 run bubbas_reworked_mobs camp build husk small 20261124
setblock 4 -51 201 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Husk camp",bold:true,color:"dark_purple"},"small; break","every banner","to clear it"]}}
execute positioned 22 -51 212 run bubbas_reworked_mobs camp build stray small 20261125
setblock 22 -51 201 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Stray camp",bold:true,color:"dark_purple"},"small; break","every banner","to clear it"]}}
execute positioned 39 -51 212 run bubbas_reworked_mobs camp build bogged small 20261126
setblock 39 -51 201 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Bogged camp",bold:true,color:"dark_purple"},"small; break","every banner","to clear it"]}}
# Row D: spiders
# A dark stone cave for a nest: interior x 20..34, z 240..252, y -51..-39; door on the north side
fill 19 -52 239 35 -38 253 minecraft:stone hollow
fill 26 -51 239 28 -49 239 minecraft:air
execute positioned 27 -51 246 run bubbas_reworked_mobs spider nest
setblock 27 -51 236 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Spider nest",bold:true,color:"dark_purple"},"in the cave:","walk under it","and look up"]}}
# Spruce grove with a giant spider: six 10-high trunks 5 apart
fill -40 -43 241 -36 -41 245 minecraft:spruce_leaves[persistent=true] keep
fill -38 -51 243 -38 -42 243 minecraft:spruce_log
fill -40 -43 246 -36 -41 250 minecraft:spruce_leaves[persistent=true] keep
fill -38 -51 248 -38 -42 248 minecraft:spruce_log
fill -35 -43 241 -31 -41 245 minecraft:spruce_leaves[persistent=true] keep
fill -33 -51 243 -33 -42 243 minecraft:spruce_log
fill -35 -43 246 -31 -41 250 minecraft:spruce_leaves[persistent=true] keep
fill -33 -51 248 -33 -42 248 minecraft:spruce_log
fill -30 -43 241 -26 -41 245 minecraft:spruce_leaves[persistent=true] keep
fill -28 -51 243 -28 -42 243 minecraft:spruce_log
fill -30 -43 246 -26 -41 250 minecraft:spruce_leaves[persistent=true] keep
fill -28 -51 248 -28 -42 248 minecraft:spruce_log
execute positioned -33 -51 246 run bubbas_reworked_mobs spider giant
setblock -33 -51 236 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Giant spider",bold:true,color:"dark_purple"},"its webs slow","you; it takes","livestock"]}}
