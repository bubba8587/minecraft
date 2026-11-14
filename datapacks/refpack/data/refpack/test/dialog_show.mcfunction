# @dummy
# The welcome dialog can be shown to and cleared from a player.
execute store result score #dialog refpack.counter run dialog show @s refpack:welcome
assert score #dialog refpack.counter matches 1
dialog clear @s
