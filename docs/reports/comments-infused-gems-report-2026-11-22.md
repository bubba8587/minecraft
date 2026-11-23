<!-- dte:C4 -->
# Comments to labels: Infused Gems (builder: agent 3)

Branch `comments-1`. B38: every comment in `mods/bubbas_infused_gems/src` (main, client, gametest; 93 files) is now a short WHAT label, or gone. `dte:` lines are kept. Dated "(owner, …)" notes and "Owner:" line comments are removed. Assertion messages inside tests are code, so they are unchanged.
- Spec (`specs/effect-gems.md`) gains "Smaller rules" before "Not done yet":
  - Hero gems leave gifts vanilla's;
  - Breeze veer's base spread (vanilla's 0.0172275 per unit of uncertainty);
  - unknown modded armor and weapons count as iron;
  - when the local client recomputes its totals.
- Everything else in the old comments was already in the spec, which is very thorough.
- One stale comment was dropped, not moved: "Poison and Wither drawbacks roll half as often". The spec's rarity weights replaced that rule.
- Comment-only check: all 93 changed files are identical to before once comments and whitespace are stripped. Core: 9/9 pass.
