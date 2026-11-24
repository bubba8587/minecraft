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

`.github/workflows/build.yml` runs the full suite on release tags, but no publish step is wired in yet and no release tag exists, so nothing has been released.
