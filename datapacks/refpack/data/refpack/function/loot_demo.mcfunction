# Fills a chest from the ref_chest loot table, renames slot 0 with an item modifier,
# Chunk 0 0 is forceloaded by refpack:load.
# then counts relic items with `execute if items`. Returns the relic count.
setblock 0 300 0 minecraft:chest
loot replace block 0 300 0 container.0 loot refpack:chests/ref_chest
item replace block 0 300 0 container.26 with minecraft:flint 3
item modify block 0 300 0 container.26 refpack:named_relic
return run execute if items block 0 300 0 container.* *[custom_data~{refpack:{relic:1b}}]
