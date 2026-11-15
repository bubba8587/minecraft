# Combat arena: fenced x 14..40, z -44..-14, gate on the south side at x 27.
fill 15 -61 -43 39 -61 -15 minecraft:coarse_dirt
fill 14 -60 -44 40 -60 -44 minecraft:oak_fence
fill 14 -60 -14 26 -60 -14 minecraft:oak_fence
fill 28 -60 -14 40 -60 -14 minecraft:oak_fence
fill 14 -60 -44 14 -60 -14 minecraft:oak_fence
fill 40 -60 -44 40 -60 -14 minecraft:oak_fence
setblock 27 -60 -14 minecraft:oak_fence_gate[facing=south]
setblock 24 -60 -12 minecraft:chest[facing=south]{CustomName:"Weapons",Items:[{Slot:0b,id:"minecraft:diamond_sword",count:1,components:{"minecraft:enchantments":{"minecraft:smite":5}}},{Slot:1b,id:"minecraft:netherite_sword",count:1,components:{"minecraft:enchantments":{"minecraft:smite":5}}},{Slot:2b,id:"minecraft:iron_axe",count:1,components:{"minecraft:enchantments":{"minecraft:smite":5}}},{Slot:3b,id:"minecraft:diamond_sword",count:1},{Slot:4b,id:"minecraft:bow",count:1},{Slot:5b,id:"minecraft:bow",count:1,components:{"minecraft:enchantments":{"minecraft:power":5}}},{Slot:6b,id:"minecraft:crossbow",count:1},{Slot:7b,id:"minecraft:shield",count:1},{Slot:8b,id:"minecraft:arrow",count:64},{Slot:9b,id:"minecraft:arrow",count:64},{Slot:10b,id:"minecraft:spectral_arrow",count:32}]}
setblock 30 -60 -12 minecraft:chest[facing=south]{CustomName:"Armor and food",Items:[{Slot:0b,id:"minecraft:iron_helmet",count:1},{Slot:1b,id:"minecraft:iron_chestplate",count:1},{Slot:2b,id:"minecraft:iron_leggings",count:1},{Slot:3b,id:"minecraft:iron_boots",count:1},{Slot:4b,id:"minecraft:diamond_helmet",count:1},{Slot:5b,id:"minecraft:diamond_chestplate",count:1},{Slot:6b,id:"minecraft:diamond_leggings",count:1},{Slot:7b,id:"minecraft:diamond_boots",count:1},{Slot:8b,id:"minecraft:netherite_helmet",count:1},{Slot:9b,id:"minecraft:netherite_chestplate",count:1},{Slot:10b,id:"minecraft:netherite_leggings",count:1},{Slot:11b,id:"minecraft:netherite_boots",count:1},{Slot:12b,id:"minecraft:golden_apple",count:16},{Slot:13b,id:"minecraft:cooked_beef",count:64}]}
setblock 23 -60 -12 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Combat Arena",bold:true,color:"dark_red"},"Gear in chests.","To fight, use","survival mode"]}}
setblock 25 -60 -12 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Smite",bold:true,color:"dark_red"},"4% per level to","kill an undead","in one hit"]}}
setblock 29 -60 -12 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Arrows",bold:true,color:"dark_red"},"skeletons: weak","zombies: aim","for the head"]}}
setblock 31 -60 -12 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Fists",bold:true,color:"dark_red"},"their punch","streaks shred","armor; back off"]}}
setblock 18 -60 -40 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1},{Slot:2b,id:"minecraft:iron_helmet",count:1},{Slot:3b,id:"minecraft:chainmail_chestplate",count:1},{Slot:4b,id:"minecraft:iron_leggings",count:1},{Slot:5b,id:"minecraft:chainmail_boots",count:1}]}
setblock 21 -60 -40 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock 24 -60 -40 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock 27 -60 -40 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=lying]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1},{Slot:1b,id:"minecraft:shield",count:1},{Slot:2b,id:"minecraft:golden_helmet",count:1},{Slot:3b,id:"minecraft:iron_chestplate",count:1},{Slot:4b,id:"minecraft:leather_leggings",count:1},{Slot:5b,id:"minecraft:iron_boots",count:1}]}
setblock 30 -60 -40 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1}]}
setblock 33 -60 -40 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}
setblock 36 -60 -40 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]{Items:[{Slot:0b,id:"minecraft:bow",count:1},{Slot:2b,id:"minecraft:iron_helmet",count:1},{Slot:3b,id:"minecraft:chainmail_chestplate",count:1},{Slot:4b,id:"minecraft:iron_leggings",count:1},{Slot:5b,id:"minecraft:chainmail_boots",count:1}]}
setblock 20 -60 -30 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]
setblock 27 -60 -30 bubbas_reworked_mobs:skeleton_sleeper[facing=south,pose=sitting]
setblock 34 -60 -30 bubbas_reworked_mobs:zombie_sleeper[facing=south,pose=sitting]
setblock 16 -56 -42 minecraft:light[level=15]
setblock 16 -56 -36 minecraft:light[level=15]
setblock 16 -56 -30 minecraft:light[level=15]
setblock 16 -56 -24 minecraft:light[level=15]
setblock 16 -56 -18 minecraft:light[level=15]
setblock 22 -56 -42 minecraft:light[level=15]
setblock 22 -56 -36 minecraft:light[level=15]
setblock 22 -56 -30 minecraft:light[level=15]
setblock 22 -56 -24 minecraft:light[level=15]
setblock 22 -56 -18 minecraft:light[level=15]
setblock 28 -56 -42 minecraft:light[level=15]
setblock 28 -56 -36 minecraft:light[level=15]
setblock 28 -56 -30 minecraft:light[level=15]
setblock 28 -56 -24 minecraft:light[level=15]
setblock 28 -56 -18 minecraft:light[level=15]
setblock 34 -56 -42 minecraft:light[level=15]
setblock 34 -56 -36 minecraft:light[level=15]
setblock 34 -56 -30 minecraft:light[level=15]
setblock 34 -56 -24 minecraft:light[level=15]
setblock 34 -56 -18 minecraft:light[level=15]
