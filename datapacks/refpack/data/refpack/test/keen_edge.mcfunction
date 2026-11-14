# @dummy
# The custom enchantment applies to shears via the refpack:keen_edge_enchantable tag.
item replace entity @s weapon.mainhand with minecraft:shears
enchant @s refpack:keen_edge 2
execute store result score #keen refpack.counter if items entity @s weapon.mainhand *[enchantments~[{enchantments:"refpack:keen_edge",levels:2}]]
assert score #keen refpack.counter matches 1
