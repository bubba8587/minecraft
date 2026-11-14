# Summons a pig, swings its main hand with the 26.3 stab animation (returns 1), removes it.
# Chunk 0 0 is forceloaded by refpack:load.
summon minecraft:pig 0 300 2 {NoAI:1b,Tags:["refpack.swing"]}
execute store result storage refpack:state swung int 1 run swing @e[type=minecraft:pig,tag=refpack.swing] mainhand stab 10t
kill @e[type=minecraft:pig,tag=refpack.swing]
