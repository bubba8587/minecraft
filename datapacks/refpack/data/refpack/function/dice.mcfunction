# Rolls 1..6 from a named random sequence, stores it and returns it.
execute store result storage refpack:state dice int 1 run random value 1..6 refpack:dice
return run data get storage refpack:state dice
