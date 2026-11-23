<!-- dte:C3 -->
# Comments to labels: Extra Recipes (builder: agent 3)

Branch `comments-1`. B38: every comment in `mods/bubbas_extra_recipes/src` (main, client, gametest) is now a short WHAT label, or gone. `dte:` lines are kept.
- Spec: `specs/extra-recipes-mod.md` gains a "Technique notes" list under Synthetic Diamond Ore. It covers why the mixin wraps the state-definition call, why the default must be forced to natural, the no-registry rule for `SyntheticOre`, and why the handshake exists. Everything else in the old comments was already in the spec.
- Comment-only check: all 17 changed files are identical to before once comments and whitespace are stripped. Core: 4/4 pass.
