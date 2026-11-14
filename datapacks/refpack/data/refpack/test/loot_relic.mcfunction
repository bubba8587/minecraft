# @dummy
# The ref_chest loot table always gives a named relic shard, found with `execute if items`.
loot give @s loot refpack:chests/ref_chest
execute store result score #relics refpack.counter if items entity @s container.* minecraft:amethyst_shard[custom_data~{refpack:{relic:1b}}]
assert score #relics refpack.counter matches 1..
