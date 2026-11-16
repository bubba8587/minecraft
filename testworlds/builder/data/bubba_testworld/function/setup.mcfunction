# World rules and the area the stations use (tools/build-testworld.py waits after forceload add).
forceload add -48 -48 47 95
gamerule spawn_mobs false
gamerule spawn_phantoms false
gamerule spawn_patrols false
gamerule spawn_wandering_traders false
gamerule advance_time false
gamerule advance_weather false
gamerule respawn_radius 0
gamerule keep_inventory true
time set midnight
weather clear
setworldspawn 0 -60 7 180 0
