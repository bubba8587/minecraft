# Cobweb field x -23..-17, z 38..46: a cobweb lane (x -23..-21, 2 high) beside an open lane (x -19..-17).
fill -23 -60 38 -21 -59 46 minecraft:cobweb
fill -20 -60 38 -20 -60 46 minecraft:stone_brick_wall
setblock -22 -60 36 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Cobweb Field",bold:true,color:"dark_green"},{text:"walk the web"},{text:"lane, then the"},{text:"open one"}]}}
setblock -21 -60 36 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Weaving",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"perfect set",click_event:{action:"run_command",command:"gems kit weaving"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"faster webs",color:"gray"}]}}
setblock -23 -60 36 minecraft:birch_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Weaving",bold:true,color:"dark_green",click_event:{action:"run_command",command:"item override entity @s armor.* with minecraft:air"}},{text:"Gem Power V 88%",click_event:{action:"run_command",command:"gems kit weaving netherite 5"}},{text:"+ sword",click_event:{action:"run_command",command:"playsound minecraft:item.armor.equip_netherite player @s ~ ~ ~ 1 1"}},{text:"faster webs",color:"dark_gray"}]}}
setblock -20 -60 36 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Reset webs",bold:true,color:"dark_green",click_event:{action:"run_command",command:"fill -23 -60 38 -21 -59 46 minecraft:cobweb replace minecraft:air"}},{text:"puts every web"},{text:"back"},{text:""}]}}
setblock -18 -60 36 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Open Lane",bold:true,color:"dark_green"},{text:"same length,"},{text:"no webs"},{text:""}]}}
setblock -19 -60 36 minecraft:oak_sign[rotation=8]{allow_op_features:1b,front_text:{messages:[{text:"Shears",bold:true,color:"dark_green",click_event:{action:"run_command",command:"give @s minecraft:shears"}},{text:"cut webs"},{text:"quickly"},{text:""}]}}
setblock -22 -56 38 minecraft:light[level=15]
setblock -22 -56 44 minecraft:light[level=15]
setblock -16 -56 38 minecraft:light[level=15]
setblock -16 -56 44 minecraft:light[level=15]
