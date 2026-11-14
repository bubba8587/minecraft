# @dummy
# `swing` returns the number of living entities swung.
execute store result score #swing refpack.counter run swing @s offhand whack
assert score #swing refpack.counter matches 1
