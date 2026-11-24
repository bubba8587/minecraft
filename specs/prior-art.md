<!-- dte:A1,A2 -->
# Prior art

## Reuse existing mods, libraries and tools before building anything new

Before designing a mod, pack or tool, an agent searches Modrinth (`python3 tools/prior-art.py "<idea>"`) and GitHub for existing projects that already do it or part of it.

- Existing libraries and tools are used instead of re-implementing them (B9 libsOverHandroll; `docs/knowledge/libraries.md`).
- Existing open-source mods are studied for how they solve the same problem (`docs/knowledge/reference-mods.md`), minding their license.
- A new project that mostly duplicates an existing one is flagged to the owner before work starts, saying what ours would add.
