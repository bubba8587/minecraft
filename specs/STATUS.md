<!-- dte:A3 -->
# Status board

Every feature of every project, and where it stands. Last checked against the code on `develop`: 2026-11-24.

| Mark | Means |
|---|---|
| **built** | in `develop` now, playable |
| **in progress** | an agent is building it (branch named) |
| **designed** | decided by the owner, nobody building it yet |
| **parked** | waiting; see `BACKLOG.md` |
| **dropped** | decided against; kept here so nobody builds it |

**Keeping it current:**
- Whoever merges a branch moves its lines to **built** in the same commit.
- Whoever starts a feature marks it **in progress** with the branch name.
- A newly decided feature gets a **designed** line.
- Before marking a line **built**, check it in the mod's source; the specs are not proof.
- Each line is one short phrase and a link to the spec section.
- Owner decisions still open are in each spec's "Open" section, not here.

Nothing is released yet: every mod is 0.1.0 in `develop`.

## Bubba's Reworked Mobs

**Undead** ([spec](undead.md))
- built: sleeping undead as blocks, noise meter, stirring, chain waking ([§](undead.md#hearing-and-waking))
- built: face-down sleepers for every kind ([§](undead.md#face-down-sleepers))
- built: waking in tight spots ([§](undead.md#waking-in-tight-spots))
- built: husk sleepers burst from sand, stray sleepers crack out of ice ([§](undead.md#undead-variants))
- built: bogged lurk in water instead of sleeping ([§](undead.md#undead-variants))
- built: robbing sleepers ([§](undead.md#robbing))
- built: mining and placing sleepers with Silk Touch ([§](undead.md#mining-and-placing-sleepers))
- built: loot on display, buried loot, undead grab loot ([§](undead.md#loot-on-display))
- built: woken undead: armor-shred streaks, Smite kills, arrow rules, zombie shamble, drifting into groups ([§](undead.md#tuning))
- built: light and bells calm or wake sleepers ([§](undead.md#calming-sleepers-light-and-bells))
- built: natural monster spawns cut to 15 % ([§](undead.md#natural-spawning))
- built: back to sleep at dawn ([§](undead.md#back-to-sleep-at-dawn))
- built: fire as a weak ward ([§](undead.md#fire-as-a-weak-ward))
- built: drowned sleepers in shipwrecks ([§](undead.md#drowned-sleepers-in-shipwrecks))
- built: sneak + middle click lays the held item on the ground anywhere ([§](undead.md#placing-displays))
- designed: hybrid sleeper bodies and real 16x/32x body art, before release ([§](undead.md#dormant-undead-are-real-blocks))
- parked: the bury-break system, more sleeper kinds ([§](undead.md#scope-and-variants))

**Graveyards** ([spec](graveyards.md))
- built: gravestone block ([§](graveyards.md#gravestones))
- built: crypt yards, 4 sizes, 5 states of decay ([§](graveyards.md#crypt-yards-surface))
- built: the resting dead ([§](graveyards.md#the-resting-dead))
- built: pauper, family and noble crypts ([§](graveyards.md#crypts-underground))
- built: noble crypt burial goods ([§](graveyards.md#noble-crypt-loot))
- built: desert and snowy yards ([§](graveyards.md#desert-and-snowy-yards))
- built: will-o'-wisps ([§](graveyards.md#will-o-wisps))
- built: noble dead in gemmed armor (with Infused Gems) ([§](graveyards.md#noble-dead-in-gemmed-armor))
- designed: standalone hillside crypts (owner to confirm) ([§](graveyards.md#crypts-underground))

**Monster camps** ([spec](monster-camps.md))
- built: camps with rosters, leaders, banner keys, daily reset ([§](monster-camps.md#what-a-camp-is))
- built: camp shade and underground room ([§](monster-camps.md#shade-and-the-underground-room-owner))
- built: a lit fire quiets a camp ([§](monster-camps.md#lighting-a-fire-quiets-a-camp-owner))
- built: pillager outposts as camps ([§](monster-camps.md#pillager-outposts-become-camps-too-owner))
- built: explorer maps from cartographers to graveyards, camps and Fabricators (the Fabricator map marks the Fabricator itself) ([§](monster-camps.md#maps-owner))
- built: Nether outposts, the piglin kind: crimson forest and Nether wastes, gilded gate, hoglin pen, portal room ([§](monster-camps.md#nether-outposts))
- built: trader posts; the wandering trader arrives at a nearby post with its llamas penned ([§](monster-camps.md#trader-posts))
- dropped: overworld piglin outposts (owner: vanilla zombification) ([§](monster-camps.md#nether-outposts))

**Spiders** ([spec](spiders.md))
- built: cave nests, dropping and latching spiders ([§](spiders.md#cave-nests))
- built: giant spiders ([§](spiders.md#giant-spiders-owner-2026-11-22))
- built: spun webs ([§](spiders.md#spun-webs-owner-2026-11-22-we-should-try-at-least-to-make-something-really-outstanding-looking-there))
- built: cocoons ([§](spiders.md#cocoons-owner-2026-11-22))
- built: giants prowl off the perch and haul to their own webs ([§](spiders.md#prowling-and-own-webs-backlog-playtest-fixes-5-and-6-built-2026-11-24))
- built: walls and ceilings (Stormie's Spiders, required) ([§](spiders.md#walls-and-ceilings-owner-2026-11-22))

**Creepers** ([spec](creepers.md))
- built: watcher creepers, startle meter, peekaboo, one per player at night ([§](creepers.md#how-its-built))
- built: charged and spawner creepers stay vanilla ([§](creepers.md#charged-creepers))
- built: `show_meters` game rule for the creeper and drowned meters (off by default) ([§](creepers.md#how-its-built))

**Drowned** ([spec](drowned.md))
- built: shore lurkers that sink when stared at ([§](drowned.md#the-lurker))
- built: grab by the ankle, drag to deep water, grip meter, extra holders ([§](drowned.md#how-its-built))
- built: no thrown tridents; tridents in buried treasure ([§](drowned.md#how-its-built))
- designed: a "drowned shore" test world station (idea) ([§](drowned.md#test-world-station-idea-designed-not-built))

**Endermen** ([spec](endermen.md))
- built: caretaker endermen repair terrain and fill holes ([§](endermen.md#two-jobs))
- built: cairns: sites, spawning, builders, vigil, glow ([§](endermen.md#how-its-built))
- built: seven cairn forms ([§](endermen.md#cairn-forms-built-2026-11-22-agent-2-owner-sure-add-as-much-variety-as-you-like))
- built: cairn offering, the ripple, the nexus (for Totems) ([§](endermen.md#cairn-gifts-the-ripple-and-the-nexus-built-2026-11-22-for-totems--artifacts))
- built: endermen trading at cairns (trade pad, random answers) ([§](endermen.md#trading-at-cairns-owner-2026-11-22-built))

**Ancient Fabricators** ([spec](mob-factory.md))
- built: a Fabricator beside every ancient city: look, teleporter, hidden spawner, Warden, debuffs, loot ([§](mob-factory.md#mechanics))
- built: secrets and the lower city ([§](mob-factory.md#secrets-and-the-lower-city))
- designed (ready to assign): Fabricators as real structures with their own placement grid, near ancient cities when possible; then `/locate` and Seeking point straight at them ([§](mob-factory.md#fabricators-as-real-structures-designed-not-built))
- parked: why challenge a Fabricator, office jukebox loop, totem of spawning ([§](mob-factory.md#parked))

## Bubba's Totems & Artifacts ([spec](totems-artifacts.md))
- built: Grave Talisman, glows near undead, in crypt chests ([§](totems-artifacts.md#grave-talisman))
- built: Totem of Warding, held and in a beacon ([§](totems-artifacts.md#totem-of-warding))
- built: Totem of Keeping ([§](totems-artifacts.md#totem-of-keeping-keepingkeeping-mixinplayerkeepingmixin))
- built: Totem of Gathering: held; in a beacon, a floating ring of items and XP for a side hopper ([§](totems-artifacts.md#totem-of-gathering-gatheringgathering-totemofgatheringitem-compatgatheringbeaconpayload))
- built: Totem of Fortification ([§](totems-artifacts.md#totem-of-fortification-fortificationfortification-mixinserverexplosionmixin-compatfortificationbeaconpayload))
- built: Totem of Growth, also plants dropped seeds ([§](totems-artifacts.md#totem-of-growth-working-name-growthgrowth-totemofgrowthitem-compatgrowthbeaconpayload))
- built: Totem of Seeking ([§](totems-artifacts.md#totem-of-seeking-seekingtotemofseekingitem-seekingtypes-structureseeker-seekingbeacon-mixincompassitemmixin-compatseekingbeaconpayload))
- built: Totem of Herding ([§](totems-artifacts.md#totem-of-herding-herdingherding))
- built: Totem of Clear Skies ([§](totems-artifacts.md#totem-of-clear-skies-skiesclearskies))
- built: Totem of the North Wind: a rocket boost while gliding; in a beacon or as a statue, for gliders in the area ([§](totems-artifacts.md#totem-of-the-north-wind-northwindtotemofnorthwinditem-northwindnorthwind-compatnorthwindbeaconpayload))
- built: Totem of the Beginning: a reusable pulse with a cooldown and per-block odds; beacons and statues pulse the ground directly every 15 s ([§](totems-artifacts.md#totem-of-the-beginning-beginningtotemofthebeginningitem-beginningbeginningareas-compatbeginningbeaconpayload))
- built: Totem of the End ([§](totems-artifacts.md#totem-of-the-end-endtotemoftheenditem-endendteleport-mixinendermankinmixin))
- built: a statue for every totem, giving its totem's effect in a small radius ([§](totems-artifacts.md#totem-statue-block-b15-exception))
- built: relic map shreds, combined into relic maps that lead to seeded relic vaults; vaults behind some noble crypts ([§](totems-artifacts.md#relic-maps-and-relic-vaults))
- parked: Grave Talisman as a rare sleeper drop ([§](totems-artifacts.md#grave-talisman))

## Bubba's Infused Gems ([spec](effect-gems.md))
- built: brew, enchant, socket and grind gems; 39 effects worn and on weapons ([§](effect-gems.md#as-built-2026-11-14))
- built: gems beyond potions: Wind Burst, Weaving, Oozing, Silence, Ender, Breeze ([§](effect-gems.md#as-built-2026-11-14))
- built: gem inversion at the brewing stand ([§](effect-gems.md#as-built-2026-11-14))
- built: Turtle Master shell up only while sneaking ([§](effect-gems.md#owners-answers-to-the-balance-models-questions-2026-11-14))
- built: Absorption refill shown as a cooldown sweep ([§](effect-gems.md#as-built-2026-11-14))
- built: loot gems and gemmed mob gear ([§](effect-gems.md#as-built-2026-11-14))
- built: gems in Reworked Beacons ([§](effect-gems.md#gems-in-reworked-beacons-owner-2026-11-16))
- built: `/gems` and `/gems kit` ([§](effect-gems.md#as-built-2026-11-14))
- parked: Frost gem, waiting for vanilla's freezing effect ([§](effect-gems.md#open))
- parked: per-weapon tuning; Silence and Breeze on weapons ([§](effect-gems.md#open))

## Bubba's Reworked Beacons ([spec](reworked-beacons.md))
- built: one item slot; payload API for other mods ([§](reworked-beacons.md#payload-api-the-hook-other-mods-use))
- built: pyramid caps Gem Power; every living thing in range ([§](reworked-beacons.md#the-beacon-owner-decisions))
- built: standalone payments for the six vanilla powers ([§](reworked-beacons.md#standalone-reworked-beacons-without-infused-gems-owner-2026-11-16))
- built: upside-down beacons and underground pairs ([§](reworked-beacons.md#range))
- parked: compatibility tests against other beacon mods ([§](reworked-beacons.md#open))

## Bubba's Reworked Mining ([spec](reworked-mining.md))
- built: steep curve for every tool tier on stone and deepslate ([§](reworked-mining.md#the-curve-draft-numbers-owner-to-tune-by-playing))
- built: wood and gold can't mine deepslate; axes, shovels, hoes on a gentler curve ([§](reworked-mining.md#the-curve-draft-numbers-owner-to-tune-by-playing))
- built: stronger Efficiency and Unbreaking ([§](reworked-mining.md#numbers-miningtuning))
- built: rare finds from stone and deepslate ([§](reworked-mining.md#rare-finds-in-plain-stone-owner-2026-11-22))
- built: True Unbreaking, a rare loot book ([§](reworked-mining.md#true-unbreaking-owner-2026-11-22))
- built: quartz tools and armor for the Nether ([§](reworked-mining.md#quartz-tools-and-armor-owner-2026-11-22))

## Bubba's Extra Recipes ([datapack spec](extra-recipes.md), [mod spec](extra-recipes-mod.md))
- built: datapack 1.1.0 with 16 recipes ([§](extra-recipes.md))
- built: companion mod bundling the datapack and 16x/32x packs ([§](extra-recipes-mod.md#bundled-packs))
- built: the Glider ([§](extra-recipes-mod.md#glider))
- built: Synthetic Diamond Ore ([§](extra-recipes-mod.md#synthetic-diamond-ore))
- parked: person-made Glider art and mod icon ([§](extra-recipes-mod.md#open))

## Bubba's World Tweaks ([spec](world-tweaks.md))
- built: lava bubbles over magma ([§](world-tweaks.md#as-built-2026-11-22-agent-3))
- built: lava geysers with black smoke ([§](world-tweaks.md#as-built-2026-11-22-agent-3))
- built: lift and speed from lava and sulfur geysers ([§](world-tweaks.md#as-built-2026-11-22-agent-3))
- built: bubble art redrawn from vanilla lava, soot plume, slow and sparse bubbles ([§](world-tweaks.md#art-redo-and-slower-bubbles-2026-11-22-agent-3-owner-feedback))

## Repo
- designed (low priority, owner): settings for every mod: game rules in a per-mod category, structure datapacks at world creation, optional Mod Menu button ([§](settings.md))
- built: test world with stations for most features, and a natural world with examples of the rest; gaps listed in [testworld.md](testworld.md#stations)
- parked: compatibility tests before releases (`BACKLOG.md`, Repo)
