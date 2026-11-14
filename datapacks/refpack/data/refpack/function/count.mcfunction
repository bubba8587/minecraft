# Stores into command storage and reads it back.
data modify storage refpack:state last set value "counted"
execute store result storage refpack:state loads int 1 run scoreboard players get #loads refpack.counter
data get storage refpack:state
