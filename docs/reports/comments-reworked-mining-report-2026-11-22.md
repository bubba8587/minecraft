<!-- dte:C7 -->
# Comments to labels: Reworked Mining (builder: agent 3)

Branch `comments-1`. B38: every comment in `mods/bubbas_reworked_mining/src` (main, client, gametest) is now a short WHAT label, or gone. That includes the dated "Owner:" notes in `MiningTuning`; the spec already holds each of those facts in its tables. `dte:` lines are kept.
- Spec: `specs/reworked-mining.md` gains one line under "How it's built": the curve leaves speed alone when the tool isn't meant for the block, and untiered tools stay vanilla. Everything else in the old comments was already in the spec.
- Two labels that said "natural stone" now say "stone", since placed blocks count too (owner).
- Comment-only check: all 13 changed files are identical to before once comments and whitespace are stripped. Core: 6/6 pass.
