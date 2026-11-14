<!-- dte:B5,B6 -->
<!-- covers: mods/refmod/*, datapacks/refpack/* -->
# Reference projects

`mods/refmod` and `datapacks/refpack` exist to prove the toolchain and the knowledge base against the real game. They are never published.

`refmod` uses each major system the knowledge base describes, the way the notes say to, and has server game tests for each and a client game test with a screenshot. When it stops compiling or a test fails after a version bump, the notes for that system are stale.

`refpack` covers pack.mcmeta, function tags, plain and macro functions, command storage, recipes, predicates and advancements, with PackTest test functions for each, and must load clean on the vanilla server (`tools/packload.py`).
