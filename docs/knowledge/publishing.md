# Publishing and shipping (Modrinth, CurseForge, GitHub, CI, multi-version)

Scope: getting a Fabric mod, datapack or resource pack from this repo onto Modrinth (and optionally CurseForge or GitHub Releases), with CI, versioning, licensing and moderation rules. Verified 2026-11-14 against Minecraft 26.3, Loom 1.19.4, Gradle 9.8.2, the live Modrinth API v2, Modrinth's moderation source (`modrinth/code` `packages/moderation`), Fabric API 26.3 build scripts, and the Gradle plugin portal.

Repo context: `mods/<modid>/` subprojects share `gradle/fabric-mod.gradle`. Version is already `${mod_version}+${minecraft_version}`, so for example `0.1.0+26.3`. No publishing plugin is applied yet, and `fabric.mod.json` says `All-Rights-Reserved` until the owner answers `decisions/inbox/license.md`. **Do not publish anything until the license and author decisions are made.**

---

## 0. TL;DR recommendations

| Need | Use | Version (verified 2026-11-14) |
|---|---|---|
| Publish from Gradle to Modrinth, CurseForge and GitHub | `me.modmuss50.mod-publish-plugin` (Fabric API itself uses it) | **2.2.1** |
| Official Modrinth-only Gradle plugin | `com.modrinth.minotaur` | **2.10.0** (2026-09-16; adds the `environment` field) |
| GitHub Action instead of Gradle | `Kir-Antipov/mc-publish` | v3.3.2. Works, but Modrinth gives no support for it |
| Multi-version from one source tree | Stonecutter `dev.kikugie.stonecutter` with `dev.kikugie.loom-back-compat` | **0.10** and **0.4.3** |
| CI actions | `actions/checkout@v7`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v7`, `actions/download-artifact@v8`, `softprops/action-gh-release@v3` | latest tags from `git ls-remote` |

On 26.x, **the file to upload is the output of `jar`, not `remapJar`.** Loom 1.19 with `net.fabricmc.fabric-loom` does no remapping. Every old guide, and Minotaur's own README, still says `remapJar`. (src: Loom `task/prod/AbstractProductionRunTask.java`: "if dontRemapOutputs → jar task")

---

## 1. Modrinth concepts

### 1.1 Project types
`GET /v2/tag/project_type` currently returns `["mod","modpack","resourcepack","shader","plugin","datapack","minecraft_java_server"]`. `minecraft_java_server` is new: server listings with their own category set.

**The project type is derived from the loaders on your versions, not set directly.** `POST /project` accepts only `project_type: "mod" | "modpack"`.
- A version whose only loader is `datapack` gives a **datapack** project.
- A version whose only loader is `minecraft` gives a **resourcepack** project.
- Versions with `iris`, `optifine`, `canvas` or `vanilla` give a **shader** project.
- `.mrpack` files give a **modpack** project.
- Loaders such as `paper`, `spigot` or `velocity` give a **plugin** project.
- If you mix in `fabric` or another mod loader, the project shows as **mod**. Terralith ships `datapack` + `fabric` + `neoforge` versions and is `project_type: "mod"`, with `loaders: ["datapack","fabric","forge","neoforge","quilt"]`.

Moderation template `versions/incorrect-project-type/datapack.md`: "Data Packs must only be uploaded with the 'Data Pack' loader selected." That applies when you intend a pure datapack project. Selecting `fabric` on a zip by mistake reclassifies the project.

Loader tags and the project types they allow (`GET /v2/tag/loader`):
- `fabric`: mod, modpack
- `quilt`: mod, modpack
- `neoforge`: mod, modpack
- `forge`: mod, modpack
- `datapack`: datapack, mod
- `minecraft`: resourcepack (this is the "Resource Pack" loader)
- `iris`, `optifine`, `canvas`, `vanilla`: shader
- `paper`, `spigot`, `bukkit`, `folia`, `purpur`, `sponge`, `velocity`, `waterfall`, `bungeecord`, `geyser`: plugin
- Legacy and other: `legacy-fabric`, `ornithe`, `babric`, `bta-babric`, `java-agent`, `liteloader`, `modloader`, `nilloader`, `rift`

### 1.2 Game version tags
`GET /v2/tag/game_version` already lists `26.3` (release, 2026-09-15), plus `26.3-rc-N`, `26.3-pre-N`, `26.3-snapshot-N` and `26.4-snapshot-1..8`. The format is year-based and the strings are exactly those. Do not invent `1.26.3`.

### 1.3 Categories (live `GET /v2/tag/category`)
- **mod** (datapacks use these too; there is no separate datapack category set): adventure, cursed, decoration, economy, equipment, food, game-mechanics, library, magic, management, minigame, mobs, optimization, social, storage, technology, transportation, utility, worldgen
- **resourcepack categories**: combat, cursed, decoration, modded, realistic, simplistic, themed, tweaks, utility, vanilla-like
- **resourcepack features**: audio, blocks, core-shaders, entities, environment, equipment, fonts, gui, items, locale, models
- **resourcepack resolutions**: 8x-, 16x, 32x, 48x, 64x, 128x, 256x, 512x+
- **modpack**: adventure, challenging, combat, kitchen-sink, lightweight, magic, multiplayer, optimization, quests, technology
- **shader**: has its own categories, features and performance-impact headers

Loaders are shown as categories in search results. That is display only; do not put `fabric` in `categories`. `categories` holds the featured tags (up to 3 in the UI); the rest go in `additional_categories`.

### 1.4 Environment: the new system (it replaces client_side/server_side)
Announced 2025-08-28 ("new environments"). It is now visible in API v2:
- The **project** object has `environment: [...]`, an array computed across versions. For example `fabric-api` gives `["client_or_server_prefers_both"]`, `sodium` gives `["client_only"]`, and `terralith` gives `["server_only"]`.
- The **version** object has `environment: "<value>"`. It defaults to `"unknown"` until it is set.
- `client_side` and `server_side` (`required|optional|unsupported|unknown`) are still returned but are **deprecated** in the create-project docs ("use environment").
- Environment is **set per version**. The moderation message links to `/project/<id>/settings/versions` and offers "apply environment to all versions".

Values (version `environment`; also the Minotaur `environment`; mod-publish-plugin uses upper-case enum names such as `CLIENT_ONLY`):

| value | meaning | typical for |
|---|---|---|
| `client_only` | works on the client and with vanilla servers | HUD, rendering, keybind, resource-only mods |
| `server_only` | server only, no client install; also works in singleplayer | datapacks, worldgen, server-logic mods that use only vanilla content |
| `dedicated_server_only` | dedicated servers only, not singleplayer | admin tools |
| `client_and_server` | required on both sides | anything that registers blocks, items or entities, or uses custom payloads |
| `server_only_client_optional` | server required; client adds extras | |
| `client_only_server_optional` | client required; server adds extras | |
| `client_or_server_prefers_both` | works on either side alone, best on both | libraries like Fabric API |
| `client_or_server` | either side, no benefit from both | |
| `singleplayer_only` | singleplayer only | |

Gotchas:
- A mod that registers new content (blocks, items, entity types) or registry entries that sync is `client_and_server`. Registry sync rejects clients without it.
- A datapack is `server_only`.
- A resource pack is not required to set an environment. `requiresEnvironmentInfo` is true only for project types `mod` and `modpack` (src: moderation `utils.ts`).
- The moderators' checklist has "Environment Metadata inaccurate" and "mixed" issue templates, so a wrong environment is a review finding.

### 1.5 Licenses
- `license_id` must be an SPDX id or `LicenseRef-<Something>`. `GET /v2/tag/license` lists the SPDX set; `GET /v2/tag/license/MIT` returns the text.
- Custom or proprietary licenses use `LicenseRef-...` plus `license_url`. Examples seen live: `LicenseRef-Polyform-Shield-1.0.0`, `LicenseRef-See-Terms-of-Use-in-Description`.
- "All Rights Reserved" is `LicenseRef-All-Rights-Reserved` on Modrinth. In `fabric.mod.json` the `license` field is free text. The repo currently uses `"All-Rights-Reserved"`; that is fine for Fabric but is not an SPDX id. Keep the two consistent: moderation rule 5.1 says the license must match "information elsewhere".
- The moderation "Invalid license link" issue fires if `license_url` does not lead directly to license text. A nag also warns if the URL is "not in allowlist" (known license hosts).

**Licenses that trigger a mandatory source check** (src: moderation `utils.ts` `licensesRequiringSource`). For project types `mod` and `plugin` these ids make the moderator prompt "Missing source code", whose suggested status is **rejected**:
`GPL-2.0(+/-only/-or-later)`, `GPL-3.0*`, `LGPL-2.1*`, `LGPL-3.0*`, `AGPL-3.0*`, `MPL-2.0`.
Fix it by filling in `source_url` (a public repo) or attaching a sources jar as an additional file with `file_type: "sources-jar"`. Datapacks and resource packs are exempt, because their source is the distributed file.

### 1.6 License tradeoffs (for the owner's decision)

| License | Code reuse by others | Obligations for you on Modrinth | Fits |
|---|---|---|---|
| `MIT` | anyone, including closed forks; attribution only | none. Source not required (but recommended) | libraries, small mods, maximum adoption |
| `Apache-2.0` | like MIT plus a patent grant and NOTICE | none | Fabric API's choice |
| `LGPL-3.0-only` / `-or-later` | can be linked or used by any mod; changes to *your* files must stay open | **must provide source** (public repo or sources jar) or the project is rejected | mods you want to stay open without blocking closed addons |
| `MPL-2.0` | file-level copyleft | must provide source | similar to LGPL, simpler for Java |
| `GPL-3.0` | derivatives must be GPL | must provide source; also blocks closed addons and can conflict with ARR modpacks | strong copyleft |
| `CC0-1.0` | public domain dedication | none | datapacks and resource packs, templates (the Stonecutter template uses it) |
| `CC-BY-4.0` / `CC-BY-SA-4.0` | assets with attribution (SA: share-alike) | none | art, resource packs. Not recommended for code |
| `LicenseRef-All-Rights-Reserved` | no reuse without permission | none, but modpack authors must ask for permission to redistribute, which reduces inclusion in packs | when the owner wants control |

Modpack angle: Modrinth's 2026 modpack-permissions system asks pack authors for each file's license or permission basis. "The project's license allows redistribution" is one option. Under ARR every pack author needs explicit permission, but files that are hosted on Modrinth and referenced by URL in a `.mrpack` are not re-uploads. (src: Modrinth news "modpack permissions", 2026-06)

### 1.7 Content rules and moderation (what actually gets rejected)
New projects start as **draft**. You submit for review (web UI "Submit for review"), and a moderator approves or rejects using a checklist. The checklist source is public in `modrinth/code/packages/moderation/src/data/messages/checklist/messages/**.md`. Rejection reasons that matter to us:

1. **Generative AI (rules §6, last modified 2026-08-13).**
   - "Projects cannot be entirely or primarily comprised of content created or derived from generative AI output."
   - You **must** apply the **"Contains AI-generated content"** content disclosure when "a substantial portion of the project's code is a product of AI output", when any assets are primarily AI output, when the design or functionality relies on generative AI, or when "any element of the project's page such as description or publishing relies on generative AI".
   - AI-generated **images** (gallery, icon, description images) are prohibited and may be removed.
   - Moderation template `rules/ai-generated.md` rejects with "we ask that you **do not resubmit**".
   - **This directly affects this repo (agent-written mods).** The owner must know this before publishing. Expect to need the disclosure, human-made icon and screenshots (real in-game screenshots are fine; they are not AI images), and substantial human authorship.
2. **Obfuscation** (`tech-review/misused-obfuscation.md`): allowed only for legal or licensing need, a critical user benefit, or protecting auth checks. Otherwise remove it and delete every obfuscated version. Never use ProGuard or similar.
3. **Source requested** (`tech-review/request-source*.md`): for binaries, natives, or anything suspicious. Can be shared privately with GitHub org `ModrinthModeration`. Moderators prefer "binary files ... built through transparent automation", which is a reason to publish from CI.
4. **License without source** (see 1.5).
5. **Malware** leads to indefinite rejection of all of the account's projects.
6. **Undisclosed data upload.** Rules §1.11: no uploading data to a remote server "without clear disclosure", including connections the user does not directly choose. Content disclosures include **Telemetry**, **External system interactions**, **Ads**, **Paid features**, **Photosensitivity**, **Derivative content**, **Archive** and **AI-generated content**. Disclosures are plain text or inline Markdown, with no HTML.
7. **Cheats** (§3): x-ray, aim assist, flight or speed, auto-PvP, dupes, or hiding mods from servers, unless there is a server-side opt-in. Advertising something as a "hack client" is rejected.
8. **Description** (§2.1–2.2): must say what the project does, why someone would download it, and any critical pre-download info.
   - It needs a plain-text version: no image-only descriptions, and no headers used as body text.
   - An English version is required.
   - No hyperbolic or misleading claims (`tech-review/unclear-misleading.md`).
9. **Metadata** (§5.1): correct loaders, game versions, environment, tags and license. Moderators have templates for each. List dependencies on **every** version (§5.6).
10. **Files** (§5.7, §5.8):
    - One primary file per version. Additional files are only for special purposes such as sources, dev or javadoc jars, or signatures.
    - Never put alternate variants of the content (e.g. a "lite" edition) in one project; each variant gets its own project. New versions must be a "linear upgrade".
    - Do not upload two files with the same name in one version (it creates duplicate primary entries).
    - Do not redistribute libraries you depend on (`versions/redist-libs.md`). Jar-in-jar of a small library is fine; shading all of Fabric API is not.
11. **Resource packs**: do not include an "excessive amount of unmodified assets from vanilla" (`versions/vanilla-assets.md`).
12. **Title**: just the name. **Summary**: short, unformatted, and without the title. The **slug** regex is `^[\w!@$()`.+,"\-']{3,64}$`.
13. **Reuploads**: need explicit permission; a fork must diverge substantially and credit the original.

---

## 2. Creating a project

### 2.1 Web UI (recommended for the first time)
1. Go to modrinth.com, click "Create a project", and pick a name, a slug, visibility, and a summary.
2. Fill in the settings: description (body), icon (square PNG, human-made), links, license, tags, and environment (per version).
3. Upload a version (see 3).
4. Fill in content disclosures.
5. Submit for review.
6. Copy the **project id** (8 chars, e.g. Fabric API is `P7dR8mSH`) from the project page for build scripts. The slug also works in most endpoints.

### 2.2 API v2 `POST /v2/project` (multipart)
Scope `PROJECT_CREATE`. Parts: `data` (JSON), plus an optional `icon` file (`.png/.jpg/.jpeg/.bmp/.gif/.webp/.svg/.svgz/.rgb`).

```json
{
  "slug": "my-mod",
  "title": "My Mod",
  "description": "One-line summary, no title, no formatting.",
  "body": "# My Mod\nLong markdown description...",
  "categories": ["utility"],
  "additional_categories": [],
  "client_side": "required",
  "server_side": "required",
  "license_id": "MIT",
  "license_url": null,
  "project_type": "mod",
  "is_draft": true,
  "initial_versions": [],
  "source_url": "https://github.com/OWNER/REPO",
  "issues_url": "https://github.com/OWNER/REPO/issues",
  "wiki_url": null,
  "discord_url": null,
  "donation_urls": []
}
```
- `client_side`/`server_side`, `initial_versions`, `is_draft` (docs say always `true`) and `gallery_items` are deprecated, but older servers may still require them.
- Set `environment` per version afterwards.
- Edit fields later with `PATCH /v2/project/{id|slug}` (JSON body with any of the fields, e.g. `{"body": "..."}`, `{"license_id": "MIT"}`). Scope `PROJECT_WRITE`.
- Icon: `PATCH /v2/project/{id}/icon?ext=png` with a raw image body.
- Submitting for review via the API is UNVERIFIED; it is probably `PATCH` with `{"requested_status":"approved","status":"processing"}`. Use the web UI.

### 2.3 Gallery
`POST /v2/project/{id|slug}/gallery?ext=png&featured=true&title=...&description=...&ordering=0`
- Body: the raw image bytes. Max **5 MiB**. Scope `PROJECT_WRITE`. Returns 204.
- `ext` and `featured` are required query parameters.
- Change an image with `PATCH /v2/project/{id}/gallery?url=<image url>&...`; delete with `DELETE ...?url=` (the `url` param form is UNVERIFIED).
- Gallery images must not be AI-generated and must not misrepresent the content (§5.5).

### 2.4 Body Markdown
Modrinth renders GitHub-flavoured Markdown, with sanitised HTML (no scripts or iframes, except allowed embeds such as YouTube). Rules: no image-only body, no headers used as paragraphs, and include an English version. Keep the body in `README.md` and sync it (Minotaur `syncBodyFrom`, or mod-publish `projectDescription`). Both overwrite the remote body with no undo.

---

## 3. Uploading versions

### 3.1 API v2 `POST /v2/version` (multipart/form-data)
Auth header: `Authorization: mrp_xxx...` (the PAT itself, **no `Bearer`**). Scope `VERSION_CREATE`.
- Part `data`: a JSON string.
- One part per file, with arbitrary names that are listed in `file_parts`.
- File types accepted: `.jar`, `.zip`, `.mrpack`, `.litemod`.

```json
{
  "project_id": "AABBCCDD",
  "file_parts": ["file"],
  "primary_file": "file",
  "name": "My Mod 1.2.0 for 26.3",
  "version_number": "1.2.0+26.3",
  "changelog": "### Added\n- ...",
  "game_versions": ["26.3"],
  "version_type": "release",
  "loaders": ["fabric"],
  "featured": true,
  "status": "listed",
  "requested_status": null,
  "environment": "client_and_server",
  "dependencies": [
    { "project_id": "P7dR8mSH", "version_id": null, "file_name": null, "dependency_type": "required" }
  ],
  "file_types": { "file": null }
}
```

Fields:
- **Required:** `project_id`, `file_parts`, and at least one file unless `status: "draft"`.
- `version_type`: `release | beta | alpha`.
- `status`: `listed | archived | draft | unlisted | scheduled | unknown`.
- `dependency_type`: `required | optional | incompatible | embedded`. Give `project_id` for "any version of X", `version_id` to pin one version, or `file_name` for a dependency that is not on Modrinth.
- `file_types` per part: `required-resource-pack | optional-resource-pack | sources-jar | dev-jar | javadoc-jar | signature | unknown` (or null).
- `loaders` by artifact:

  | Artifact | `loaders` |
  |---|---|
  | Fabric mod | `["fabric"]` (add `"quilt"` only if you actually test on Quilt) |
  | Datapack zip | `["datapack"]` |
  | Resource pack zip | `["minecraft"]` |
  | Shader | `["iris"]` etc. |

Errors: `400 {"error":"invalid_input","description":...}`; `401 unauthorized` for a wrong scope.

curl (with a unique User-Agent; see 3.3):
```bash
curl -sS -X POST https://api.modrinth.com/v2/version \
  -A "OWNER/REPO-publisher/1.0" \
  -H "Authorization: $MODRINTH_TOKEN" \
  -F 'data={"project_id":"AABBCCDD","file_parts":["file"],"primary_file":"file","version_number":"1.2.0+26.3","name":"1.2.0","game_versions":["26.3"],"version_type":"release","loaders":["fabric"],"featured":true,"environment":"client_and_server","dependencies":[{"project_id":"P7dR8mSH","dependency_type":"required"}]};type=application/json' \
  -F "file=@mods/mymod/build/libs/mymod-1.2.0+26.3.jar"
```

Related endpoints:
- `POST /v2/version/{id}/file`: add files.
- `PATCH /v2/version/{id}`: edit metadata, e.g. set `environment` on old versions.
- `DELETE /v2/version/{id}`
- `GET /v2/project/{id}/version?loaders=["fabric"]&game_versions=["26.3"]`: list versions (URL-encode the JSON arrays).
- `GET /v2/version_file/{sha1}?algorithm=sha1`: look up a version by file hash.
- Staging server for dry runs: `https://staging-api.modrinth.com/v2`. It needs a separate staging account and token. Minotaur's `apiURL` and mod-publish-plugin both accept a custom endpoint (mod-publish option name UNVERIFIED).

### 3.2 Tokens (PAT) and scopes
- Create a token at modrinth.com, Settings, "PATs" (account settings). It is shown once and starts with `mrp_`.
- Scope names (src: labrinth `models/v3/pats.rs`): `PROJECT_CREATE`, `PROJECT_READ`, `PROJECT_WRITE`, `PROJECT_DELETE`, `VERSION_CREATE`, `VERSION_READ`, `VERSION_WRITE`, `VERSION_DELETE`, `USER_READ`, `NOTIFICATION_READ`, `ANALYTICS`, `ORGANIZATION_*`, `COLLECTION_*`, and others.
- Minimum for CI publishing: **Create versions** (`VERSION_CREATE`). Body sync also needs **Read projects** and **Write projects** (`PROJECT_READ` + `PROJECT_WRITE`). Minotaur's README calls these `CREATE_VERSION`/`PROJECT_WRITE`, which are the UI labels.
- A missing scope produces 401, which Minotaur reports as "Invalid Authentication Credentials".
- Store the token as GitHub secret `MODRINTH_TOKEN`. Never commit it. Give it an expiry and narrow scopes.
- Old GitHub-token auth "will stop working once API v3 is generally available".

### 3.3 Rate limits and User-Agent
- **300 requests/minute per IP**, with or without a token. Headers: `X-Ratelimit-Limit`, `X-Ratelimit-Remaining`, `X-Ratelimit-Reset` (verified live).
- A **uniquely identifying User-Agent is required.** Generic agents (`okhttp/x`, `curl/x`, `python-requests`) are likely to be blocked.
- The docs' format is `github_username/project_name/1.56.0 (contact)`. Contact info is recommended, not required. Do not put the owner's email in it without asking.
- Deprecated API versions return **410**.

### 3.4 Versioning conventions
- Use `<mod semver>+<mc version>`, e.g. `1.2.0+26.3`, which the repo already does. `+build` metadata is ignored for ordering by Fabric Loader's SemVer and Modrinth's sort, and it keeps file names unique across MC ports.
- Separate files for the same mod version across loaders can share `version_number`. Terralith has three versions all numbered `2.6.5+26.3`, for datapack, neoforge and fabric. Minotaur's README note "will fail if Modrinth has this version already" is stale.
- File names must be unique **within** a version.
- `version_type`: `alpha` for snapshot (`26.4-snapshot-N`) builds, `beta` for pre-1.0 or experimental builds, and `release` otherwise.
- `fabric.mod.json` `depends.minecraft`: the repo uses `"~${minecraft_version}"`, which for `26.3` means `>=26.3 <26.4`. Use `">=26.3 <26.4"` for explicit ranges, or `"26.3.x"`. Patch releases like `26.3.1` usually work, but list them in `game_versions` only after testing.
- Changelogs: keep `CHANGELOG.md` in Keep-a-Changelog format (`## [1.2.0] - 2026-11-14` / `### Added|Changed|Fixed|Removed`). Extract the current section in Gradle (see 5.3) and pass it as the Modrinth changelog (Markdown).

### 3.5 Uploading datapacks
A zip layout (no wrapping folder):
```
mypack-1.0.0+26.3.zip
├── pack.mcmeta      {"pack":{"description":"...","min_format":121,"max_format":121}}
├── pack.png         (optional, 64x64 or larger, square)
└── data/<namespace>/...
```
- 26.3 data pack format is **121.0**; vanilla built-in packs use `"min_format": 121, "max_format": 121` (src: vanilla-jar `data/minecraft/datapacks/*/pack.mcmeta`, `version.json` `pack_version.data_major=121`).
- The old `pack_format` and `supported_formats` keys are pre-1.21.9.
- On Modrinth: loaders `["datapack"]`, `game_versions ["26.3"]`, environment `server_only`. Categories come from the mod list (e.g. `worldgen`, `game-mechanics`).

**Can one thing be both a datapack and a Fabric mod?** Yes, as two artifacts in one project (the Terralith pattern):
- Version A: `mypack-1.0.0+26.3.zip` with loaders `["datapack"]`.
- Version B: `mypack-1.0.0+26.3.jar` with loaders `["fabric"]`. It is a jar with `fabric.mod.json` and the same `data/` folder. Fabric's resource loader exposes a mod's `data/` as a built-in datapack that is always enabled, so a `pack.mcmeta` in the jar is not needed. It should declare `depends: fabric-resource-loader-v1` (or `fabric-api`), and Modrinth gets a `required` dependency on Fabric API.
- The project then becomes `project_type: "mod"`. That is accepted, and the datapack zip is still downloadable.
- Upload them as **separate versions** with one loader each. Do not tag one zip with `["datapack","fabric"]`.
- One hybrid file can technically work in both places: a jar is a zip, so a jar with `pack.mcmeta` at the root works when dropped into `datapacks/`. On Modrinth, however, a version's primary file is either a datapack or a mod, so ship two files.

### 3.6 Uploading resource packs
- Zip with `pack.mcmeta` (`"min_format": 97.1, "max_format": 97.1` for 26.3, resource pack format 97.1 per `version.json` `resource_major=97, resource_minor=1`) and `assets/`.
- Modrinth loaders `["minecraft"]`, with resource pack categories and features plus one `resolutions` tag.
- Don't bundle unmodified vanilla assets.
- A mod can declare a resource pack dependency with `file_types: {"pack": "required-resource-pack"}` on an additional file. That field exists for server resource-pack style setups; most mods put assets in the jar instead.

---

## 4. Gradle publishing plugins

### 4.1 mod-publish-plugin (recommended)
Fabric API 26.3 publishes with it: `build-logic/.../fabric-api.root-publishing.gradle`, version 2.0.1 at the time. The Stonecutter template wires it too. It supports Modrinth, CurseForge, GitHub Releases and Discord webhooks, with a dry-run mode.

Root `build.gradle` (declare the version once, next to Loom):
```groovy
plugins {
	id 'net.fabricmc.fabric-loom' version "${loom_version}" apply false
	id 'me.modmuss50.mod-publish-plugin' version '2.2.1' apply false
}
```
Root `settings.gradle` already has `gradlePluginPortal()` in `pluginManagement`.

`mods/<modid>/build.gradle`:
```groovy
plugins {
	id 'net.fabricmc.fabric-loom'
	id 'me.modmuss50.mod-publish-plugin'
}
apply from: rootProject.file('gradle/fabric-mod.gradle')

