# Crafting Tweaks: x -30..-14, z -4..12 (oak floor).
fill -30 -61 -4 -14 -61 12 minecraft:oak_planks
setblock -15 -60 3 minecraft:oak_sign[rotation=12]{is_waxed:1b,front_text:{messages:[{text:"Crafting",bold:true,color:"dark_green"},"Every recipe's","ingredients are","in the chests"]}}
setblock -20 -60 -4 minecraft:oak_sign[rotation=12]{is_waxed:1b,front_text:{messages:[{text:"Crafting",bold:true,color:"dark_green"},"glider, trident","dye, paper,","charcoal block"]}}
setblock -20 -60 -3 minecraft:chest[facing=east]{CustomName:"Crafting ingredients",Items:[{Slot:0b,id:"minecraft:bamboo",count:64},{Slot:1b,id:"minecraft:charcoal",count:64},{Slot:2b,id:"minecraft:coal",count:64},{Slot:3b,id:"minecraft:prismarine_shard",count:64},{Slot:4b,id:"minecraft:prismarine_crystals",count:64},{Slot:5b,id:"minecraft:stick",count:64},{Slot:6b,id:"minecraft:diamond",count:16},{Slot:7b,id:"minecraft:phantom_membrane",count:16},{Slot:8b,id:"minecraft:feather",count:16}]}
setblock -20 -60 -2 minecraft:crafting_table
setblock -20 -60 0 minecraft:oak_sign[rotation=12]{is_waxed:1b,front_text:{messages:[{text:"Blast Furnace",bold:true,color:"dark_green"},"coal block: ore","(slow); stone,","gravel, glass"]}}
setblock -20 -60 1 minecraft:chest[facing=east]{CustomName:"Blasting ingredients",Items:[{Slot:0b,id:"minecraft:coal_block",count:64},{Slot:1b,id:"minecraft:stone",count:64},{Slot:2b,id:"minecraft:cobblestone",count:64},{Slot:3b,id:"minecraft:gravel",count:64},{Slot:4b,id:"minecraft:glass",count:64},{Slot:5b,id:"minecraft:andesite",count:64},{Slot:6b,id:"minecraft:diorite",count:64},{Slot:7b,id:"minecraft:granite",count:64},{Slot:8b,id:"minecraft:coal",count:64},{Slot:9b,id:"minecraft:coal",count:64}]}
setblock -20 -60 2 minecraft:blast_furnace[facing=east]
setblock -20 -60 4 minecraft:oak_sign[rotation=12]{is_waxed:1b,front_text:{messages:[{text:"Smoker",bold:true,color:"dark_green"},"rotten flesh ->","leather; glow","items -> dust"]}}
setblock -20 -60 5 minecraft:chest[facing=east]{CustomName:"Smoking ingredients",Items:[{Slot:0b,id:"minecraft:rotten_flesh",count:64},{Slot:1b,id:"minecraft:glow_berries",count:64},{Slot:2b,id:"minecraft:glow_lichen",count:64},{Slot:3b,id:"minecraft:glow_ink_sac",count:64},{Slot:4b,id:"minecraft:coal",count:64}]}
setblock -20 -60 6 minecraft:smoker[facing=east]
fill -26 -60 2 -26 -58 2 minecraft:deepslate_diamond_ore[synthetic=true]
fill -26 -60 5 -26 -58 5 minecraft:deepslate_diamond_ore
setblock -25 -59 2 minecraft:oak_wall_sign[facing=east]{is_waxed:1b,front_text:{messages:[{text:"Synthetic",bold:true,color:"dark_green"},"Diamond Ore","1 in 4 drops a","diamond, no XP"]}}
setblock -25 -59 5 minecraft:oak_wall_sign[facing=east]{is_waxed:1b,front_text:{messages:[{text:"Natural",bold:true,color:"dark_green"},"Diamond Ore","normal drops,","Fortune works"]}}
setblock -25 -60 0 minecraft:chest[facing=east]{CustomName:"Ore testing",Items:[{Slot:0b,id:"minecraft:diamond_pickaxe",count:1},{Slot:1b,id:"minecraft:diamond_pickaxe",count:1,components:{"minecraft:enchantments":{"minecraft:silk_touch":1}}},{Slot:2b,id:"minecraft:diamond_pickaxe",count:1,components:{"minecraft:enchantments":{"minecraft:fortune":3}}},{Slot:3b,id:"minecraft:deepslate_diamond_ore",count:16,components:{"minecraft:item_name":"Synthetic Diamond Ore","minecraft:block_state":{synthetic:"true"}}},{Slot:4b,id:"minecraft:deepslate_diamond_ore",count:16}]}
setblock -25 -60 -1 minecraft:oak_sign[rotation=12]{is_waxed:1b,front_text:{messages:[{text:"Ore testing",bold:true,color:"dark_green"},"picks: plain,","silk touch,","fortune III"]}}
fill -27 -60 8 -27 -58 10 minecraft:stone_bricks
summon minecraft:item_frame -26 -59 8 {Pos:[-26.0d,-59.0d,8.0d],block_pos:[I;-26,-59,8],Facing:5b,Fixed:1b,Invulnerable:1b,Item:{id:"minecraft:paper",count:1,components:{"minecraft:item_name":"Bamboo Paper","minecraft:custom_model_data":{colors:[14217408]}}}}
summon minecraft:item_frame -26 -59 10 {Pos:[-26.0d,-59.0d,10.0d],block_pos:[I;-26,-59,10],Facing:5b,Fixed:1b,Invulnerable:1b,Item:{id:"minecraft:paper",count:1}}
setblock -26 -60 8 minecraft:oak_wall_sign[facing=east]{is_waxed:1b,front_text:{messages:[{text:"Bamboo Paper",bold:true,color:"dark_green"},"(light green","with the pack)",""]}}
setblock -26 -60 10 minecraft:oak_wall_sign[facing=east]{is_waxed:1b,front_text:{messages:[{text:"Plain Paper",bold:true,color:"dark_green"},"for comparison","",""]}}
setblock -26 -60 9 minecraft:chest[facing=east]{CustomName:"Paper",Items:[{Slot:0b,id:"minecraft:paper",count:32,components:{"minecraft:item_name":"Bamboo Paper","minecraft:custom_model_data":{colors:[14217408]}}},{Slot:1b,id:"minecraft:paper",count:32}]}
setblock -29 -56 -3 minecraft:light[level=15]
setblock -29 -56 3 minecraft:light[level=15]
setblock -29 -56 9 minecraft:light[level=15]
setblock -23 -56 -3 minecraft:light[level=15]
setblock -23 -56 3 minecraft:light[level=15]
setblock -23 -56 9 minecraft:light[level=15]
setblock -17 -56 -3 minecraft:light[level=15]
setblock -17 -56 3 minecraft:light[level=15]
setblock -17 -56 9 minecraft:light[level=15]
