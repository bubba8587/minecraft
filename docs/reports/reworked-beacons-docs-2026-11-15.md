<!-- dte:C5 -->
# Reworked Beacons: docs to merge (2026-11-15)

Written by the Reworked Beacons builder under the owner's policy (builders don't edit `player-docs/` or spec prose). For the orchestrator or a docs agent to merge into `specs/reworked-beacons.md` and a new player vault folder `player-docs/Projects/Bubba's Reworked Beacons/` (overview page + a "Beacon" page, linked from Home). Already in the spec: the "Payload API" section, the `covers:` line and the milestone objectives.

## Player-facing (overview + Beacon page)

- **Bubba's Reworked Beacons** is a Fabric mod (26.3, needs Fabric API) that reworks the vanilla beacon. No new blocks or items. Required on both client and server (a client without it is turned away with a message).
- **The screen**: one item slot, no power buttons, no payment row. The item **stays in the beacon** and can be taken out again; breaking the beacon drops it. Hoppers can't fill beacons. The screen shows: the state ("Sky beacon: N-layer pyramid, R blocks out", "Paired beacons: N-layer pyramids, R blocks out", "Idle: needs a pyramid below", "Idle: needs a clear sky above, or an upside-down beacon to pair with"), the item's name and what it does in its tooltip words, then "Needs a N-layer pyramid" (red) or "Only outputting Gem Power N" (yellow) when relevant.
- **Every living thing in range** gets the effect: players and mobs (spectators excluded). Effects pulse every 4 s and last (9 + 2 x layers) s, as in vanilla.
- **Pyramid caps Gem Power** (for items from other mods): 1 layer I, 2 layers up to III, 3 up to V, 4 layers VI.
- **Standalone payments** (nothing else installed): the item is the power choice.

| Item | Power | Pyramid needed | 4-layer pyramid |
|---|---|---|---|
| Amethyst Shard | Speed | 1 | Speed II |
| Nether Quartz | Haste | 1 | Haste II |
| Ender Pearl | Resistance | 2 | Resistance II |
| Diamond | Jump Boost | 2 | Jump Boost II |
| Prismarine Shard | Strength | 3 | Strength II |
| Nether Star | Regeneration | 3 | Regeneration II |

  Emeralds, iron, gold and netherite ingots are no longer accepted.
- **Sky beacon range** (unchanged from vanilla): 20 / 30 / 40 / 50 blocks out for 1-4 layers, from that far below the beacon up to the build limit. Needs the vanilla clear beam to the sky.
- **Upside-down beacon**: placing a beacon against the underside of a block hangs it upside down (like a hanging lantern). Alone it does nothing (no beam, no effect, no item use). Its pyramid is built upside down above it.
- **Paired beacons** (underground): a normal beacon with its pyramid below, and an upside-down beacon straight above it with its pyramid above, with only clear blocks (air, glass, anything the vanilla beam passes) between them. The item goes in the bottom beacon. The smaller pyramid sets the range: **60 / 90 / 120 / 150 blocks** out sideways (3x vanilla); vertically from 10 blocks below the floor pyramid to 10 blocks above the ceiling pyramid. A white beam joins the two beacons while the pair works; a blocked beam turns the pair off (no beam, no effect). Beacon activate/deactivate/ambient sounds play for pairs as for sky beacons.
- **Existing worlds**: beacons keep working as sky beacons but lose their old vanilla powers; put an item in to set a power again.
- **Compatibility**: Bubba's Infused Gems and Bubba's Reworked Mobs supply beacon items when installed (gems; Totem of Warding). None of the three mods requires another.

## Spec facts (for `specs/reworked-beacons.md`, a "How it's built" section)

- Code: `BeaconBlockEntityMixin` adds the kept item (saved as `bubbas_reworked_beacons:item` in the block entity), the pair state, cancels vanilla `applyEffects` (also neutralises old saved `primary_effect`), replaces `createMenu` with `BeaconItemMenu` (own `MenuType` `bubbas_reworked_beacons:beacon`; lock codes still respected), drops the item in `preRemoveSideEffects`. The menu's slot writes straight to the block entity (`BeaconSlotContainer`), so two viewers can't duplicate the item; the block entity is deliberately not a `Container` (hoppers).
- `BeaconLogic`: pair check every 20 ticks on both sides (client needs it for the beam), pulse every 80 ticks on the server after vanilla's tick (vanilla still scans the sky beam and counts the pyramid). Pair scan goes straight up from the floor beacon to the WORLD_SURFACE height; vanilla's beam rule (light dampening 15 blocks, bedrock doesn't); an upright beacon in between ends it. A pair takes priority over a sky beam.
- Upside-down: `upside_down` boolean state added to `minecraft:beacon` (B15 approach C, like Crafting Tweaks' synthetic ore), default false; placement on a DOWN face sets it; model is vanilla's rotated `x: 180` (`assets/minecraft/blockstates/beacon.json`), so no new texture (B16 has nothing to ship; the empty 32x pack was removed). `ExtraStatesHandshake` turns away clients without the mod.
- Screen: vanilla's `beacon.png` background with the two power panels and payment row painted over (`BeaconItemScreen`), no new texture.
- Beam: `BeaconRendererMixin` stops a paired beam's last section at the upside-down beacon instead of running to y 2048.
- Prior art (2026-11-15, `tools/prior-art.py`): beacon mods on Modrinth change range (Beacon Range, Extended Beacon Range), base blocks (PurpurPack bases), payments (Potion Beacons: potions as beacon effects, CC-BY-SA), or let beacons work underground (Underground Beacons, ARR; Underground Beacon, MIT, 26.2). None offers a one-slot item beacon with an API for other mods, mobs in range, a pyramid power cap, or upside-down pairs. No library fits (custom menu + block entity mixin); hand-rolled because no library does it.

## Decisions I made (builder's choices, for the owner to confirm)

1. **Screen wording** (lang keys above) and showing the item's name above its description.
2. **A pair's beam shows whenever both pyramids and the clear line exist**, item or not, like vanilla's sky beam showing without a payment.
3. **A pair overrides a sky beam** when a floor beacon has both (an upside-down beacon above it in an open column).
4. **No maximum gap** between paired beacons: any height, as long as the line is clear.
5. **Standalone describe text** is vanilla's potion-tooltip words ("Speed", "Speed II") in the effect's colour.
6. **API extras**: `priority()` (standalone is -1000) and `replacesStandalonePayments()` so Infused Gems can choose whether plain gem bases stop working when it's installed. Infused Gems' gems are their own item, so by default plain amethyst shards etc. keep buying vanilla powers alongside gems.
7. Effects are ambient, visible particles (vanilla beacon style); spectators are not affected.

## Questions for the owner

- With Infused Gems installed, should plain gem bases (amethyst shard = Speed ...) still work in beacons, or only gems? (Spec says "the slot takes gems instead"; the API supports either; currently both work unless Infused Gems sets `replacesStandalonePayments`.)
- Should an upside-down beacon need the block above it (break/drop when that block goes, like a lantern)? Currently it stays.
- Old worlds: beacons lose their vanilla powers on install. OK, or convert the old payment to the matching gem-base power?
