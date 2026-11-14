# Macro data modify writes a value into storage; return run passes the dice value through.
function refpack:set_state {key:"mode",value:'"test"'}
assert data storage refpack:state {mode:"test"}
execute store result score #dice refpack.counter run function refpack:dice
assert score #dice refpack.counter matches 1..6
