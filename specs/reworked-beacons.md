<!-- dte:C5,B15,B17,B18,B19 -->
<!-- covers: mods/bubbas_reworked_beacons/* -->
# Bubba's Reworked Beacons (spec)

A separate mod that reworks vanilla beacons into a general "area" system.

- **Status:** built, version 0.1.0, unreleased.
- **Ids:** mod id `bubbas_reworked_beacons`, code in `mods/bubbas_reworked_beacons/`, Java package `io.github.bubba8587.bubbas_reworked_beacons`.
- **Owner's principles:** B17 vanillaFirst (vanilla's beacon block, model, beam rule, ranges and timing), B18 equalTreatment (every living thing in range), B19 noHiddenCooldowns.
- Design background and the options considered: `docs/research/area-gems-and-warding.md`.

## Why its own mod (owner)

Area effects were first needed by two projects: Bubba's Infused Gems (gems broadcasting their effect) and Warding (mob-free zones without spamming torches; the Totem of Warding lives in Bubba's Totems & Artifacts). The owner split the beacon rework out so the mods stay independent:

| Installed | Beacons |
|---|---|
| Reworked Beacons alone | the reworked beacon; gem-base payments buy vanilla's six powers (Standalone, below) |
| + Infused Gems | gems go in the slot and broadcast their effect; the standalone payments are off (owner) |
| + Totems & Artifacts | totems go in the slot: Warding wards the area; Gathering, Fortification, Growth, Seeking, Herding, Clear Skies, the North Wind and the Beginning have area forms too (`specs/totems-artifacts.md`) |
| + Reworked Mobs | the Ancient Fabricator's Fabricator Heart works in its beacons (Mining Fatigue III, Weakness and Slowness on players in the area; `specs/mob-factory.md`) |
| Gems, Totems or Mobs without Beacons | beacons stay vanilla; those mods work exactly as they do now |

Reworked Beacons offers one hook: "this item in a beacon does X at power Y" (Payload API, below). The other mods answer it only when Reworked Beacons is present (optional dependency), so none of them depends on another.

## The beacon (owner decisions)

- **One item slot** replaces vanilla's primary/secondary powers and the iron/gold/emerald/diamond payment. The screen says what the beacon will do, in the item's own tooltip words.
- **The item stays in the beacon** (removable), not consumed. Revisit if too strong; the beacon itself already costs a nether star.
- **Pyramid size caps the item's power:** 1 layer Gem Power I, 2 layers up to III, 3 layers up to V, 4 layers VI. A stronger item is capped, with a message like "Only outputting Gem Power III".
- **Affects every living thing in range, mobs included** (B18; spectators excluded). A Regeneration gem heals nearby mobs too.
- **No protection from a harmful item, the builder included (owner).** Players can counter with gems (e.g. matching resistances), and it enables designed area debuff environments (hostile structures, boss arenas).
- **Existing beacons are simply wiped** of their vanilla powers when the mod is installed; no conversion (owner). They keep working as sky beacons with no power until an item goes in.

## Range

- **Sky beacons: vanilla's area exactly.** A square box out to the range (20 / 30 / 40 / 50 blocks for 1-4 layers) sideways and downward, extended up to the build limit (26.3 `BeaconBlockEntity`: `new AABB(pos).inflate(range).expandTowards(0, height, 0)`).
- **Underground: paired beacons.**
  - **Upside-down placement (owner):** placing a beacon against the underside of a block hangs it upside down, like a hanging lantern. It is a new placement of the existing block, not a new block (B15). Unlike a lantern, it **stays** when the block above is removed (owner).
  - An upside-down beacon does nothing on its own (no beam, effect or item); it only closes a pair (owner).
  - **A pair:** a beacon on the floor with its pyramid below, and an upside-down beacon directly above it with its pyramid built upside down above that. The space between must be clear or glass, like a beam. The upside-down beacon holds no item.
  - **Range:** the **smaller** pyramid sets it. A square box **3x vanilla's range sideways** (60 / 90 / 120 / 150 blocks out), and vertically from **10 blocks below the floor pyramid to 10 blocks above the ceiling pyramid**.
  - **Beam as status light (owner):** a pair shows a beam connecting the two beacons, floor to ceiling. A blocked beam means the pair is off.
  - Paired beacons trade vanilla's full-height column for a wide, height-limited box that fits mines, bases and crypts.

## Standalone (Reworked Beacons without Infused Gems) (owner, 2026-11-16)

Beacons keep vanilla's six powers, but the payment is a gem base item instead of iron, gold, emerald or diamond, and **the payment item is the power choice**. The base gem order is matched to vanilla's power order (which already follows vanilla's pyramid gating):

| Payment | Power | Pyramid needed |
|---|---|---|
| Amethyst shard | Speed | 1 |
| Quartz | Haste | 1 |
| Ender pearl | Resistance | 2 |
| Diamond | Jump Boost | 2 |
| Prismarine shard | Strength | 3 |
| Nether star | Regeneration | 3 (owner: all six work at level I by 3 layers; vanilla needed 4) |

- Emerald, iron, gold and netherite ingots are no longer payments.
- **A 4-layer pyramid automatically upgrades the paid power to level II** and reaches the furthest range (50); there is no secondary slot (owner).
- The screen describes the power in vanilla's potion-tooltip words ("Speed", "Speed II") in the effect's colour *(agent's choice)*.
- **With Infused Gems installed, these payments are switched off** (owner; Infused Gems' payload returns `replacesStandalonePayments()` true). Only finished gems and other mods' items (the Totem of Warding) fit the slot.

## Payload API (the hook other mods use)

Other mods (Infused Gems, Totems & Artifacts, Reworked Mobs) supply beacon items **without depending on this mod and without this mod knowing them**.

- Everything public lives in the package `io.github.bubba8587.bubbas_reworked_beacons.api`; nothing outside it is API.
- Keep it small and stable: add methods only as `default` methods.

**Registering.** Implement `BeaconPayload` (public no-argument constructor) and list it under the entrypoint key `"bubbas_reworked_beacons:payload"` in the supplying mod's `fabric.mod.json`:

```json
"entrypoints": { "bubbas_reworked_beacons:payload": ["io.github.bubba8587.bubbas_infused_gems.compat.GemBeaconPayload"] }
```

- Compile against this mod only (`compileOnly project(':bubbas_reworked_beacons')`, or the published jar), and list it under `"suggests"`, not `"depends"`.
- Without Reworked Beacons the entrypoint is never loaded, so the payload class (the only class that touches the API) never loads.
- Payloads are created lazily, the first time a beacon needs them, after every mod has initialised.
- Code outside the payload that calls `BeaconAreas` or `BeaconSlots` must check `FabricLoader.getInstance().isModLoaded("bubbas_reworked_beacons")` first.

**`BeaconPayload`** (one per supplying mod):

| Method | Meaning |
|---|---|
| `boolean accepts(ItemStack)` | Whether this payload handles the stack. The beacon slot only accepts stacks some payload accepts. |
| `int priority()` (default 0) | Highest wins when several accept the same stack. The standalone payments are -1000. |
| `int power(ItemStack)` (default 1) | The item's Gem Power, 1-6. The beacon caps it by pyramid (1 layer I, 2 III, 3 V, 4 VI) and shows "Only outputting Gem Power N" when it does. |
| `int requiredLevels(ItemStack)` (default 1) | Pyramid layers needed before it works at all; below that the screen says "Needs a N-layer pyramid" and the beacon idles. |
| `List<Component> describe(ItemStack, BeaconContext)` | The screen text, in the item's own tooltip words. Client and server; no world access. `context.levels()` is at least 1 here. |
| `void apply(ServerLevel, ItemStack, BeaconContext)` | One pulse (every 80 ticks, vanilla's rhythm) of an active beacon, server only. `context.livingEntities()` is every living thing in the area, players and mobs. Effects should last `context.effectDurationTicks()` (vanilla's (9 + 2 x layers) s). |
| `boolean replacesStandalonePayments()` (default false) | True switches the standalone gem-base payments off while that mod is installed. |

`priority()` and `replacesStandalonePayments()` are API extras *(agent's choice)*, used by Infused Gems.

**`BeaconContext`:**

- `level()`, `pos()` (the item's beacon; for a pair, the floor one), `mode()` (`BeaconMode.SKY` or `PAIRED`), `levels()` (1-4; for a pair, the smaller pyramid), `powerCap()`, `effectivePower()` (the item's power after the cap), `range()`, `area()` (an `AABB`), `effectDurationTicks()`, `livingEntities()`.
- Static rules, public for other mods' tests: `powerCapFor(levels)`, `skyRange(levels)`, `pairedRange(levels)`, `skyArea(level, pos, levels)`, `pairedArea(floor, floorLevels, ceilingY, ceilingLevels, levels)`.

**`BeaconAreas`** (server, main thread):

- `isInside(level, BlockPos or Vec3, Predicate<ItemStack>)` answers "is this inside an active beacon area supplied by item X" (Warding's spawn check).
- `at(level, Vec3)` lists the active beacons covering a point; `all(level)` lists every active one.
- Each `ActiveBeacon` has `pos, mode, levels, effectivePower, area, item, lastPulse`.
- A beacon is listed from its first pulse with a working item until it goes idle, is broken or unloads; an entry that missed two pulses counts as gone.
- Cheap enough for spawn checks: one list per level, one box test per active beacon.

**`BeaconSlots`** (server): `get(level, pos)` reads the item in a beacon, and `set(level, pos, stack)` puts one in (false when there's no beacon there). For structures that come with a loaded beacon: Reworked Mobs' Ancient Fabricators put their Fabricator Heart in theirs.

**Standalone payments are a payload too** (`beacon/StandalonePayload`, registered under the same key in this mod's own `fabric.mod.json`), so the hook is exercised by this mod's own tests.

## Totem of Warding (Totems & Artifacts side, for reference)

- Warding is not a gem: it's an artifact, the **Totem of Warding**, a found item like the Grave Talisman, owned by Bubba's Totems & Artifacts (so it never goes on armor or weapons). Its payload is that mod's `compat/WardingBeaconPayload`.
- Found in a mix of places, more common than the Totem of Undying (owner).
- In a reworked beacon it wards the beacon's area, at the normal range: any pyramid (1-3 layers) stops natural monster spawns and keeps monsters out; 4 layers also keep sleepers asleep (owner).
- The sleepers line on the screen shows only when Reworked Mobs is installed.
- Details: `specs/totems-artifacts.md`, Totem of Warding.

## As built

Technical choices, not leaves: change them when a better way turns up.

- No new blocks or items (B15).
- Required on client and server: `ExtraStatesHandshake` turns away a client without the mod (it lacks the added block state).

**Screen and slot.**

- `BeaconBlockEntityMixin` adds the kept item (saved as `bubbas_reworked_beacons:item` in the block entity) and the pair state.
- It cancels vanilla `applyEffects`, which also neutralises an old saved `primary_effect`, so existing beacons keep working as sky beacons with no power.
- It replaces `createMenu` with `BeaconItemMenu` (own `MenuType` `bubbas_reworked_beacons:beacon`; lock codes still respected) and drops the item in `preRemoveSideEffects`.
- The menu's slot writes straight to the block entity (`BeaconSlotContainer`), so two viewers can't duplicate the item. The block entity is deliberately not a `Container`, so hoppers can't fill it.
- Changing the item drops the beacon's area at once; it comes back on the next pulse with the new item.
- **Corner particles (owner, 2026-11-25):** when the effect changes (item, effective power or area, compared by a hash each pulse; not on the first pulse after loading, not when switching off), the server sends a column of dust (`EDGE_COLUMN` 7 particles half a block apart, scale `EDGE_SCALE` 3) at each corner of the area, every `EDGE_BURST_TICKS` 10 for `EDGE_FLASH_TICKS` 60 (`BeaconLogic.noteEdge` / `showCorners`), forced so they show from afar. Sky: on the ground (motion-blocking height) at each corner; paired: at both beacons' heights. Colour from the payload: `BeaconPayload.color(stack)` (API, default white): standalone and gems use their effect's colour, each totem its own, the Fabricator heart teal. No persistent box (owner). Test: `BeaconEdgeGameTest.cornersShowOnlyWhenTheEffectChanges` (milestone); client shots `edge-corner`, `edge-square`.
- `BeaconItemScreen` paints over vanilla's `beacon.png` power panels and payment row (no new texture).
- Screen text *(wording and the item-name line: agent's choice)*:
  - the state: "Sky beacon: N-layer pyramid, R blocks out", "Paired beacons: N-layer pyramids, R blocks out", "Idle: needs a pyramid below", "Idle: needs a clear sky above, or an upside-down beacon to pair with", "No item";
  - the item's name above its `describe` lines;
  - then "Needs a N-layer pyramid" (red) or "Only outputting Gem Power N" (yellow).

**Pulse.**

- `BeaconLogic`: pair check every 20 ticks on both sides (the client needs it for the beam); pulse every 80 ticks on the server after vanilla's tick (vanilla still scans the sky beam and counts the pyramid).
- Effects from the standalone payload are ambient with visible particles, vanilla style; spectators are not affected *(agent's choice)*.
- Vanilla's beacon activate, deactivate and ambient sounds play for pairs as for sky beacons.

**Upside-down beacons** (B15 approach C, like Extra Recipes' synthetic ore).

- An `upside_down` boolean state added to `minecraft:beacon` (`HangingBeacon`, `BlockExtraPropertyMixin`), default false; placing against a DOWN face sets it.
- The model is vanilla's rotated `x: 180` (`assets/minecraft/blockstates/beacon.json`), so there is no new texture and nothing for B16 to ship.
- Technique notes:
  - `BooleanProperty` lists `true` first, so the mixin sets `false` as the default explicitly.
  - The beacon doesn't override placement, so the mixin hooks `Block`'s own `getStateForPlacement`.
  - `HangingBeacon` is loaded while `Blocks` initialises, so it must not touch any registry.
  - The handshake is needed because the added state shifts every block state id after the beacon, and Fabric's registry sync sends nothing for state-only changes. The beacon `MenuType` is a modded registry entry that registry sync already checks; the handshake keeps the rule explicit.

**Pairs.**

- The scan goes straight up from the floor beacon to the WORLD_SURFACE height using vanilla's beam rule (blocks with light dampening 15 stop it, bedrock doesn't); an upright beacon in between ends it.
- No maximum gap between the two *(agent's choice)*.
- A pair takes priority over a sky beam when a floor beacon has both *(agent's choice)*.
- A pair's beam shows whenever both pyramids and the clear line exist, item or not, like vanilla's sky beam without a payment *(agent's choice)*. `BeaconRendererMixin` stops its last section at the upside-down beacon instead of running to y 2048.

**Prior art (`tools/prior-art.py`).** Beacon mods change range (Beacon Range, Extended Beacon Range), base blocks (PurpurPack bases), payments (Potion Beacons: potions as beacon effects, CC-BY-SA) or let beacons work underground (Underground Beacons, ARR; Underground Beacon, MIT, 26.2). None offers a one-slot item beacon with an API for other mods, mobs in range, a pyramid power cap or upside-down pairs. Hand-rolled because no library does it (B9): it needs a custom menu plus a block entity mixin.

## Open

- **Builder's choices to review** (marked *(agent's choice)* above): screen wording and the item-name line; standalone effects ambient with particles, spectators unaffected; no maximum gap between paired beacons; a pair beats a sky beam on the same floor beacon; a pair's beam shows even with no item; the colour of standalone power lines.
- **Gem strength in a beacon:** half a full netherite set (`BEACON_SHARE` 0.5, `specs/effect-gems.md`), approved for now (owner); revisit after playtesting.
- **Compatibility before release:** test against Beyond Basic Beacons / Potion Beacons (likely mark incompatible) and Underground Beacons / Extended Beacon Range (BACKLOG).
- Not done: a client screenshot of the beacon screen holding a gem (for the player docs).

## Milestone test objectives

**Core suite** (`ReworkedBeaconsCoreGameTest`, 4 tests; bodies in `BeaconGameTest`, which has no tests of its own):

- Standalone payment: an amethyst shard on a 1-layer sky beacon gives Speed I to a pig (real ticks).
- Pyramid cap: the standalone table, pyramid caps, and a test payload (Gem Power V echo shard, registered through the entrypoint from the test mod) capped to III on 2 layers, with `BeaconAreas.isInside` true at 25 blocks and false at 35.
- Paired box: a pair through glass gives the 3x-wide, height-limited box, its pulse reaches a pig, and stone in the beam turns it off.
- Hanging beacon: a beacon placed under a block hangs upside down; on top of one it stands upright.

**Milestone objectives without code yet:** 4 layers give level II; an ender pearl on 1 layer does nothing and the screen says "Needs a 2-layer pyramid"; the item survives save/reload and drops when the beacon breaks; two players with the screen open can't duplicate the item; a client without the mod is turned away.

**Client test part `beacons`** (`BubbasReworkedBeaconsClientGameTest`, milestone): sky beam, sky screen with an amethyst shard, paired beam in a closed room, paired screen with a capped payload.

The payload users' own objectives: `GemBeaconGameTest` (`specs/effect-gems.md`) and `WardingBeaconGameTest` (`specs/totems-artifacts.md`).
