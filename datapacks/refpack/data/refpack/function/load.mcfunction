# Runs on world load and /reload.
scoreboard objectives add refpack.counter dummy
scoreboard players set #loads refpack.counter 0
scoreboard players add #loads refpack.counter 1
