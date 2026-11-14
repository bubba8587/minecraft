# Uses the custom registry entries from commands, so a broken reference fails here.
# Chunk 0 0 is forceloaded by refpack:load.
setblock 1 300 0 minecraft:chest
item replace block 1 300 0 container.0 with minecraft:shears[minecraft:enchantments={"refpack:keen_edge":2}]
item replace block 1 300 0 container.1 with minecraft:music_disc_cat[minecraft:jukebox_playable="refpack:flint_tune"]
item replace block 1 300 0 container.2 with minecraft:iron_chestplate[minecraft:trim={material:"refpack:flint",pattern:"refpack:shard"}]
item replace block 1 300 0 container.3 with minecraft:stick[minecraft:block_transformer="refpack:chisel"]
item replace block 1 300 0 container.5 with minecraft:painting[minecraft:painting/variant="refpack:flint_still_life"]
execute store result storage refpack:state keen int 1 if items block 1 300 0 container.* *[enchantments~[{enchantments:"refpack:keen_edge",levels:2}]]
