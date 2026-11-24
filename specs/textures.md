<!-- dte:B16 -->
<!-- covers: mods/*/src/main/resources/assets/*/textures/*, mods/*/src/main/resources/resourcepacks/*, resourcepacks/* -->
# Textures: 16x and 32x

We make our own textures for everything we add, in both resolutions. Classic Faithful 32x Jappa (the owner's pack, owner 2026-11-23; it replaced Faithful 32x as the reference) is the style reference for the 32x set, not a dependency: whether that pack covers our content is never a design consideration. 32x art drawn earlier in Faithful 32x style is restyled over time.

## The two sets

- Every custom texture exists twice: a **16x** version in vanilla style and a **32x** version in the style of Classic Faithful 32x Jappa.
- Each 32x texture is exactly twice the width and height of its 16x twin, at the same path.
- **Animations and metadata:** a `.png.mcmeta` next to a 16x texture has a matching one next to its 32x twin; frame counts and timing match.

## Where they live

- **Mods:**
  - The 16x set is the mod's normal assets: `src/main/resources/assets/<modid>/textures/...`.
  - The 32x set is a built-in resource pack inside the same jar, at `src/main/resources/resourcepacks/<modid>_32x/`, with its own `pack.mcmeta` and the same `assets/<modid>/textures/...` paths.
  - The mod registers it on the client with `ResourceLoader.registerBuiltinPack(<modid>:<modid>_32x, container, <display name>, PackActivationType.NORMAL)`. The display name ends in "32x (Faithful style)" (e.g. "Bubba's Infused Gems 32x (Faithful style)"). Players switch it on in the Resource Packs screen, above their 32x pack (Classic Faithful 32x Jappa or Faithful 32x) if they use one. The display names still say "(Faithful style)" until the packs are restyled.
- **Resource pack projects:** `resourcepacks/<name>/` is the 16x pack and `resourcepacks/<name>-32x/` the 32x pack, released together.

## Checking

`python3 tools/texcheck.py` fails when:
- a 16x texture has no 32x twin;
- a twin is not exactly double size;
- the animation metadata doesn't match.

Projects with `internal=true` in `gradle.properties` (refmod) are skipped; their textures are placeholders.

## Credit

A project with Classic Faithful- or Faithful-derived textures has a `CREDITS.md` naming what was used and linking the pack's site (Faithful: https://faithfulpack.net); check that pack's license terms before release. Its player docs page and Modrinth listing carry the same credit.

## Who makes them

- **During development** agents may make the art they need (scripted or AI-made), and give every texture their best shot: vanilla-fitting, shaded, checked in screenshots at 16x and 32x (owner, CLAUDE.md). Every agent-made asset gets a row in `docs/art-todo.md`, so the owner can redo or tweak it.
- **For a Modrinth release** (B14, B16): no texture may be AI-generated where Modrinth's rules forbid it. Person-made textures are the most natural part to carry a project over Modrinth's human-made bar.