publishMods {
	file = tasks.named('jar').flatMap { it.archiveFile }   // 26.x: jar, NOT remapJar
	additionalFiles.from(tasks.named('sourcesJar').flatMap { it.archiveFile })
	changelog = providers.environmentVariable('CHANGELOG').orElse('See CHANGELOG.md')
	type = STABLE                                  // BETA, ALPHA
	version = project.version.toString()           // default anyway
	displayName = "${project.mod_id} ${project.version}"
	modLoaders.add('fabric')
	dryRun = providers.environmentVariable('MODRINTH_TOKEN').getOrNull() == null

	modrinth {
		accessToken = providers.environmentVariable('MODRINTH_TOKEN')
		projectId = project.modrinth_id            // put modrinth_id=XXXXXXXX in the mod's gradle.properties
		minecraftVersions.add(project.minecraft_version)
		environment = CLIENT_AND_SERVER            // CLIENT_ONLY, SERVER_ONLY, SINGLEPLAYER_ONLY, ...
		requires('fabric-api')                     // slug or id; also optional(), incompatible(), embeds()
		// requires { id = 'P7dR8mSH'; version = 'IIJJKKLL' }
		// projectDescription = providers.fileContents(layout.projectDirectory.file('README.md')).asText  // needs PROJECT_READ+WRITE
	}

	github {
		accessToken = providers.environmentVariable('GITHUB_TOKEN')
		repository = providers.environmentVariable('GITHUB_REPOSITORY').orElse('OWNER/REPO')
		commitish = providers.environmentVariable('GITHUB_SHA').orElse('main')
		tagName = providers.environmentVariable('GITHUB_REF_NAME').orElse('dryrun')  // option name UNVERIFIED; Fabric API passes commitish only
	}
}
```

Other options:
- `minecraftVersionRange { start = "26.1"; end = "latestRelease"; includeSnapshots = false }`
- `minecraftVersionList("26.1, 26.1.1, 26.1.2")`
- `additionalFile(sourcesJar) { type = SOURCES_JAR }`
- `maxRetries` (default 3)

Tasks: `publishMods` publishes to every configured platform. Per-platform tasks such as `publishModrinth` and `publishGithub` exist (exact names UNVERIFIED; run `./gradlew :mymod:tasks --group publishing`).

Dry run writes the release assets under `build/publishMods/` (Fabric API uploads that folder as a CI artifact).

Use `providers.environmentVariable` (lazy) so a configuration-cached build doesn't embed secrets.

### 4.2 Minotaur (official Modrinth plugin)
- `id "com.modrinth.minotaur" version "2.10.0"`. Pin it exactly; the README suggests `2.+`, but pinning is safer.
- 2.10.0 (2026-09-16) adds `environment`. 2.9.0 adds file types for `additionalFiles`.
- Loader auto-detection: if a `loom` extension exists, it adds `fabric` (or `loom.platform`). This works with `net.fabricmc.fabric-loom` 1.19.
- Game version auto-detection: reads the `minecraft` configuration's dependency version, which gives `26.3`.
- Gradle 9.8 and configuration-cache compatibility is **UNVERIFIED**; the releases page lists no Gradle notes. The build does not enable the configuration cache (`gradle.properties` has only `org.gradle.caching`).

```groovy
plugins { id 'com.modrinth.minotaur' version '2.10.0' }

