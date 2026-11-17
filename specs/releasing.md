<!-- dte:A1,A2,B14 -->
# Releasing

## Pushing a version tag publishes that release from CI

A release is published by pushing a git tag of the form `<project>/v<version>`. CI runs the full suite (core, milestone and client tests) and publishes to **Modrinth** if the project meets Modrinth's generative-AI rules in `docs/knowledge/publishing.md` (B14 modrinthAiCompliant: the "Contains AI-generated content" disclosure on, no AI-generated icon, gallery or page image, and a substantial human-made part), otherwise to **GitHub Releases**, until it qualifies. Before a project's first Modrinth release, the owner is told which human-made parts carry it over the bar. Nothing is uploaded by hand.

## Releases come from `main`

Work happens on `develop`; `main` is the released state (CLAUDE.md "Branch model"). To release: run the release milestone checks on `develop`, merge `develop` into `main`, push `main`, then tag `<project>/v<version>` on `main` and push the tag. `main` never gets direct commits.

Status (2026-11-16): `.github/workflows/build.yml` runs the full suite on release tags, but no publish step is wired in yet, so nothing has been released.
