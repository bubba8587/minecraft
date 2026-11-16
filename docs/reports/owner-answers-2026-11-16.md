# Owner answers to merge into specs and player docs (2026-11-16)

Collected by the orchestrator while builders worked; merge in the docs pass, then delete this file.

## Bubba's Infused Gems / Reworked Beacons

- **Gem strength in a beacon:** `BEACON_SHARE = 0.5` (half of a full 12-gem netherite set, about six armor gems' worth) is approved **for now** (owner). Was the builder's choice; now an owner decision, revisit after playtesting.

## Reworked Beacons (owner, 2026-11-16)

- With Infused Gems installed, the standalone gem-base payments are **disabled** (only finished gems and other mods' items work). Being built in GemBeaconPayload.replacesStandalonePayments.
- Upside-down beacons **stay** when the block above is removed (not like a lantern).
- Existing beacons are simply **wiped** of vanilla powers when the mod is installed (no conversion).

## Decision tree (owner: 'retitle/edit what you need, I'm not ratifying anything right now')

- C4 retitled (armor, weapons, beacons) and its body updated; C5 retitled to fit 100 chars (still unratified).
- New unratified B leaves for the owner's principles: B17 vanillaFirst, B18 equalTreatment, B19 noHiddenCooldowns. Specs that follow them should cite them (dte:B17,B18,B19) in the docs pass.