modrinth {
	token = System.getenv('MODRINTH_TOKEN')    // default: env MODRINTH_TOKEN
	projectId = 'my-mod'                       // slug or id
	versionNumber = project.version.toString() // default: project.version
	versionName = "My Mod ${project.version}"
	versionType = 'release'                    // release | beta | alpha
	uploadFile = jar                           // 26.x: jar (README's "remapJar" is for obfuscated MC)
	additionalFiles = [sourcesJar]
	gameVersions = [project.minecraft_version] // auto-detected from Loom
	loaders = ['fabric']                       // auto-detected from Loom
	environment = 'client_and_server'          // upper-cased to the Environment enum internally
	changelog = rootProject.file('CHANGELOG.md').text   // or extracted section
	dependencies {
		required.project 'fabric-api'          // scopes: required, optional, incompatible, embedded
		optional.version 'modmenu', '21.0.1'   // type: project | version
	}
	syncBodyFrom = rootProject.file('README.md').text
	debugMode = false                          // true: print payload, don't upload
	failSilently = false
}
tasks.modrinth.dependsOn(tasks.modrinthSyncBody)  // optional: sync body every publish
```
- Tasks: `modrinth` (upload) and `modrinthSyncBody`.
- In the README, content between `<!-- modrinth_exclude.start -->` and `<!-- modrinth_exclude.end -->` is not synced.
- Body sync cannot be undone; check it with `debugMode` first.
- After an upload: `tasks.modrinth.uploadInfo` / `errorInfo` / `wasUploadSuccessful()`.

### 4.3 Alternatives
- **mc-publish** (`Kir-Antipov/mc-publish@v3.3.2`): a GitHub Action that reads metadata from `fabric.mod.json` and uploads to Modrinth, CurseForge and GitHub.
  - Inputs: `modrinth-id`, `modrinth-token`, `curseforge-id`, `curseforge-token`, `github-token`, `files`, `loaders`, `game-versions`, `version-type`, `dependencies`. The inputs are from memory: UNVERIFIED for v3.3.2.
  - Releases continue (3.3.0, 3.3.1, then 3.3.2 on Oct 22). Its Node runtime status under GitHub's Node 24 migration is UNVERIFIED.
  - Minotaur's README states Modrinth does not support mc-publish.
  - It has to guess game versions from `fabric.mod.json`'s `"~26.3"`; set `game-versions` explicitly.
- **Raw curl** from CI (3.1): zero dependencies, which suits datapacks and resource packs that are not built with Gradle.
- **CLIs**: Modrinth has no official CLI. Third-party tools: `packwiz` (modpack authoring and `.mrpack` export), `ferium` (mod manager), `mrpack-install` (server install from `.mrpack`). These are pointers only, not verified for 26.x.
- **Modrinth Maven** (for consuming dependencies, not publishing): `maven { url 'https://api.modrinth.com/maven' }` with `maven.modrinth:<slug>:<version>`. The repo already declares it.

---

## 5. GitHub Actions

Verified latest majors on 2026-11-14 (by `git ls-remote --tags`):
- `actions/checkout` v7.0.1. v7 blocks fork PR checkout under `pull_request_target` and `workflow_run`.
- `actions/setup-java` v6.3.0
- `gradle/actions` v6.4.1
- `actions/upload-artifact` v7.0.2
- `actions/download-artifact` v8.0.2
- `softprops/action-gh-release` v3.0.4

Fabric API's own CI (branch 26.3) still uses checkout@v6, setup-java@v5 and upload-artifact@v7, so either generation works.

`gradle/actions/setup-gradle@v6` gotchas:
- The default cache provider is "enhanced", a proprietary `gradle-actions-caching` library. It is free for public repos and a "Free Preview" for private repos. Set `cache-provider: basic` (MIT, built on `actions/cache`) to avoid licensing questions.
- `setup-gradle` **validates the wrapper by default** (`validate-wrappers`), so a separate `wrapper-validation` step is optional.
- Use `cache-read-only` on non-default branches (the default already does this for non-default branches).

### 5.1 `.github/workflows/build.yml`: build and test on push and PR
```yaml
name: Build
on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

