<!-- dte:A4 -->
# Quality

## Every mod has automated game tests that must pass before release

Each mod ships with Fabric game tests covering its main behavior, and the build that publishes a release runs them on a headless server first. Packs get the equivalent: they are loaded on a headless server and must load clean.
