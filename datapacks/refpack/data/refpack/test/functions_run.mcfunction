# Plain and macro functions run, and load set up the scoreboard.
assert score #loads refpack.counter matches 1..
execute store result score #ret refpack.counter run function refpack:hello
assert score #ret refpack.counter matches 1
function refpack:greet {name:"Steve"}
assert chat "Hello, Steve!"
