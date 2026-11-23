# Suspicious sand is not accepted: the crafter keeps both items (a craft would use them up).
setblock ~1 ~1 ~1 minecraft:crafter[orientation=up_north]{Items:[{Slot:0b,id:"minecraft:mud",count:1},{Slot:1b,id:"minecraft:suspicious_sand",count:1}]}
setblock ~2 ~1 ~1 minecraft:redstone_block
await delay 20
assert items block ~1 ~1 ~1 container.0 minecraft:mud
assert items block ~1 ~1 ~1 container.1 minecraft:suspicious_sand
