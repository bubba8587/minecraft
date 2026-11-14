# Runs on world load and /reload.
scoreboard objectives add refpack.counter dummy
scoreboard players set #loads refpack.counter 0
scoreboard players add #loads refpack.counter 1
# Keep the chunk at 0 0 loaded in both test dimensions. Forceload loads chunks over the next
# ticks, so doing it here at startup means it is ready before any check command runs.
forceload add 0 0
execute in refpack:flatland run forceload add 0 0