concurrency:
  group: build-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '25'
      - uses: gradle/actions/setup-gradle@v6
        with:
          cache-provider: basic
      # `build` -> `check` -> `test` (JUnit) + `runGameTest` (server gametests, via fabricApi.configureTests)
      - run: ./gradlew build --stacktrace
      - name: Upload reports on failure
        if: failure()
        uses: actions/upload-artifact@v7
        with:
          name: reports
          path: |
            **/build/reports/
            **/build/test-results/
            **/build/run/gameTest/logs/
            **/build/run/gameTest/crash-reports/
      - uses: actions/upload-artifact@v7
        with:
          name: jars
          path: mods/*/build/libs/*.jar
          if-no-files-found: error

  client-gametest:
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '25'
      - uses: gradle/actions/setup-gradle@v6
        with:
          cache-provider: basic
      - run: sudo apt-get update && sudo apt-get install -y xvfb mesa-vulkan-drivers
      # Loom 1.19 wraps client run tasks in `xvfb-run -a` automatically when CI is set, on Linux, with xvfb-run on PATH.
      - run: ./gradlew runClientGameTest --stacktrace
      - if: ${{ !cancelled() }}
        uses: actions/upload-artifact@v7
        with:
          name: client-gametest-screenshots
          path: mods/*/build/run/clientGameTest/screenshots/
