<!-- dte:C5 -->
<!-- covers: mods/bubbas_reworked_beacons/* -->
# Bubba's Reworked Beacons (spec)

A separate mod that reworks vanilla beacons into a general "area" system. Mod id `bubbas_reworked_beacons`, code in `mods/bubbas_reworked_beacons/`, Java package `io.github.bubba8587.bubbas_reworked_beacons`. Design history and the options considered: `docs/research/area-gems-and-warding.md`.

## Payload API (the hook other mods use)

Other mods (Infused Gems, Reworked Mobs) supply beacon items **without depending on this mod and without this mod knowing them**. Everything public lives in the package `io.github.bubba8587.bubbas_reworked_beacons.api`; nothing outside it is API. Keep it small and stable: add methods only as `default` methods.

**Registering.** Implement `BeaconPayload` (public no-argument constructor) and list it under the entrypoint key `"bubbas_reworked_beacons:payload"` in the supplying mod's `fabric.mod.json`:

```json
"entrypoints": { "bubbas_reworked_beacons:payload": ["io.github.bubba8587.bubbas_infused_gems.compat.GemBeaconPayload"] }
```

Compile against this mod only (`compileOnly project(':bubbas_reworked_beacons')`, or the published jar), and add it under `"suggests"`, not `"depends"`. Without Reworked Beacons the entrypoint is never loaded, so the payload class (the only class that touches the API) never loads. Payloads are created lazily, the first time a beacon needs them, after every mod has initialised. Code outside the payload that calls `BeaconAreas` must check `FabricLoader.getInstance().isModLoaded("bubbas_reworked_beacons")` first.

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

**`BeaconContext`**: `level()`, `pos()` (the item's beacon; for a pair, the floor one), `mode()` (`BeaconMode.SKY` or `PAIRED`), `levels()` (1-4; a pair's smaller pyramid), `powerCap()`, `effectivePower()` (the item's power after the cap), `range()`, `area()` (an `AABB`), `effectDurationTicks()`, `livingEntities()`. Static rules, public for other mods' tests: `powerCapFor(levels)`, `skyRange(levels)`, `pairedRange(levels)`, `skyArea(level, pos, levels)`, `pairedArea(floor, floorLevels, ceilingY, ceilingLevels, levels)`.

**`BeaconAreas`** (server, main thread): `isInside(level, BlockPos or Vec3, Predicate<ItemStack>)` answers "is this inside an active beacon area supplied by item X" (Warding's spawn check), `at(level, Vec3)` lists the active beacons covering a point, `all(level)` lists every active one. Each `ActiveBeacon` has `pos, mode, levels, effectivePower, area, item, lastPulse`. A beacon is listed from its first pulse with a working item until it goes idle, is broken or unloads; an entry that missed two pulses counts as gone.

**Standalone payments are a payload too** (`beacon/StandalonePayload`, registered under the same key in this mod's own `fabric.mod.json`), so the hook is exercised by this mod's own tests.

## Why its own mod (owner, 2026-11-16)

Area effects are needed by two projects: Bubba's Infused Gems (gems broadcasting their effect) and Bubba's Reworked Mobs (Warding: mob-free zones without spamming torches). The owner split the beacon rework out so all three mods stay independent:

| Installed | Beacons |
|---|---|
| Reworked Beacons alone | the reworked beacon, but nothing to put in it until another mod supplies items (to decide: should vanilla payments still work alone?) |
| + Infused Gems | gems go in the slot and broadcast their effect |
| + Reworked Mobs | the Totem of Warding goes in the slot and wards the area |
| Gems or Mobs without Beacons | beacons stay vanilla; those mods work exactly as they do now |

Reworked Beacons offers one hook: "this item in a beacon does X at power Y". Infused Gems and Reworked Mobs answer it only when Reworked Beacons is present (optional dependency), so none of the three depends on another.

## The beacon (owner decisions)

- **One item slot** replaces vanilla's primary/secondary powers and the iron/gold/emerald/diamond payment. The screen says what the beacon will do, in the item's own tooltip words.
- **The item stays in the beacon** (removable), not consumed. Revisit if too strong; the beacon itself already costs a nether star.
- **Pyramid size caps the item's power:** 1 layer Gem Power I, 2 layers up to III, 3 layers up to V, 4 layers VI. A stronger item is capped, with a message like "Only outputting Gem Power III".
- **Affects every living thing in range, mobs included** (owner's equality principle): a Regeneration gem heals nearby mobs too; a harmful gem makes a trap for everyone.

## Range

- **Sky beacons: vanilla's area exactly.** A square box out to the range (20 / 30 / 40 / 50 blocks for 1-4 layers) sideways and downward, extended up to the build limit (26.3 `BeaconBlockEntity`: `new AABB(pos).inflate(range).expandTowards(0, height, 0)`).
- **Underground: paired beacons.** Beacons can be placed upside down (a new placement of the existing block, not a new block: B15). A beacon on the floor with its pyramid below pairs with an upside-down beacon directly above it, whose pyramid is built upside down above it; the space between must be clear or glass, like a beam. The upside-down beacon holds no item. The **smaller** pyramid sets the range: a square box **3x vanilla's range sideways** (60 / 90 / 120 / 150 blocks out) and vertically from **10 blocks below the floor pyramid to 10 blocks above the ceiling pyramid**. Paired beacons trade vanilla's full-height column for a wide, height-limited box that fits mines, bases and crypts.

## Totem of Warding (Reworked Mobs side, for reference)

Warding is not a gem: it's an artifact, the **Totem of Warding**, a found item like the Grave Talisman, owned by Reworked Mobs (so it never goes on armor or weapons). In a reworked beacon it wards the beacon's area: no monster spawns, monsters avoid the area, sleepers stay asleep. Details are open in `docs/research/area-gems-and-warding.md`.

## Standalone (Reworked Beacons without Infused Gems) (owner, 2026-11-16)

Beacons keep vanilla's six powers, but the payment is a gem base item instead of iron, gold, emerald or diamond, and **the payment item is the power choice**, in the base gem order matched to vanilla's power order (which already follows vanilla's pyramid gating):

| Payment | Power | Pyramid needed (vanilla) |
|---|---|---|
| Amethyst shard | Speed | 1 |
| Quartz | Haste | 1 |
| Ender pearl | Resistance | 2 |
| Diamond | Jump Boost | 2 |
| Prismarine shard | Strength | 3 |
| Nether star | Regeneration | 3 (owner: all six work at level I by 3 layers; vanilla needed 4) |

Emerald, iron and gold are no longer payments. (With Infused Gems installed, the slot takes gems instead; see above.)

## Open questions (owner)

- (answered) Standalone 4-layer pyramid: **automatically upgrades the paid power to level II** and reaches the furthest range (50); no secondary slot (owner, 2026-11-16).
- (answered) Totem of Warding: found in a mix of places, more common than the Totem of Undying; in a beacon: 1-3 layers stop spawns and keep monsters out, 4 layers also keep sleepers asleep; range as normal. Details in the Warding doc.
- (answered) Upside-down placement: placing a beacon against the underside of a block hangs it upside down, like a hanging lantern. An upside-down beacon does nothing on its own: it only closes a pair (owner, 2026-11-16).
- (answered) A pair shows a beam connecting the two beacons, floor to ceiling, as its status light: blocked beam = ward off (owner).
- (answered) Harmful gems in a beacon hit everyone, the builder included: **no protection** (owner). Players can counter with gems (e.g. matching resistances), and it enables designed AoE debuff environments (e.g. hostile structures, boss arenas).

## Milestone test objectives

None yet (not built).
