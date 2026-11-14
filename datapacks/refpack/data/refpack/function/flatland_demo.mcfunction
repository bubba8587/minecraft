# Places the custom feature in the custom flat dimension.
# Chunk 0 0 is forceloaded by refpack:load.
execute in refpack:flatland run place feature refpack:amethyst_rock 0 5 0
execute in refpack:flatland if block 0 5 0 minecraft:amethyst_block run say rock placed
