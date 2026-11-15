# Glider tower: hollow 5x5 stone brick tower x 20..24, z 4..8, platform on top at y -1.
fill 20 -60 4 24 -2 8 minecraft:stone_bricks outline
fill 21 -60 5 23 -2 7 minecraft:air
fill 20 -60 6 20 -59 6 minecraft:air
fill 19 -1 3 25 -1 9 minecraft:oak_planks
fill 23 -60 6 23 -1 6 minecraft:ladder[facing=west]
setblock 19 -60 5 minecraft:oak_sign[rotation=4]{is_waxed:1b,front_text:{messages:[{text:"Glider Tower",bold:true,color:"dark_aqua"},"Climb the","ladder inside;","chest on top"]}}
setblock 21 0 6 minecraft:chest[facing=east]{CustomName:"Wings",Items:[{Slot:0b,id:"bubbas_crafting_tweaks:glider",count:1},{Slot:1b,id:"minecraft:elytra",count:1},{Slot:2b,id:"minecraft:firework_rocket",count:64},{Slot:3b,id:"minecraft:firework_rocket",count:64,components:{"minecraft:fireworks":{flight_duration:2}}},{Slot:4b,id:"bubbas_crafting_tweaks:glider",count:1},{Slot:5b,id:"minecraft:elytra",count:1}]}
setblock 21 0 4 minecraft:oak_sign[rotation=0]{is_waxed:1b,front_text:{messages:[{text:"Glide!",bold:true,color:"dark_aqua"},"Wear one, jump","off, press jump","to glide"]}}
setblock 21 0 8 minecraft:oak_sign[rotation=8]{is_waxed:1b,front_text:{messages:[{text:"Glider",bold:true,color:"dark_aqua"},"turns slower,","weaker rockets,","never breaks"]}}
setblock 22 -58 6 minecraft:light[level=15]
setblock 22 -50 6 minecraft:light[level=15]
setblock 22 -42 6 minecraft:light[level=15]
setblock 22 -34 6 minecraft:light[level=15]
setblock 22 -26 6 minecraft:light[level=15]
setblock 22 -18 6 minecraft:light[level=15]
setblock 22 -10 6 minecraft:light[level=15]
setblock 19 2 3 minecraft:light[level=15]
setblock 19 2 9 minecraft:light[level=15]
setblock 25 2 3 minecraft:light[level=15]
setblock 25 2 9 minecraft:light[level=15]
