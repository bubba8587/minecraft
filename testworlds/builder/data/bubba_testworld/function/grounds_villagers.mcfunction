# Villagers x 30..44, z 36..46: three NoAI, invulnerable level 5 villagers at their work stations with
# fixed big-emerald trades, for the Hero of the Village gem.
fill 30 -61 37 44 -61 45 minecraft:cobblestone
setblock 33 -60 43 minecraft:blast_furnace[facing=north]
summon minecraft:villager 33.5 -60 41.5 {Tags:["tw_villager"],NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Rotation:[180f,0f],VillagerData:{profession:"minecraft:armorer",level:5,type:"minecraft:plains"},Offers:{Recipes:[{buy:{id:"minecraft:emerald",count:20},sell:{id:"minecraft:diamond_chestplate",count:1},maxUses:9999,rewardExp:0b,xp:0},{buy:{id:"minecraft:emerald",count:12},sell:{id:"minecraft:iron_helmet",count:1},maxUses:9999,rewardExp:0b,xp:0}]}}
setblock 37 -60 43 minecraft:lectern[facing=north]
summon minecraft:villager 37.5 -60 41.5 {Tags:["tw_villager"],NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Rotation:[180f,0f],VillagerData:{profession:"minecraft:librarian",level:5,type:"minecraft:plains"},Offers:{Recipes:[{buy:{id:"minecraft:emerald",count:32},sell:{id:"minecraft:enchanted_book",count:1,components:{"minecraft:stored_enchantments":{"minecraft:mending":1}}},maxUses:9999,rewardExp:0b,xp:0},{buy:{id:"minecraft:emerald",count:16},sell:{id:"minecraft:name_tag",count:1},maxUses:9999,rewardExp:0b,xp:0}]}}
setblock 41 -60 43 minecraft:grindstone[face=floor,facing=north]
summon minecraft:villager 41.5 -60 41.5 {Tags:["tw_villager"],NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Rotation:[180f,0f],VillagerData:{profession:"minecraft:weaponsmith",level:5,type:"minecraft:plains"},Offers:{Recipes:[{buy:{id:"minecraft:emerald",count:24},sell:{id:"minecraft:diamond_sword",count:1},maxUses:9999,rewardExp:0b,xp:0},{buy:{id:"minecraft:emerald",count:10},sell:{id:"minecraft:iron_axe",count:1},maxUses:9999,rewardExp:0b,xp:0}]}}
setblock 31 -60 38 minecraft:chest[facing=north]{CustomName:"Emeralds",Items:[{Slot:0b,id:"minecraft:emerald",count:64},{Slot:1b,id:"minecraft:emerald",count:64},{Slot:2b,id:"minecraft:emerald",count:64},{Slot:3b,id:"minecraft:emerald",count:64},{Slot:4b,id:"minecraft:emerald_block",count:64}]}
setblock 32 -60 37 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Villagers",bold:true,color:"dark_green"},{text:"trade, then"},{text:"wear Hero and"},{text:"trade again"}]}}
setblock 34 -60 37 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Hero of the",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Village",bold:true,color:"dark_green",click_event:{action:"run_command",command:"gems kit hero_of_the_village"}},{text:"netherite set +",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"sword"}]}}
setblock 36 -60 37 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Prices",bold:true,color:"dark_green"},{text:"20 to 32"},{text:"emeralds, so a"},{text:"cut shows"}]}}
setblock 31 -56 38 minecraft:light[level=15]
setblock 31 -56 44 minecraft:light[level=15]
setblock 37 -56 38 minecraft:light[level=15]
setblock 37 -56 44 minecraft:light[level=15]
setblock 43 -56 38 minecraft:light[level=15]
setblock 43 -56 44 minecraft:light[level=15]
