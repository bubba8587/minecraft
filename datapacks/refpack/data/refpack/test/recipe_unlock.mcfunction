# @dummy
# Recipes are not auto-unlocked; `recipe give` unlocks one and returns the count.
execute store result score #recipes refpack.counter run recipe give @s refpack:flint_block_shaped
assert score #recipes refpack.counter matches 1
