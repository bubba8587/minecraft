# Checks run on a copy of the finished world (tools/build-testworld.py verify step). Every setblock and display
# command of the build is also checked automatically (the block and its state are there); these are the rest.
forceload add -48 -48 47 15
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
