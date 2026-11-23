# Hub: spawn at 0 -60 7 facing north, one sign per station.
fill -7 -61 2 7 -61 10 minecraft:polished_andesite
fill -1 -61 -13 1 -61 1 minecraft:dirt_path replace minecraft:grass_block
fill -13 -61 -1 -8 -61 1 minecraft:dirt_path replace minecraft:grass_block
fill -20 -61 -13 -18 -61 -5 minecraft:dirt_path replace minecraft:grass_block
fill 8 -61 -1 28 -61 0 minecraft:dirt_path replace minecraft:grass_block
fill 26 -61 -13 28 -61 -2 minecraft:dirt_path replace minecraft:grass_block
fill 17 -61 1 19 -61 7 minecraft:dirt_path replace minecraft:grass_block
setblock -5 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Extra",bold:true,color:"dark_green"},"Recipes: west","recipes, ores,","bamboo paper"]}}
setblock -3 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Graveyard",bold:true,color:"dark_red"},"north-west:","sleepers, wool","pen and crypt"]}}
setblock -1 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Test World",bold:true,color:"black"},"Night forever,","no mob spawns,","keep inventory"]}}
setblock 1 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Loot Display",bold:true,color:"dark_purple"},"straight ahead","4 placements,","loot grabbing"]}}
setblock 3 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Combat Arena",bold:true,color:"dark_red"},"north-east:","Smite, arrows,","armor shred"]}}
setblock 5 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Glider Tower",bold:true,color:"dark_aqua"},"east ->","glider vs","elytra"]}}
setblock 7 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Infused Gems",bold:true,color:"dark_purple"},"east, past the","tower: brew,","enchant, socket"]}}
setblock -7 -60 3 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Creepers",bold:true,color:"dark_green"},"far east, past","the gems: the","creeper grove"]}}
setblock -7 -60 5 minecraft:oak_sign[rotation=0]{allow_op_features:1b,front_text:{messages:[{text:"Cairns",bold:true,color:"dark_purple",click_event:{action:"run_command",command:"tp @s -206.5 -60 26.5 180 0"}},{text:"and endermen,"},{text:"far west"},{text:"click to go",color:"gray"}]}}
setblock 7 -60 5 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Fresh copy?",bold:true,color:"gray"},"Run install-to-","modrinth with","-ResetWorld"]}}
setblock -6 -56 2 minecraft:light[level=15]
setblock -6 -56 8 minecraft:light[level=15]
setblock 0 -56 2 minecraft:light[level=15]
setblock 0 -56 8 minecraft:light[level=15]
setblock 6 -56 2 minecraft:light[level=15]
setblock 6 -56 8 minecraft:light[level=15]
