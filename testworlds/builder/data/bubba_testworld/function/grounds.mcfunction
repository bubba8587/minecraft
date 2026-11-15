# Gem Testing Grounds: x -47..46, z 11..63, south of the hub (specs/testworld.md). Paths and hub signs;
# each area has its own grounds_* function.
gamerule command_block_output false
scoreboard objectives add tw_clock dummy
scoreboard objectives add tw_hp dummy
scoreboard players set #gallery tw_clock 0
fill -1 -61 11 1 -61 15 minecraft:dirt_path replace minecraft:grass_block
fill -46 -61 16 46 -61 17 minecraft:dirt_path replace minecraft:grass_block
fill -47 -61 33 46 -61 34 minecraft:dirt_path replace minecraft:grass_block
fill -46 -61 50 46 -61 51 minecraft:dirt_path replace minecraft:grass_block
fill -27 -61 18 -26 -61 49 minecraft:dirt_path replace minecraft:grass_block
fill 27 -61 18 28 -61 49 minecraft:dirt_path replace minecraft:grass_block
fill -47 -61 35 -47 -61 39 minecraft:dirt_path replace minecraft:grass_block
setblock -7 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Gem Grounds",bold:true,color:"dark_purple"},{text:"behind you:"},{text:"wardrobe, labs"},{text:"and ranges"}]}}
setblock -2 -60 11 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Gem Grounds",bold:true,color:"dark_purple"},{text:"click signs to"},{text:"wear gem sets;"},{text:"labs both ways"}]}}
setblock 2 -60 11 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Wardrobe",bold:true,color:"dark_purple"},{text:"straight on:"},{text:"every gem set,"},{text:"one click each"}]}}
setblock -45 -56 16 minecraft:light[level=15]
setblock -35 -56 16 minecraft:light[level=15]
setblock -25 -56 16 minecraft:light[level=15]
setblock -15 -56 16 minecraft:light[level=15]
setblock -5 -56 16 minecraft:light[level=15]
setblock 5 -56 16 minecraft:light[level=15]
setblock 15 -56 16 minecraft:light[level=15]
setblock 25 -56 16 minecraft:light[level=15]
setblock 35 -56 16 minecraft:light[level=15]
setblock 45 -56 16 minecraft:light[level=15]
function bubba_testworld:grounds_wardrobe
function bubba_testworld:grounds_stands
function bubba_testworld:grounds_gallery
function bubba_testworld:grounds_range
function bubba_testworld:grounds_drops
function bubba_testworld:grounds_webs
function bubba_testworld:grounds_pool
function bubba_testworld:grounds_pen
function bubba_testworld:grounds_dummies
function bubba_testworld:grounds_villagers
function bubba_testworld:grounds_sculk
function bubba_testworld:grounds_lava
function bubba_testworld:grounds_dark
function bubba_testworld:grounds_food
function bubba_testworld:grounds_mining
