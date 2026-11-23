# @timeout 400
# A blast furnace turns a bone block into calcite.
setblock ~1 ~1 ~1 minecraft:blast_furnace{Items:[{Slot:0b,id:"minecraft:bone_block",count:1},{Slot:1b,id:"minecraft:coal",count:1}]}
await items block ~1 ~1 ~1 container.2 minecraft:calcite
