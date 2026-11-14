# Evaluates the chest_rolls context_int_provider by ID and inline (26.3 /compute).
data modify storage refpack:state rolls set compute default integer refpack:chest_rolls
execute store result score #rolls refpack.counter run compute default integer {type:"minecraft:add",inputs:[1,2]}
return run scoreboard players get #rolls refpack.counter