```
See `testing-debugging.md` for the gametest details. `runClientGameTest` runs every mod's client tests: an unqualified task name runs in every project that has it.

### 5.2 `.github/workflows/release.yml`: publish on tag
Tag convention for this multi-mod repo: `<modid>-v<mod_version>`, e.g. `refmod-v0.2.0`. The job derives the Gradle project from the tag. It also checks that the tag matches `mod_version` in `mods/<modid>/gradle.properties`, so a stale property cannot publish under the wrong number.

```yaml
name: Release
on:
  push:
    tags: ['*-v*']

permissions:
  contents: write   # GitHub release

jobs:
  publish:
    runs-on: ubuntu-24.04
    environment: release            # optional: add required reviewers in repo settings
    steps:
      - uses: actions/checkout@v7
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '25'
      - uses: gradle/actions/setup-gradle@v6
        with:
          cache-provider: basic
          cache-read-only: true
      - name: Resolve mod from tag
        id: mod
        run: |
          tag="${GITHUB_REF_NAME}"
          modid="${tag%-v*}"; ver="${tag##*-v}"
          test -f "mods/${modid}/build.gradle" || { echo "::error::no mods/${modid}"; exit 1; }
          prop=$(grep -E '^mod_version=' "mods/${modid}/gradle.properties" | cut -d= -f2)
          [ "$prop" = "$ver" ] || { echo "::error::tag $ver != mod_version $prop"; exit 1; }
          echo "modid=$modid" >> "$GITHUB_OUTPUT"
          echo "version=$ver" >> "$GITHUB_OUTPUT"
      - name: Extract changelog section
        run: |
          f="mods/${{ steps.mod.outputs.modid }}/CHANGELOG.md"
          if [ -f "$f" ]; then
            awk -v v="${{ steps.mod.outputs.version }}" '
              $0 ~ "^## \\[?"v"\\]?" {p=1; next} p && /^## / {exit} p {print}' "$f" > CHANGELOG.part
          else echo "No changelog provided" > CHANGELOG.part; fi
          { echo 'CHANGELOG<<EOF'; cat CHANGELOG.part; echo EOF; } >> "$GITHUB_ENV"
      - run: ./gradlew ":${{ steps.mod.outputs.modid }}:build" ":${{ steps.mod.outputs.modid }}:publishMods" --stacktrace
        env:
          MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          # CURSEFORGE_TOKEN: ${{ secrets.CURSEFORGE_TOKEN }}
      - uses: actions/upload-artifact@v7
        with:
          name: release-${{ steps.mod.outputs.modid }}
          path: mods/${{ steps.mod.outputs.modid }}/build/libs/*.jar
```
- If you don't configure `github {}` in `publishMods`, add a `softprops/action-gh-release@v3` step: `with: { files: mods/<modid>/build/libs/*.jar, body_path: CHANGELOG.part }`.
- With Minotaur, replace the Gradle task with `:<modid>:modrinth`.
- Do not run `publishMods` on `pull_request`: secrets are not available to forks, and a dry run happens anyway.

### 5.3 Changelog into Gradle (alternative to the awk step)
```groovy
def changelogSection = providers.provider {
	def f = file('CHANGELOG.md'); if (!f.exists()) return 'No changelog provided'
	def out = []; boolean on = false
	for (line in f.readLines()) {
		if (line.startsWith('## ')) { if (on) break; on = line.contains(project.mod_version) ; continue }
		if (on) out << line
	}
	out.join('\n').trim() ?: 'No changelog provided'
}
publishMods { changelog = providers.environmentVariable('CHANGELOG').orElse(changelogSection) }
```

---

## 6. CurseForge (only if the owner wants it)
- Separate account. Projects are created and moderated in the web UI (authors.curseforge.com) before any upload works.
- Upload API (legacy authors API on the game site):
  - Get version ids: `GET https://minecraft.curseforge.com/api/game/versions`, header `X-Api-Token: <token>`. Returns `id`, `name`, `slug`. Game version, loader ("Fabric") and Java version are all "game versions" by id.
  - Upload: `POST https://minecraft.curseforge.com/api/projects/{projectId}/upload-file` as multipart, with `metadata` (JSON) and `file`.
  - `metadata` fields:
    - `changelog`
    - `changelogType` (`text|html|markdown`)
    - `displayName`
    - `gameVersions` (array of ids)
    - `releaseType` (`alpha|beta|release`)
    - `parentFileID` (for additional files; then omit `gameVersions`)
    - `relations.projects[]`: `{slug, projectID?, type}`, where `type` is one of `embeddedLibrary|incompatible|optionalDependency|requiredDependency|tool`
  - (src: CurseForge support article "CurseForge Upload API")
- mod-publish-plugin: `curseforge { projectId = "123456"; projectSlug = "my-mod"; accessToken = providers.environmentVariable("CURSEFORGE_TOKEN"); minecraftVersions.add("26.3"); client = true; server = true; requires("fabric-api") }`. Fabric API sets `client`/`server` and `curseforge_minecraft_versions`.
- Whether CurseForge accepts the `26.3` version string is UNVERIFIED. Fabric API keeps a separate `curseforge_minecraft_versions` property, which suggests CurseForge naming can differ.
- CurseForge has its own project-level rules, distribution toggles for third-party launchers, and an AI policy (UNVERIFIED).

## 7. GitHub Releases
- Cheapest distribution, and it doubles as proof of "transparent automation" for Modrinth moderators.
- `gh release create refmod-v0.2.0 mods/refmod/build/libs/*.jar --title "refmod 0.2.0+26.3" --notes-file CHANGELOG.part` (needs `contents: write`).
- Or use `softprops/action-gh-release@v3`, or mod-publish `github {}`.
- Attach the sources jar too. Link the Modrinth `source_url` to the repo, which must be public for that to count as source.

---

## 8. Multi-version support

### 8.1 The 1.21.x / 26.x split
- **Up to 1.21.11:** obfuscated Minecraft.
  - Plugin `net.fabricmc.fabric-loom-remap` (or legacy `fabric-loom`).
  - Needs `mappings loom.officialMojangMappings()` or Yarn.
  - Dependencies use `modImplementation`. Output is `remapJar` to intermediary.
  - Java 21. Class tweaker header `classTweaker v2 named`.
  - Mojang names changed at 1.21.11: `ResourceLocation` became `Identifier`.
- **26.1 and later:** unobfuscated.
  - Plugin `net.fabricmc.fabric-loom`. No mappings line.
  - Dependencies use plain `implementation`. Output is `jar`.
  - Java 25. Class tweaker header `classTweaker v2 official`.
- Loom 1.19 registers both plugin ids: `net.fabricmc.fabric-loom` (`LoomNoRemapGradlePlugin`) and `net.fabricmc.fabric-loom-remap` (`LoomRemapGradlePlugin`), plus `net.fabricmc.fabric-loom-companion`. (src: fabric-loom `dev/1.19` `build.gradle`)

### 8.2 Stonecutter 0.10 (released 2026-11-08 on the plugin portal and maven.kikugie.dev)
How it works:
- One `src/` tree is shared by a Gradle subproject per Minecraft version (under `versions/<mc>/`, with build dirs only).
- Version-specific code lives in comment conditionals.
- "Set active project to X" rewrites the files in `src/` so the IDE sees the code for that version. Commit with the `vcsVersion` active.
- Each version builds independently: `./gradlew :26.3:build`. An unqualified `./gradlew buildAndCollect` runs in every version project.

Official template: `stonecutter-versioning/stonecutter-template-fabric`, last commit 2026-11-08. It targets `1.21.1, 1.21.11, 26.2, 26.3`, uses Gradle wrapper 9.9.0, and `loomx.loom_version = "1.19-SNAPSHOT"`.

`settings.gradle.kts`:
```kotlin
pluginManagement { repositories {
    mavenCentral(); gradlePluginPortal(); maven("https://maven.fabricmc.net/")
    maven("https://maven.kikugie.dev/releases"); maven("https://maven.kikugie.dev/snapshots")
} }
plugins {
    id("dev.kikugie.stonecutter") version "0.10"
    id("dev.kikugie.loom-back-compat") version "0.4.3"   // picks fabric-loom-remap (<26.1) or fabric-loom (>=26.1)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
stonecutter { create(rootProject) { versions("1.21.1", "1.21.11", "26.2", "26.3"); vcsVersion = "26.3" } }
```

`stonecutter.gradle.kts` (the controller):
```kotlin
plugins { id("dev.kikugie.stonecutter") }
stonecutter active "26.3"
stonecutter parameters {
    swaps["mod_version"] = "\"${properties["mod.version"]}\";"
    constants["release"] = properties["mod.id"] != "template"
    replacements {
        string(current.parsed >= "1.21.11") { replace("ResourceLocation", "Identifier") }
        string(current.parsed >= "26.1") { replace("classTweaker v2 named", "classTweaker v2 official") }
    }
}
```

`build.gradle.kts` essentials:
- `id("dev.kikugie.loom-back-compat")`
- `minecraft("com.mojang:minecraft:${sc.current.version}")`
- `loomx.applyMojangMappings()`: a no-op on 26.x.
- Keep using `modImplementation(...)` everywhere; loom-back-compat converts it to `implementation` on 26.1+.
- `loomx.modJar` gives the right jar task for each version.
- `version = "${sc.properties["mod.version"]}+${sc.current.version}"`
- Java selection: `sc.current.parsed >= "26.1"` → 25, `>= "1.20.5"` → 21.
- `accessWidenerPath = sc.process(rootProject.file("src/main/resources/template.ct"), "build/processed.ct")`

Per-version properties go in `stonecutter.properties.toml`, in `["26.3"]` tables with keys such as `deps.fabric_api`, `mod.mc_compat = "~26.3"` and `mod.mc_releases = ["26.3"]` (for Modrinth `minecraftVersions`).

Source syntax:
```java
//? if <1.21 {
/*return new Identifier(namespace, path);
*///?} else
return Identifier.fromNamespaceAndPath(namespace, path);

