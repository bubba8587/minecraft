# @dummy
# The holding_flint predicate matches a player holding flint and not one holding nothing.
assert not predicate refpack:holding_flint
item replace entity @s weapon.mainhand with flint
assert predicate refpack:holding_flint
