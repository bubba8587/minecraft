# Showcase, south of the boss arena: newer features built with the mods' own commands, for playtesting.
forceload add -48 96 47 175
# Crypt yard with a noble crypt (gravestones, the resting dead, noble dead in gemmed armor)
execute positioned -26 -60 126 run bubbas_reworked_mobs graveyard build large kept noble
# Skeleton camp (its roster appears when a player comes near; break every banner to clear it)
execute positioned 24 -60 126 run bubbas_reworked_mobs camp build skeleton medium 20261122
# World Tweaks: lava over magma bubbles and now and then erupts
fill -11 -61 149 -1 -61 159 minecraft:stone_bricks
fill -10 -63 150 -2 -63 158 minecraft:magma_block
fill -10 -62 150 -2 -61 158 minecraft:lava
# Sulfur geyser: potent sulfur under two blocks of water, lifts and speeds you up
fill 5 -61 151 11 -61 157 minecraft:stone_bricks
fill 6 -63 152 10 -63 156 minecraft:sulfur
fill 6 -62 152 10 -61 156 minecraft:water
setblock 8 -63 154 minecraft:potent_sulfur[potent_sulfur_state=wet]
# Every totem, with a sheep pen for Herding
setblock 0 -60 104 minecraft:chest[facing=north]{CustomName:"Every totem",Items:[{Slot:0b,id:"bubbas_totems_artifacts:totem_of_herding",count:1},{Slot:1b,id:"bubbas_totems_artifacts:totem_of_clear_skies",count:1},{Slot:2b,id:"bubbas_totems_artifacts:totem_of_gathering",count:1},{Slot:3b,id:"bubbas_totems_artifacts:totem_of_growth",count:1},{Slot:4b,id:"bubbas_totems_artifacts:totem_of_keeping",count:1},{Slot:5b,id:"bubbas_totems_artifacts:totem_of_seeking",count:1},{Slot:6b,id:"bubbas_totems_artifacts:totem_of_fortification",count:1},{Slot:7b,id:"bubbas_totems_artifacts:totem_of_warding",count:1},{Slot:8b,id:"bubbas_totems_artifacts:totem_of_the_beginning",count:1},{Slot:9b,id:"bubbas_totems_artifacts:totem_of_the_end",count:1},{Slot:10b,id:"bubbas_totems_artifacts:grave_talisman",count:1},{Slot:18b,id:"minecraft:wheat_seeds",count:32},{Slot:19b,id:"minecraft:oak_sapling",count:16}]}
setblock 0 -60 103 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Showcase",bold:true,color:"dark_purple"},"yard + crypt W,","camp E, lava","+ geyser S"]}}
fill -6 -60 164 6 -60 172 minecraft:oak_fence hollow
fill -5 -60 165 5 -60 171 minecraft:air
summon minecraft:sheep -2 -60 167
summon minecraft:sheep 0 -60 168
summon minecraft:sheep 2 -60 167
summon minecraft:cow 0 -60 170