public static final String VERSION = /*$ mod_version*/ "0.1.0";
//? if !release
LOGGER.info("dev build");
```

Adopting Stonecutter here would mean restructuring `mods/<id>/` into a Stonecutter tree per mod. That is only worth it if the owner wants 1.21.x support; 26.x-only mods don't need it. Stonecutter's docs site blocks scrapers, so the details above come from the template repo. Other DSL features (`targets`, `dependencies`, `chiseled*`) are UNVERIFIED for 0.10.

### 8.3 Multiloader (pointer only)
- NeoForge: ModDevGradle `net.neoforged.moddev` **2.0.148** (plugin portal).
- Common multiloader layouts: `common` + `fabric` + `neoforge` subprojects (e.g. jaredlll08 "MultiLoader-Template"), or Architectury Loom plus the Architectury API.
- Architectury Loom's maven metadata shows `1.17-SNAPSHOT` as latest. Its support for unobfuscated 26.x is UNVERIFIED.
- Modrinth: one project with separate versions per loader (`["fabric"]`, `["neoforge"]`). Minotaur auto-detects `net.neoforged.moddev` as `neoforge`.

---

## 9. Pre-publish checklist (agent-runnable)
1. The owner has decided the license, author and namespace (`decisions/inbox`). The root `LICENSE` is filled in, `fabric.mod.json` `license` matches the Modrinth `license_id`, and the license file is included in the jar (the build already copies `LICENSE` as `LICENSE_<modid>`).
2. `./gradlew :<mod>:build` is green, including server gametests. `runClientGameTest` passes. A headless dedicated server smoke test passes (see `testing-debugging.md`).
3. The jar contains no dev-only classes. Gametests live in the `gametest` source set (`createSourceSet = true`), so they are excluded. No shaded libraries without need. No obfuscation.
4. `fabric.mod.json`:
   - `depends.minecraft` is `~26.3`; `fabricloader` and `fabric-api` match.
   - `environment` matches the Modrinth environment: `"*"` means both sides, `"client"` means client_only.
   - `contact.sources` and `contact.issues` are set.
   - Icon is human-made.
5. Modrinth page:
   - Plain-text English description with no hype.
   - Real in-game screenshots.
   - Content disclosures filled in, **including AI-generated content where it applies**.
   - Environment set on every version.
   - Dependencies listed (Fabric API `P7dR8mSH` required).
   - `source_url` set if the license is copyleft.
6. Token scopes: `VERSION_CREATE` (+ `PROJECT_READ`/`PROJECT_WRITE` for body sync). The secret is stored in GitHub, and a dry run has been done (`publishMods` without a token, or Minotaur `debugMode = true`).

## 10. Where to look
- Modrinth API docs: https://docs.modrinth.com/api/ (pages `createproject`, `createversion`, `addgalleryimage`).
- Live tags: `https://api.modrinth.com/v2/tag/{category,loader,game_version,license,project_type,side_type,donation_platform,report_type}`.
- Moderation messages: https://github.com/modrinth/code/tree/main/packages/moderation/src/data/messages/checklist/messages
- Rules: https://modrinth.com/legal/rules (last modified 2026-08-13).
- New environments: https://modrinth.com/news/article/new-environments
- Fabric API publishing reference: `/tmp/claude-0/mcsrc/fabric-api/build-logic/src/main/groovy/fabric-api.root-publishing.gradle`, `/tmp/claude-0/mcsrc/fabric-api/.github/workflows/{build,release}.yml`
- mod-publish-plugin docs: https://modmuss50.github.io/mod-publish-plugin/
- Minotaur: https://github.com/modrinth/minotaur
- Stonecutter template: https://github.com/stonecutter-versioning/stonecutter-template-fabric
