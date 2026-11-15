# Graveyard (Reworked Mobs): x -44..-14, z -44..-14.
fill -44 -60 -44 -14 -60 -44 minecraft:mossy_cobblestone_wall
fill -44 -60 -14 -21 -60 -14 minecraft:mossy_cobblestone_wall
fill -17 -60 -14 -14 -60 -14 minecraft:mossy_cobblestone_wall
fill -44 -60 -44 -44 -60 -14 minecraft:mossy_cobblestone_wall
fill -14 -60 -44 -14 -60 -14 minecraft:mossy_cobblestone_wall
setblock -22 -60 -13 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Graveyard",bold:true,color:"dark_red"},"Noise wakes","them. Sneak to","stay quiet."]}}
setblock -16 -60 -13 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Rob them",bold:true,color:"dark_red"},"Use one with an","empty hand to","take one item"]}}
setblock -17 -60 -16 minecraft:chest[facing=south]{CustomName:"Graveyard tools",Items:[{Slot:0b,id:"bubbas_reworked_mobs:skeleton_sleeper",count:8},{Slot:1b,id:"bubbas_reworked_mobs:skeleton_sleeper",count:8,components:{"minecraft:block_state":{pose:"lying"}}},{Slot:2b,id:"bubbas_reworked_mobs:zombie_sleeper",count:8},{Slot:3b,id:"bubbas_reworked_mobs:zombie_sleeper",count:8,components:{"minecraft:block_state":{pose:"lying"}}},{Slot:4b,id:"minecraft:diamond_pickaxe",count:1,components:{"minecraft:enchantments":{"minecraft:silk_touch":1}}},{Slot:5b,id:"minecraft:diamond_pickaxe",count:1},{Slot:6b,id:"minecraft:white_wool",count:64},{Slot:7b,id:"minecraft:tnt",count:4},{Slot:8b,id:"minecraft:flint_and_steel",count:1}]}
setblock -15 -60 -16 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Sleepers",bold:true,color:"dark_red"},"Silk Touch pick","mines one as","an item"]}}
setblock -42 -61 -18 minecraft:coarse_dirt
setblock -42 -60 -18 bubbas_reworked_mobs:skeleton_sleeper[facing=north,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1},{Slot:2b,id:"minecraft:iron_helmet",count:1},{Slot:3b,id:"minecraft:chainmail_chestplate",count:1},{Slot:4b,id:"minecraft:iron_leggings",count:1},{Slot:5b,id:"minecraft:chainmail_boots",count:1}]}
setblock -39 -61 -18 minecraft:coarse_dirt
setblock -39 -60 -18 bubbas_reworked_mobs:skeleton_sleeper[facing=east,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock -36 -61 -18 minecraft:coarse_dirt
setblock -36 -60 -18 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]
setblock -33 -61 -18 minecraft:coarse_dirt
setblock -33 -60 -18 bubbas_reworked_mobs:skeleton_sleeper[facing=west,pose=sitting]{Items:[{Slot:0b,id:"minecraft:stone_sword",count:1},{Slot:2b,id:"minecraft:leather_helmet",count:1}]}
setblock -30 -61 -18 minecraft:coarse_dirt
setblock -30 -60 -18 bubbas_reworked_mobs:skeleton_sleeper[facing=north,pose=lying]{Items:[{Slot:0b,id:"minecraft:bow",count:1},{Slot:2b,id:"minecraft:iron_helmet",count:1},{Slot:3b,id:"minecraft:chainmail_chestplate",count:1},{Slot:4b,id:"minecraft:iron_leggings",count:1},{Slot:5b,id:"minecraft:chainmail_boots",count:1}]}
setblock -27 -61 -18 minecraft:coarse_dirt
setblock -27 -60 -18 bubbas_reworked_mobs:skeleton_sleeper[facing=east,pose=lying]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock -24 -61 -18 minecraft:coarse_dirt
setblock -24 -60 -18 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=lying]
setblock -21 -61 -18 minecraft:coarse_dirt
setblock -21 -60 -18 bubbas_reworked_mobs:skeleton_sleeper[facing=west,pose=lying]{Items:[{Slot:0b,id:"minecraft:stone_sword",count:1},{Slot:2b,id:"minecraft:leather_helmet",count:1}]}
setblock -42 -61 -24 minecraft:coarse_dirt
setblock -42 -60 -24 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1},{Slot:1b,id:"minecraft:shield",count:1},{Slot:2b,id:"minecraft:golden_helmet",count:1},{Slot:3b,id:"minecraft:iron_chestplate",count:1},{Slot:4b,id:"minecraft:leather_leggings",count:1},{Slot:5b,id:"minecraft:iron_boots",count:1}]}
setblock -39 -61 -24 minecraft:coarse_dirt
setblock -39 -60 -24 bubbas_reworked_mobs:zombie_sleeper[facing=east,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_axe",count:1}]}
setblock -36 -61 -24 minecraft:coarse_dirt
setblock -36 -60 -24 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]
setblock -33 -61 -24 minecraft:coarse_dirt
setblock -33 -60 -24 bubbas_reworked_mobs:zombie_sleeper[facing=west,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_shovel",count:1},{Slot:3b,id:"minecraft:chainmail_chestplate",count:1}]}
setblock -30 -61 -24 minecraft:coarse_dirt
setblock -30 -60 -24 bubbas_reworked_mobs:zombie_sleeper[facing=north,pose=lying]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1},{Slot:1b,id:"minecraft:shield",count:1},{Slot:2b,id:"minecraft:golden_helmet",count:1},{Slot:3b,id:"minecraft:iron_chestplate",count:1},{Slot:4b,id:"minecraft:leather_leggings",count:1},{Slot:5b,id:"minecraft:iron_boots",count:1}]}
setblock -27 -61 -24 minecraft:coarse_dirt
setblock -27 -60 -24 bubbas_reworked_mobs:zombie_sleeper[facing=east,pose=lying]{Items:[{Slot:0b,id:"minecraft:iron_axe",count:1}]}
setblock -24 -61 -24 minecraft:coarse_dirt
setblock -24 -60 -24 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=lying]
setblock -21 -61 -24 minecraft:coarse_dirt
setblock -21 -60 -24 bubbas_reworked_mobs:zombie_sleeper[facing=west,pose=lying]{Items:[{Slot:0b,id:"minecraft:iron_shovel",count:1},{Slot:3b,id:"minecraft:chainmail_chestplate",count:1}]}
setblock -43 -60 -20 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Skeletons",bold:true,color:"dark_red"},"sit N E S W,","lie N E S W;","mixed gear"]}}
setblock -43 -60 -26 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Zombies",bold:true,color:"dark_red"},"sit N E S W,","lie N E S W;","mixed gear"]}}
fill -42 -60 -36 -36 -58 -30 minecraft:white_wool outline
fill -41 -60 -35 -37 -58 -31 minecraft:air
setblock -40 -60 -33 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock -39 -60 -33 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock -38 -60 -33 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1},{Slot:1b,id:"minecraft:shield",count:1},{Slot:2b,id:"minecraft:golden_helmet",count:1},{Slot:3b,id:"minecraft:iron_chestplate",count:1},{Slot:4b,id:"minecraft:leather_leggings",count:1},{Slot:5b,id:"minecraft:iron_boots",count:1}]}
setblock -39 -60 -29 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Wool Pen",bold:true,color:"dark_red"},"Stomp around:","the wool walls","block noise"]}}
fill -32 -61 -44 -18 -61 -34 minecraft:stone_bricks
fill -32 -60 -44 -18 -59 -34 minecraft:mossy_stone_bricks outline
fill -31 -60 -43 -19 -59 -35 minecraft:air
fill -26 -60 -34 -24 -59 -34 minecraft:air
setblock -27 -60 -33 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Crypt",bold:true,color:"dark_red"},"One waking up","wakes up to 4","more nearby"]}}
setblock -30 -60 -42 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1},{Slot:2b,id:"minecraft:iron_helmet",count:1},{Slot:3b,id:"minecraft:chainmail_chestplate",count:1},{Slot:4b,id:"minecraft:iron_leggings",count:1},{Slot:5b,id:"minecraft:chainmail_boots",count:1}]}
setblock -28 -60 -42 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock -26 -60 -42 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock -24 -60 -42 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]
setblock -22 -60 -42 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock -20 -60 -42 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1},{Slot:1b,id:"minecraft:shield",count:1},{Slot:2b,id:"minecraft:golden_helmet",count:1},{Slot:3b,id:"minecraft:iron_chestplate",count:1},{Slot:4b,id:"minecraft:leather_leggings",count:1},{Slot:5b,id:"minecraft:iron_boots",count:1}]}
setblock -30 -60 -40 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]
setblock -28 -60 -40 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock -26 -60 -40 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:stone_axe",count:1}]}
setblock -24 -60 -40 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]
setblock -22 -60 -40 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1},{Slot:1b,id:"minecraft:shield",count:1},{Slot:2b,id:"minecraft:golden_helmet",count:1},{Slot:3b,id:"minecraft:iron_chestplate",count:1},{Slot:4b,id:"minecraft:leather_leggings",count:1},{Slot:5b,id:"minecraft:iron_boots",count:1}]}
setblock -20 -60 -40 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock -30 -60 -38 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]
setblock -28 -60 -38 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock -26 -60 -38 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock -24 -60 -38 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1},{Slot:1b,id:"minecraft:shield",count:1},{Slot:2b,id:"minecraft:golden_helmet",count:1},{Slot:3b,id:"minecraft:iron_chestplate",count:1},{Slot:4b,id:"minecraft:leather_leggings",count:1},{Slot:5b,id:"minecraft:iron_boots",count:1}]}
setblock -22 -60 -38 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock -20 -60 -38 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:stone_axe",count:1}]}
setblock -30 -60 -36 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=lying]
setblock -28 -60 -36 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=lying]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock -26 -60 -36 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=lying]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1},{Slot:1b,id:"minecraft:shield",count:1},{Slot:2b,id:"minecraft:golden_helmet",count:1},{Slot:3b,id:"minecraft:iron_chestplate",count:1},{Slot:4b,id:"minecraft:leather_leggings",count:1},{Slot:5b,id:"minecraft:iron_boots",count:1}]}
setblock -24 -60 -36 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=lying]
setblock -22 -60 -36 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=lying]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock -20 -60 -36 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=lying]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock -42 -56 -42 minecraft:light[level=15]
setblock -42 -56 -36 minecraft:light[level=15]
setblock -42 -56 -30 minecraft:light[level=15]
setblock -42 -56 -24 minecraft:light[level=15]
setblock -42 -56 -18 minecraft:light[level=15]
setblock -36 -56 -42 minecraft:light[level=15]
setblock -36 -56 -36 minecraft:light[level=15]
setblock -36 -56 -30 minecraft:light[level=15]
setblock -36 -56 -24 minecraft:light[level=15]
setblock -36 -56 -18 minecraft:light[level=15]
setblock -30 -56 -42 minecraft:light[level=15]
setblock -30 -56 -36 minecraft:light[level=15]
setblock -30 -56 -30 minecraft:light[level=15]
setblock -30 -56 -24 minecraft:light[level=15]
setblock -30 -56 -18 minecraft:light[level=15]
setblock -24 -56 -42 minecraft:light[level=15]
setblock -24 -56 -36 minecraft:light[level=15]
setblock -24 -56 -30 minecraft:light[level=15]
setblock -24 -56 -24 minecraft:light[level=15]
setblock -24 -56 -18 minecraft:light[level=15]
setblock -18 -56 -42 minecraft:light[level=15]
setblock -18 -56 -36 minecraft:light[level=15]
setblock -18 -56 -30 minecraft:light[level=15]
setblock -18 -56 -24 minecraft:light[level=15]
setblock -18 -56 -18 minecraft:light[level=15]
