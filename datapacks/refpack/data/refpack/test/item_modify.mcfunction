# @dummy
# The named_relic item modifier sets a custom name and custom data on the held item.
item replace entity @s weapon.mainhand with minecraft:flint
item modify entity @s weapon.mainhand refpack:named_relic
assert entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{refpack:{relic:1b}}}}}]
