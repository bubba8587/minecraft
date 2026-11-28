<!-- dte:C8 -->
# World Tweaks docs report, 2026-11-27: Amethyst Golem (phase 1 skeleton + phase 2 behaviour)

For the docs merge into `specs/world-tweaks.md` ("Amethyst Golem": change "designed, not built" to built) and `player-docs/`.

## Player-facing

- New mob: **Amethyst Golem**. Build it by placing a carved pumpkin or a jack o'lantern on top of one amethyst block (by hand or with a dispenser, like the other golems). Both blocks are used up and the golem appears where the amethyst block was.
- Spawn egg: "Amethyst Golem Spawn Egg", in the creative Spawn Eggs tab right after the Copper Golem's egg.
- **Recording:** right-click it with an empty hand: it rings low, cocks its head and lifts a hand to listen. Every note block played within 16 blocks is stored (pitch and timing; a note pops above its head for each). Right-click again to stop: a bright chime and green sparkles mean it kept the tune; a dull crack and smoke mean it heard nothing and its song is wiped. It stops by itself after 5 minutes.
- **Singing:** it plays its tune back as amethyst chimes at the recorded pitches, with coloured note particles, head tipped back.
- **Tuning:** right-click with an amethyst shard (not used up): the whole song goes up a semitone, each note wrapping from the top back to the bottom like a note block. It chimes the new pitch.
- **Jukebox:** a golem claims the nearest free jukebox within 16 blocks as its spot (one golem per jukebox), walks to it and stands on top. There it sings its song over and over. A jukebox placed before the mod was installed only counts once it is broken and placed again (see "Not vanilla" below).
- **Dancing:** when a jukebox within 10 blocks plays a disc, it stops and dances (arms up, rocking), like an allay.
- **Synced starts:** a song only starts on the beat (every 20 ticks of the day clock, 1 s; owner 2026-11-28: the first 1000-tick hour beat left golems silent most of the time). A song longer than a beat waits for whole multiples of its length in beats, so every golem with the same song is always on the same bar; a newcomer joins at the next start of the song.
- **Loudness:** its singing uses its own sound, vanilla's amethyst shimmer at full volume (vanilla's chime plays it at 20%, which the owner found nearly inaudible).
- **Wind chime:** a golem with no song chimes when it feels a vibration within 8 blocks (the sculk sensor's range) of vanilla frequency 2 or more (anything louder than footsteps: landing, splashes, doors, eating, block placing, explosions...). Higher frequency, higher note. Sneaking and wool muffle it like they do for sculk.
- Drops nothing.

## Spec facts (how it's built)

- Entity `bubbas_world_tweaks:amethyst_golem`, `golem.AmethystGolem extends AbstractGolem implements VibrationSystem`. `MobCategory.MISC`, size 0.49 x 0.98, eye height 0.8125, tracking range 10, 12 health, 0.2 speed, step height 1 (copper golem's numbers).
- Goals (vanilla goal system): float; hold still while recording or dancing; stand on its jukebox (paths to the block above the jukebox, re-paths every 20 ticks until on it, then holds MOVE so strolling never pulls it off); water-avoiding stroll; look at player; random look.
- Build pattern: `mixin/CarvedPumpkinAmethystGolemMixin` hooks vanilla `CarvedPumpkinBlock.trySpawnGolem` (HEAD) and `canSpawnGolem` (RETURN, for dispensers); `aisle("^", "#")`, pumpkin or jack o'lantern on `minecraft:amethyst_block`; spawns through vanilla's `spawnGolemInWorld` via `@Invoker` (`mixin/CarvedPumpkinBlockInvoker`).
- **Listening (recording, jukeboxes):** one plain `GameEventListener` (`SongListener`, `DynamicGameEventListener`, delivered at once, no vibration travel time), radius = `NOTE_BLOCK_PLAY`'s notification radius (16, the allay's note block range). `NOTE_BLOCK_PLAY` while recording: reads `NOTE` from the note block at the event position (a non-tunable instrument, i.e. a mob head on top, is stored as note 12 = pitch 1.0). `JUKEBOX_PLAY` / `JUKEBOX_STOP_PLAY` start and stop dancing exactly like the allay (`setJukeboxPlaying`), and every 20 ticks it checks that disc jukebox is still within 10 blocks, still a jukebox and still has a record.
- **Recording format** (`golem/GolemSong`): notes packed as `tick << 5 | note` in an int array, ticks counted from the first note heard (no leading silence), sorted. Cap `GOLEM_RECORD_CAP_TICKS` 6000 (5 min, auto stop) and `GOLEM_MAX_NOTES` 4096. Saved in entity data as `song` (int array), with `tuning` (0-24) and `jukebox` (block pos). A recording in progress is not saved (unloading stops it).
- **Playback:** `SoundEvents.AMETHYST_BLOCK_CHIME` (vanilla amethyst block chime) at `NoteBlock.getPitchFromNote(note)`, volume 1, NEUTRAL source, plus a `NOTE` particle coloured `note / 24` (note block colours). Tuning: `(note + tuning) mod 25` per note.
- **Beat clock:** the dimension's default world clock (`Level.getDefaultClockTime()`, the day clock), or game time on any tick where the day clock stood still (gamerule `advance_time` false, or a dimension without a clock), so songs still play with the daylight cycle off. A song may start only when that clock is a multiple of its period = song length rounded up to whole beats (`GOLEM_BEAT_TICKS` 20). Since every golem reads the same clock, all golems with the same song start on the same tick and stay in step; after a song ends the next allowed start is exactly the next beat after the end. (Plain "any next beat" would put a newcomer half a song out of step with one already singing a song longer than a beat; for songs up to one beat long the two rules are the same.)
- **Wind chime:** vanilla's vibration system (`VibrationSystem.Listener/Data/Ticker`, like the allay and sculk sensor), radius `GOLEM_CHIME_RADIUS` 8. Accepts vibrations only while it has no song and isn't recording, with `VibrationSystem.getGameEventFrequency(event) >= GOLEM_CHIME_FLOOR` (2). Vanilla's own vibration rules still apply (they decide what a vibration is: spectators, sneaking over `ignore_vibrations_sneaking`, wool-dampened, wool occlusion), and it shows the vanilla vibration particle travelling to it. Note = `round((frequency - 1) * 24 / 14)` plus tuning. A chime holds the singing pose for 10 ticks.
- **No feedback loops:** its chimes, recording cues and particles are plain sounds (`level.playSound`) and particles, like an amethyst block hit by a projectile in vanilla: they emit no game event, so nothing (golem, sculk, wardens, Reworked Mobs sleepers) hears them as a vibration. It also only records note blocks, never other golems. No special-casing anywhere.
- **Jukebox claim:** a point of interest `bubbas_world_tweaks:golem_jukebox` on all jukebox states, 1 ticket, registered with vanilla's `PoiTypes.register` (widened by Fabric; Fabric's `PoiHelper` is deprecated for removal in this Fabric API). Every `GOLEM_JUKEBOX_CHECK_TICKS` (100, staggered by entity id) it checks its claim still exists and is within 32 blocks (else releases it), or claims the nearest jukebox with a free ticket within `GOLEM_JUKEBOX_RADIUS` (16) via `PoiManager.findClosest` + `take`. The ticket is released when the golem dies or is discarded; it stays claimed while the golem is unloaded (like a villager's job site).
- **Feedback cues:** recording start `AMETHYST_BLOCK_RESONATE` pitch 1.2 + note particles; stop with a song `AMETHYST_BLOCK_CHIME` pitch 1.6 + happy-villager sparkles; stop with nothing heard `AMETHYST_CLUSTER_BREAK` pitch 0.6 + smoke.
- Drops: empty loot table `data/bubbas_world_tweaks/loot_table/entities/amethyst_golem.json` (drops nothing, so never more amethyst than it was built from).
- Synced flags `dancing`, `singing` (a song playing, or within 10 ticks of a wind chime), `recording` drive `AmethystGolemRenderState`; poses in `client/golem/AmethystGolemAnimations`: dance (arms up and waving, rocking, head bob, crystals twist), sing (head tipped back, arms a little open, crystals shimmer, jaw hook `mouth()` ready for a jaw part), listen (head cocked, right hand raised to the side of the head).
- **Per-tick cost per golem (server):** vanilla vibration ticker (no-op when idle); a few comparisons for the song (an index pointer into the sorted notes, no allocation unless a note plays); every 20 ticks while dancing one block state read; every 100 ticks one POI lookup. No world scans.

### Not vanilla, and why (for the spec's list)

- Jukeboxes: vanilla has no jukebox POI, so the mod registers `golem_jukebox`. Vanilla only indexes POIs when a chunk section is first scanned or a block changes, so jukeboxes already in a world from before the mod was added aren't found until broken and replaced. (Owner may want a fix; one option is a one-time section rescan, which costs more.)

## Tests

- Core (`BubbasWorldTweaksCoreGameTest`, bodies in `AmethystGolemGameTest`):
  - `pumpkinOnAmethystBuildsAGolem`: a pumpkin on one amethyst block makes exactly one golem and clears both blocks.
  - `amethystGolemRecordsAndSings`: three real note blocks (notes 5, 17, 10) triggered by redstone 5 ticks apart while recording are stored as ticks 0/5/10; a golem on its jukebox doesn't start off the beat, then sings 5, 17, 10 at the right ticks; one shard step later it sings 6, 18, 11.
  - `amethystGolemEmptyRecordingWipes`: two clicks with nothing heard wipe the song.
  - `amethystGolemSongsStartOnTheBeat`: a song a little over one beat long has a period of two beats; the first golem starts on a period boundary; a golem that arrives mid-song sings exactly in step with the first from its first note.
  - `amethystGolemWindChimeFloor`: a step (frequency 1) nearby makes no chime; a landing (frequency 2) makes one.
- Client part `golem` (`AmethystGolemClientGameTest`): the phase 1 shots, then (16x only) `golem-recording`, `golem-recording-side`; jukebox placed under it, a long song set, day clock run up to a beat, asserts it is singing, `golem-singing`, `golem-singing-side`; a disc put in the jukebox, asserts it is dancing, `golem-dancing`, `golem-dancing-side`.
- Milestone test objectives (no test yet):
  - A jack o'lantern also builds it; a dispenser facing the top of an amethyst block places a carved pumpkin and builds it; budding amethyst does not.
  - Recording stops itself after 5 minutes; notes beyond 16 blocks are not heard; a mob-head note block records as pitch 1.0.
  - The shard tune wraps after 25 steps back to the original; the shard is not used up.
  - Two golems near one jukebox: only one claims it; the other claims a second jukebox; killing the first frees the jukebox for the other; a golem walks to its jukebox and stays on top.
  - A golem dances when a jukebox within 10 blocks starts a disc and stops when the disc ends or it is taken out; it doesn't sing while dancing.
  - Song, tuning and jukebox survive save and reload.
  - With `advance_time` false songs still start (game time beats).
  - Wind chime: a golem with a song or while recording doesn't chime; wool between source and golem blocks the chime.

## Builder's choices (owner may want to weigh in)

- Spawn spot is the amethyst block's position.
- **Singing needs the jukebox:** a golem with a song but no jukebox (or not yet on it) is silent and wanders. Owner may prefer it to sing anywhere.
- **Tuning wraps per note** (`(note + steps) mod 25`), the literal "like a note block"; a melody spanning the top of the range gets a note jumping two octaves down. Alternative: shift the whole song and wrap it down as a block.
- Long songs start on multiples of their own length in beats, not on every beat (see Beat clock above): that is what keeps golems with the same song in step.
- Wind chime radius 8 (sculk sensor) rather than the allay's 16, so it reacts to its surroundings and not the whole base.
- Drops nothing.
- Recording a note within 16 blocks uses the event's notification radius (16), same as how far the allay hears note blocks.

## Singing follows the music (animation builder, 2026-11-27)

Player-facing (replaces "head tipped back" in the Singing bullet):
- **Singing:** with each note its mouth pops open and eases shut and the crystals on its head flick and swell; higher notes tip its head further back. Between notes it sways side to side in time with the tune. Wind chimes get the same per-note mouth and crystal pop.
- **Waiting for the beat:** a golem with a song standing on its jukebox, between songs, waits calmly with its head a little bowed and its hands folded in front, mouth closed, until the next beat starts its song.

Spec facts:
- Synced flags: `singing` now means performing (a song in progress, or within 10 ticks of a wind chime); new `waiting` = has a song, on its jukebox, not dancing or recording, no song in progress. Client blends both poses in and out over 5 ticks (`perform`/`wait` floats stepped 0.2 per client tick, interpolated by partial ticks).
- **Per-note sync:** every chime (song note, shard tuning chime, wind chime) calls `level.broadcastEntityEvent(golem, 100 + note)` (entity events 100-124; vanilla uses up to 72). Clients decode it in `AmethystGolem.handleEntityEvent` and record the note's entity tick, pitch, previous pitch, gap since the previous note and the sway step. Cost: one `ClientboundEntityEventPacket` (entity id + 1 byte) per note per tracking player, only to players tracking the golem; nothing per tick. Notes sharing a tick (chords) count once for timing and keep the highest pitch.
- **Animation** (`client/golem/AmethystGolemAnimations`, all from `ageInTicks`/partial ticks via the render state's `sinceNote`):
  - Note envelope: 1.5 tick attack, 5.5 tick quadratic decay. A note that lands before the last one decayed starts its attack from the level the last one had reached, so fast passages keep the mouth fluttering open instead of snapping shut.
  - Jaw: `0.55 rad x envelope` (plus 0.12 rad held open while performing).
  - Crystals: each of the four crystals rings with a damped flick (`sin(1.9 s + phase) x e^(-0.35 s)`, 0.16-0.22 rad, stronger for higher notes), and the crystal cluster swells up to 14% with the envelope. Ring ends 14 ticks after the note.
  - Head: tipped back 0.2 + 0.3 x pitch rad (pitch 0-1 over the note block range), eased over 3 ticks from the previous note's tilt, plus a small pop with the envelope.
  - Sway: body and head roll to alternate sides; one half swing lasts as many notes as fit in about 12 ticks (`round(12 / gap)` notes, gap clamped 2-36 ticks), eased with smoothstep; a note after a finished swing starts a new one.
  - Waiting pose: head bowed 0.2 rad with a slow breath, head turning damped, arms forward 0.75 rad and turned inward so the hands fold in front.
  - Dance and listen poses unchanged; they take priority over sing and wait.
- Client cost: a handful of floats per golem per frame; no allocation.

Tests:
- Core `amethystGolemRecordsAndSings` now also asserts every sung note fires exactly one note event (`notesSung` counter advances by the notes sung that tick), and that a golem receiving `noteEvent(n)` through `handleEntityEvent` reads back note `n`, its tick and the gap.
- Client part `golem` `states()`: a 30-note song with notes 16 ticks apart; the server is frozen (`tickRateManager().setFrozen`) exactly 1 tick after a note for `golem-singing-note`, `-side`, `-close`, and 8 ticks after one for `golem-singing-between`, `-side`; then the song is reset and the clock set off the beat, asserts `isWaiting && !isSinging` on the client, `golem-waiting`, `golem-waiting-side`. (Replaces `golem-singing`, `golem-singing-side`.)
- Milestone objectives: a chord (two notes the same tick) opens the mouth once at the higher pitch; the waiting flag clears when it is pushed off its jukebox, starts dancing or recording.
