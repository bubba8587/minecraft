<!-- dte:A2,A1,A4 -->
<!-- covers: docs/knowledge/* -->
# Knowledge base

`docs/knowledge/` holds reference notes on Minecraft, Fabric, packs, libraries and publishing for the target version, indexed by `docs/knowledge/README.md`.

- Each note starts with its scope and the version it was verified against.
- API claims point at the source file they came from; a claim that could not be checked says `UNVERIFIED`.
- When a note and the decompiled source disagree, the source wins, and the note is fixed in the same change.

## Agents check API names against the target version's decompiled source, not memory

Before relying on a Minecraft or Fabric API name, an agent checks it against the decompiled source of the target version (`./gradlew :<mod>:genSources`, then `python3 tools/mcsrc.py` unpacks it into `build/mcsrc/`) or the Fabric API branch named after that version. The knowledge base in `docs/knowledge/` is written the same way.
