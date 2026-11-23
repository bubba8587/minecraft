# Red sand works too, in any grid position.
setblock ~1 ~1 ~1 minecraft:crafter[orientation=up_north]{Items:[{Slot:2b,id:"minecraft:red_sand",count:1},{Slot:6b,id:"minecraft:mud",count:1}]}
setblock ~2 ~1 ~1 minecraft:redstone_block
await entity @e[type=item,distance=..6,nbt={Item:{id:"minecraft:clay_ball",count:4}}]
