# A powered crafter turns 8 sand around an iron nugget into 8 red sand.
setblock ~1 ~1 ~1 minecraft:crafter[orientation=up_north]{Items:[{Slot:0b,id:"minecraft:sand",count:1},{Slot:1b,id:"minecraft:sand",count:1},{Slot:2b,id:"minecraft:sand",count:1},{Slot:3b,id:"minecraft:sand",count:1},{Slot:4b,id:"minecraft:iron_nugget",count:1},{Slot:5b,id:"minecraft:sand",count:1},{Slot:6b,id:"minecraft:sand",count:1},{Slot:7b,id:"minecraft:sand",count:1},{Slot:8b,id:"minecraft:sand",count:1}]}
setblock ~2 ~1 ~1 minecraft:redstone_block
await entity @e[type=item,distance=..6,nbt={Item:{id:"minecraft:red_sand",count:8}}]
