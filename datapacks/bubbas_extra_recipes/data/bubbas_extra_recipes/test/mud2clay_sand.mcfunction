# A powered crafter turns 1 mud + 1 sand into 4 clay balls.
setblock ~1 ~1 ~1 minecraft:crafter[orientation=up_north]{Items:[{Slot:0b,id:"minecraft:mud",count:1},{Slot:4b,id:"minecraft:sand",count:1}]}
setblock ~2 ~1 ~1 minecraft:redstone_block
await entity @e[type=item,distance=..6,nbt={Item:{id:"minecraft:clay_ball",count:4}}]
