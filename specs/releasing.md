<!-- dte:A1,A2,B14 -->
# Releasing

## Pushing a version tag publishes that release from CI

- A release is published by pushing a git tag of the form `<project>/v<version>`. Nothing is uploaded by hand.
- CI runs the full suite (core, milestone and client tests), then publishes (once the publish step is wired in; see Status):
  - to **Modrinth**, if the project meets Modrinth's generative-AI rules in `docs/knowledge/publishing.md` (B14 modrinthAiCompliant): the "Contains AI-generated content" disclosure on, no AI-generated icon, gallery or page image, and a substantial human-made part;
  - otherwise to **GitHub Releases**, until it qualifies.
- Before a project's first Modrinth release, the owner is told which human-made parts carry it over the bar.

## Releases come from `main`

Work happens on `develop`; `main` is the released state and never gets direct commits (CLAUDE.md "Branch model"). To release:

1. Run the release milestone checks on `develop` (`specs/quality.md`).
2. Merge `develop` into `main` and push `main`.
3. Tag `<project>/v<version>` on `main` and push the tag.

## Status

- `.github/workflows/build.yml` runs the full suite on release tags. Its `mod-release` job then makes a GitHub Release with the mod's jar (notes from the mod's `CHANGELOG.md`), and uploads to Modrinth when the mod's `gradle.properties` sets `modrinth_project` and the `MODRINTH_TOKEN` secret exists.
- No release tag exists yet, so nothing has been released.

## Modrinth upload (Minotaur)

- The official Modrinth Gradle plugin, `com.modrinth.minotaur` 2.10.0, applied to every published mod by `gradle/fabric-mod.gradle`. Task `:<mod>:modrinth` (run it with `--no-configuration-cache`).
- Per-mod `gradle.properties` keys:
  - `modrinth_project`: the project's slug or id. **Left unset until the owner clears the mod for Modrinth (B14)**; while unset, the upload is skipped and the mod only goes to GitHub Releases.
  - `modrinth_required`, `modrinth_optional`: comma-separated Modrinth slugs of dependencies besides Fabric API (always required). Only list projects that already exist on Modrinth, or the upload fails.
  - `modrinth_version_type`: `release` (default), `beta` or `alpha`.
- Version number `<mod_version>+<minecraft>`, name "`<mod_version>` for `<minecraft>`", loader fabric, environment client and server, the uploaded file is `jar` (26.x is unobfuscated, no `remapJar`).
- `mods/<mod>/CHANGELOG.md`: the section under `## <mod_version>` becomes the version's changelog.
- `mods/<mod>/MODRINTH.md`: the project page body, synced to Modrinth on every upload (`modrinthSyncBody`; overwrites whatever is on the page). Written from the player docs, leading with the owner's strings from `specs/copy-strings.md`; no images in the body.
- Dry run: `./gradlew :<mod>:modrinth -Pmodrinth_project=<any existing slug> -PmodrinthDryRun --no-configuration-cache --info` prints the payload and uploads nothing (body sync is skipped too). Checked 2026-11-25 with World Tweaks.

## First Modrinth release of a project (owner's steps)

1. Make the mod icon (a person, never AI: B14). Square PNG; it goes on the Modrinth page and, optionally, in the jar at `assets/<modid>/icon.png`.
2. Create the project in the Modrinth web UI: title, slug, summary, icon, license MIT, categories, source and issues links (https://github.com/bubba8587/minecraft), gallery images, and the content disclosures with **"Contains AI-generated content" on**.
3. Tell the agent the slug; it goes in the mod's `gradle.properties` as `modrinth_project`.
4. Add a Modrinth personal access token to the GitHub repository secrets as `MODRINTH_TOKEN`, with scopes Create versions, Read projects and Write projects.
5. Release: merge `develop` into `main`, tag `<mod>/v<version>` on `main` and push (or create the release tag on GitHub). CI tests, uploads the version, and syncs the page body.
6. Submit the project for review in the web UI.

## Release prep: first wave (2026-11-25)

The four smaller mods go first, to prove the pipeline. Reworked Mobs, Totems & Artifacts and Infused Gems follow (their before-release items are in `specs/STATUS.md` and `BACKLOG.md`).

| | Extra Recipes (mod) | World Tweaks | Reworked Mining | Reworked Beacons |
|---|---|---|---|---|
| Suggested slug | `bubbas-extra-recipes` | `bubbas-world-tweaks` | `bubbas-reworked-mining` | `bubbas-reworked-beacons` |
| Summary (owner's wording) | Adds a handful of vanilla-esque extra recipes to add gameplay loop depth and automation and reduce the grind. | Magma blocks below Lava create bubbles and powerful black smoke geysers. | Overhauled tool progression, rare finds in stone, True Unbreaking and quartz gear. (agent draft: needs the owner's OK) | Beacons switch from consumed ingots to an un-consumed set of gems, affect everything in range, and pair up underground. (agent draft: needs the owner's OK) |
| Categories | game-mechanics, utility | game-mechanics, transportation | equipment, game-mechanics | game-mechanics, utility |
| Gallery (posters, `build/posters/`) | `p11_extra_recipes.png` | `p10_world_tweaks.png` | `p09_mining.png` | `p08_beacons.png` |
| Gallery (plain shots, `mods/postershots/build/run/posters/shots/`) | `blast.png`, `glideK.png` | `gyw_5.png`, `gyw_7.png` | `mineB_3.png`, `qtzC0.png` | `pcD.png`, `pcF.png`, `bcE.png` |
| Human-made part (owner to confirm) | The recipe set is the owner's own datapack, ported to 26.3; the Glider and Synthetic Diamond Ore are the owner's design. | Owner's design (bubbles, vents, lift for the Glider), tuned in play (bubble rate). | Owner's design: the tool curve, deepslate, rare finds, True Unbreaking, quartz gear. | Owner's design: the item slot, equal effects, paired and upside-down beacons. |
| Pre-release checks | server + client milestone suites | same | same | same; plus testing against other beacon mods (BACKLOG) |
| Mod icon | none yet | none yet | none yet | none yet |

- **Gallery images:** the posters are composed by script from real in-game screenshots, vanilla textures and the game font; no image generator is involved. They do show agent-made textures (B14 disclosure covers the content). Plain screenshots are the safest lead image.
- **Extra Recipes datapack:** the same Modrinth project can carry the datapack as separate versions (loader `datapack`); the datapack still releases to GitHub only (`pack-release` skips names that are also mods). Wire a datapack upload before its first Modrinth version.
- **Missing icons:** World Tweaks' and Reworked Mining's `fabric.mod.json` point at an `icon.png` that doesn't exist yet; harmless (no icon in the mod list) until the owner's icon lands.

## Pack releases

- Pack projects (`datapacks/`, `resourcepacks/`) publish to GitHub Releases from CI (`pack-release` job) on a tag `<name>/v<version>` matching the pack's `VERSION`, after the mod, client-test and datapack jobs pass. Mods publish from the `mod-release` job (Status, above).
- `bubbas_32x` (Bubba's Mods 32x) goes to GitHub Releases first; it can't go on Modrinth until it meets B14.
