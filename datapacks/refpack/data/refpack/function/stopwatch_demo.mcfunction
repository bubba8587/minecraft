# A 26.x real-time stopwatch: create, query in milliseconds, test, remove.
stopwatch create refpack:timer
stopwatch query refpack:timer 1000
execute if stopwatch refpack:timer 0.. run say stopwatch running
stopwatch remove refpack:timer
