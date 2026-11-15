# Checks run on a copy of the finished world (tools/build-testworld.py verify step). Every setblock and display
# command of the build is also checked automatically (the block and its state are there); these are the rest.
forceload add -48 -48 47 63
scoreboard objectives add tw dummy
# world rules
execute store result score #r tw run gamerule advance_time
execute if score #r tw matches 0
execute store result score #r tw run gamerule advance_weather
execute if score #r tw matches 0
execute store result score #r tw run gamerule spawn_mobs
execute if score #r tw matches 0
execute store result score #r tw run time query time
execute if score #r tw matches 18000
execute if predicate {type:"minecraft:weather_check",raining:false,thundering:false}
# nothing woke up while building
execute unless entity @e[type=bubbas_reworked_mobs:woken_zombie]
execute unless entity @e[type=bubbas_reworked_mobs:woken_skeleton]
# hub floor and paths
execute if block 0 -61 7 minecraft:polished_andesite
execute if block 0 -61 -10 minecraft:dirt_path
# graveyard: gear is stored on the sleepers
execute if data block -42 -60 -18 Items[{Slot:0b,id:"minecraft:bow"}]
execute if data block -42 -60 -18 Items[{Slot:3b,id:"minecraft:chainmail_chestplate"}]
execute if data block -42 -60 -24 Items[{Slot:1b,id:"minecraft:shield"}]
execute unless data block -36 -60 -18 Items[0]
execute if block -42 -59 -33 minecraft:white_wool
execute if data block -17 -60 -16 Items[{id:"minecraft:diamond_pickaxe",components:{"minecraft:enchantments":{"minecraft:silk_touch":1}}}]
# loot on display holds real items
execute if data block -3 -59 -30 Items[{id:"minecraft:iron_sword"}]
execute if data block 1 -60 -21 Items[{id:"minecraft:trident"}]
execute if data block 6 -60 -22 Items[{id:"minecraft:iron_axe"}]
# arena chests
execute if data block 24 -60 -12 Items[{id:"minecraft:diamond_sword",components:{"minecraft:enchantments":{"minecraft:smite":5}}}]
execute if data block 30 -60 -12 Items[{id:"minecraft:netherite_chestplate"}]
execute if block 14 -60 -30 minecraft:oak_fence
# crafting tweaks
execute if block -26 -60 2 minecraft:deepslate_diamond_ore[synthetic=true]
execute if block -26 -58 2 minecraft:deepslate_diamond_ore[synthetic=true]
execute if block -26 -60 5 minecraft:deepslate_diamond_ore[synthetic=false]
execute if data block -20 -60 -3 Items[{id:"minecraft:phantom_membrane"}]
execute if data block -26 -60 9 Items[{id:"minecraft:paper",components:{"minecraft:item_name":"Bamboo Paper"}}]
execute if data block -25 -60 0 Items[{components:{"minecraft:block_state":{synthetic:"true"}}}]
execute if entity @e[type=minecraft:item_frame,x=-26,y=-59,z=8,distance=..1]
execute if entity @e[type=minecraft:item_frame,x=-26,y=-59,z=10,distance=..1]
# glider tower
execute if block 23 -30 6 minecraft:ladder[facing=west]
execute if data block 21 0 6 Items[{id:"bubbas_crafting_tweaks:glider"}]
execute if data block 21 0 6 Items[{id:"minecraft:elytra"}]
# infused gems station
execute if block 36 -60 6 minecraft:brewing_stand
execute if block 40 -60 10 minecraft:enchanting_table
execute if block 42 -60 12 minecraft:bookshelf
execute if data block 34 -60 6 Items[{id:"minecraft:lingering_potion",components:{"minecraft:potion_contents":{potion:"minecraft:strong_swiftness"}}}]
execute if data block 31 -60 6 Items[{id:"minecraft:nether_star"}]
execute if data block 45 -60 6 Items[{id:"minecraft:netherite_chestplate"}]
execute if entity @e[type=minecraft:pig,x=35.5,y=-60,z=11.5,distance=..1]
execute if data block 37 -60 6 Items[{id:"minecraft:netherite_chestplate",components:{"bubbas_infused_gems:socketed_gems":[{effect:"minecraft:speed"}]}}]
execute if data block 38 -60 6 Items[{id:"bubbas_infused_gems:gem"}]
execute if block 40 -58 8 minecraft:bookshelf
execute if block 40 -59 8 minecraft:air
execute if data block 45 -60 9 Items[{id:"minecraft:diamond_chestplate"}]
execute if data block 45 -60 13 Items[{id:"minecraft:mace"}]
# gem testing grounds (south of the hub)
execute store result score #r tw run gamerule command_blocks_work
execute if score #r tw matches 1
execute store result score #r tw run gamerule command_block_output
execute if score #r tw matches 0
execute if block 0 -61 20 minecraft:dark_oak_planks
execute if data block -3 -58 19 {allow_op_features:1b}
execute if data block -3 -58 19 front_text.messages[1].click_event{command:"gems kit speed"}
execute if data block 3 -59 19 front_text.messages[1].click_event{command:"gems kit speed leather"}
# the wardrobe's take-off command clears every armor slot (tried on an armor stand)
summon minecraft:armor_stand 0.5 -60 44.5 {Tags:["tw_probe"],NoGravity:1b,equipment:{head:{id:"minecraft:iron_helmet",count:1},feet:{id:"minecraft:iron_boots",count:1}}}
execute as @e[tag=tw_probe] run item override entity @s armor.* with minecraft:air
execute unless data entity @e[tag=tw_probe,limit=1] equipment.head
execute unless data entity @e[tag=tw_probe,limit=1] equipment.feet
kill @e[tag=tw_probe]
execute if entity @e[type=minecraft:armor_stand,tag=tw_stand,x=-4.5,y=-60,z=37.5,distance=..1,nbt={equipment:{chest:{components:{"bubbas_infused_gems:socketed_gems":[{effect:"minecraft:speed"}]}}}}]
execute if data block -37 -59 30 Items[{id:"minecraft:arrow"}]
execute if data block -35 -62 20 {Command:"scoreboard players add #gallery tw_clock 1"}
execute if block -45 -62 20 minecraft:chain_command_block
execute if entity @e[type=minecraft:skeleton,tag=tw_turret,limit=2]
execute if entity @e[type=minecraft:pillager,tag=tw_turret]
execute if entity @e[type=minecraft:oak_boat,tag=tw_turret]
execute if block -22 -57 23 minecraft:stone_bricks
execute if block -10 -37 25 minecraft:stone_bricks
execute if block -10 -40 22 minecraft:ladder[facing=north]
execute if block -22 -59 42 minecraft:cobweb
execute if block 13 -60 25 minecraft:water
execute if block 13 -54 25 minecraft:water
execute if block 13 -58 20 minecraft:glass
execute if block 23 -62 23 minecraft:water
execute if block 25 -21 23 minecraft:red_concrete
execute if block 37 -56 25 minecraft:glass
execute if block 37 -58 25 minecraft:air
execute if entity @e[type=minecraft:wither_skeleton,tag=tw_dummy]
execute if entity @e[type=minecraft:pig,tag=tw_dummy]
execute if data block 10 -62 37 {auto:1b}
execute if entity @e[type=minecraft:villager,tag=tw_villager,nbt={VillagerData:{profession:"minecraft:librarian"}}]
execute if entity @e[type=minecraft:villager,tag=tw_villager,nbt={Offers:{Recipes:[{buy:{count:20}}]}}]
execute if block -24 -62 57 minecraft:lava
execute if block -24 -61 60 minecraft:deepslate_brick_stairs
execute if block -8 -58 57 minecraft:air
execute if block -8 -56 57 minecraft:polished_blackstone_bricks
execute if entity @e[type=minecraft:zombie,tag=tw_dark]
execute if data block 4 -60 55 Items[{id:"minecraft:cooked_beef"}]
execute if block 20 -59 56 minecraft:obsidian
# tools for the gems that need them (owner: Haste needs mining tools)
execute if data block -3 -59 25 front_text.messages[2].click_event{command:'loot give @s loot {pools:[{rolls:1,entries:[{type:"minecraft:item",name:"minecraft:netherite_pickaxe"}]},{rolls:1,entries:[{type:"minecraft:item",name:"minecraft:netherite_shovel"}]},{rolls:1,entries:[{type:"minecraft:item",name:"minecraft:netherite_axe"}]}]}'}
loot spawn 0.5 -59 44.5 loot {pools:[{rolls:1,entries:[{type:"minecraft:item",name:"minecraft:netherite_pickaxe"}]},{rolls:1,entries:[{type:"minecraft:item",name:"minecraft:netherite_shovel"}]}]}
execute if entity @e[type=minecraft:item,x=0.5,y=-59,z=44.5,distance=..3,nbt={Item:{id:"minecraft:netherite_shovel"}}]
kill @e[type=minecraft:item,x=0.5,y=-59,z=44.5,distance=..3]
execute if data block 16 -60 53 Items[{id:"minecraft:netherite_pickaxe",components:{"minecraft:enchantments":{"minecraft:efficiency":5}}}]
execute if data block 16 -60 53 Items[{Slot:20b,id:"minecraft:iron_axe"}]
execute if block 10 -59 56 minecraft:dirt
execute if block 24 -58 56 minecraft:oak_log
