# @timeout 400
# A blast furnace turns cobbled deepslate into tuff.
setblock ~1 ~1 ~1 minecraft:blast_furnace{Items:[{Slot:0b,id:"minecraft:cobbled_deepslate",count:1},{Slot:1b,id:"minecraft:coal",count:1}]}
await items block ~1 ~1 ~1 container.2 minecraft:tuff
