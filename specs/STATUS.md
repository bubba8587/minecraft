<!-- dte:A3 -->
# Status board

Every feature of every project, and where it stands. Last checked against the code on `develop`: 2026-11-22.

| Mark | Means |
|---|---|
| **built** | in `develop` now, playable |
| **in progress** | an agent is building it (branch named) |
| **designed** | decided by the owner, nobody building it yet |
| **parked** | waiting; see `BACKLOG.md` |

**Keeping it current.** Whoever merges a branch moves its lines to **built** in the same commit. Whoever starts a feature marks it **in progress** with the branch name. A newly decided feature gets a **designed** line. Before marking a line **built**, check it in the mod's source; the specs are not proof. Each line is one short phrase and a link to the spec section. Owner decisions still open are in each spec's "Open" section, not here.

Nothing is released yet: every mod is 0.1.0 in `develop`.

## Bubba's Reworked Mobs

**Undead** ([spec](undead.md))
- built: sleeping undead as blocks, noise meter, stirring, chain waking ([§](undead.md#hearing-and-waking))
- built: waking in tight spots ([§](undead.md#waking-in-tight-spots-2026-11-22))
- built: husk sleepers burst from sand, stray sleepers crack out of ice ([§](undead.md#undead-variants-owner-2026-11-22))
- built: bogged lurk in water instead of sleeping ([§](undead.md#undead-variants-owner-2026-11-22))
- built: robbing sleepers ([§](undead.md#robbing))
- built: mining and placing sleepers with Silk Touch ([§](undead.md#mining-and-placing-sleepers))
- built: loot on display, buried loot, undead grab loot ([§](undead.md#loot-on-display-shared-block))
- built: woken undead: armor-shred streaks, Smite kills, arrow rules, zombie shamble, drifting into groups ([§](undead.md#tuning))
- built: light and bells calm or wake sleepers ([§](undead.md#calming-sleepers-light-and-bells))
- built: natural monster spawns cut to 15 % ([§](undead.md#natural-spawning))
- in progress (undead-2): back to sleep at dawn ([§](undead.md#back-to-sleep-at-dawn-owner-2026-11-22-in-progress-agent-2))
- in progress (undead-2): fire as a weak ward ([§](undead.md#fire-as-a-weak-ward-owner-idea-2026-11-22-in-progress-agent-2))
- in progress (undead-2): drowned sleepers in shipwrecks ([§](undead.md#drowned-sleepers-in-shipwrecks-owner-2026-11-22-in-progress-agent-2))
- designed: hybrid sleeper bodies and real 16x/32x body art, before release ([§](undead.md#dormant-undead-are-real-blocks))
- parked: mob spawner tweaks, the bury-break system, more sleeper kinds ([§](undead.md#scope-and-variants))

**Graveyards** ([spec](graveyards.md))
- built: gravestone block ([§](graveyards.md#gravestones-a-new-block-owner-approved-2026-11-22))
- built: crypt yards, 4 sizes, 5 states of decay ([§](graveyards.md#crypt-yards-surface))
- built: the resting dead ([§](graveyards.md#the-resting-dead-owner-2026-11-22))
- built: pauper, family and noble crypts ([§](graveyards.md#crypts-underground))
- built: noble crypt burial goods ([§](graveyards.md#noble-crypt-loot-built-agents-call-owner-2026-11-22-do-whatever-you-like-with-the-noble-crypt-loot))
- built: desert and snowy yards ([§](graveyards.md#desert-and-snowy-yards-built-2026-11-22))
- in progress (undead-2): will-o'-wisps ([§](graveyards.md#will-o-wisps-owner-2026-11-22-in-progress-agent-2))
- in progress (undead-2): noble dead in gemmed armor ([§](graveyards.md#noble-dead-in-gemmed-armor-owner-2026-11-22-in-progress-agent-2))
- designed: standalone hillside crypts (owner to confirm) ([§](graveyards.md#crypts-underground))

**Monster camps** ([spec](monster-camps.md))
- built: camps with rosters, leaders, banner keys, daily reset ([§](monster-camps.md#as-built-2026-11-22))
- built: camp shade and underground room ([§](monster-camps.md#as-built-2026-11-22))
- built: a lit fire quiets a camp ([§](monster-camps.md#lighting-a-fire-quiets-a-camp-owner-2026-11-22))
- built: pillager outposts as camps ([§](monster-camps.md#pillager-outposts-become-camps-too-owner))
- designed: alarm bells and the outpost raid horn ([§](monster-camps.md#alarms-owner-2026-11-22-designed-not-built))
- in progress (undead-2, last): maps from cartographers to our places ([§](monster-camps.md#maps-owner-2026-11-22-in-progress-agent-2-after-its-other-undead-work))

**Spiders** ([spec](spiders.md))
- built: cave nests, dropping and latching spiders ([§](spiders.md#cave-nests))
- built: giant spiders ([§](spiders.md#giant-spiders-owner-2026-11-22))
- built: spun webs ([§](spiders.md#spun-webs-owner-2026-11-22-we-should-try-at-least-to-make-something-really-outstanding-looking-there))
- built: cocoons ([§](spiders.md#cocoons-owner-2026-11-22))
- built: walls and ceilings (Stormie's Spiders, required) ([§](spiders.md#walls-and-ceilings-owner-2026-11-22))

**Creepers** ([spec](creepers.md))
- built: watcher creepers, startle meter, peekaboo, one per player at night ([§](creepers.md#how-its-built))
- built: charged and spawner creepers stay vanilla ([§](creepers.md#charged-creepers))
- built: `show_meters` game rule for the creeper and drowned meters ([§](creepers.md#how-its-built))

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
- designed: endermen trading at cairns ([§](endermen.md#trading-at-cairns-owner-2026-11-22-designed-not-built))
- designed: sneak + middle click places the held item anywhere, with trading ([§](endermen.md#trading-at-cairns-owner-2026-11-22-designed-not-built))

**Ancient Fabricators** ([spec](mob-factory.md))
- built: a Fabricator beside every ancient city: look, teleporter, hidden spawner, Warden, debuffs, loot ([§](mob-factory.md#mechanics-as-built-2026-11-17))
- built: secrets and the lower city ([§](mob-factory.md#secrets-and-the-lower-city-owner-2026-11-16))
- parked: why challenge a Fabricator, office jukebox loop, totem of spawning ([§](mob-factory.md#parked))

## Bubba's Totems & Artifacts ([spec](totems-artifacts.md))
- built: Grave Talisman, glows near undead, in crypt chests ([§](totems-artifacts.md#grave-talisman))
- built: Totem of Warding, held and in a beacon ([§](totems-artifacts.md#totem-of-warding))
- built: Totem of Keeping ([§](totems-artifacts.md#totem-of-keeping-keepingkeeping-mixinplayerkeepingmixin))
- built: Totem of Gathering, held; in a beacon, into a chest below ([§](totems-artifacts.md#totem-of-gathering-gatheringgathering-totemofgatheringitem-compatgatheringbeaconpayload))
- built: Totem of Fortification ([§](totems-artifacts.md#totem-of-fortification-fortificationfortification-mixinserverexplosionmixin-compatfortificationbeaconpayload))
- built: Totem of Growth ([§](totems-artifacts.md#totem-of-growth-working-name-growthgrowth-totemofgrowthitem-compatgrowthbeaconpayload))
- built: Totem of Seeking ([§](totems-artifacts.md#totem-of-seeking-seekingtotemofseekingitem-seekingtypes-structureseeker-seekingbeacon-mixincompassitemmixin-compatseekingbeaconpayload))
- built: Totems of the Beginning and the End ([§](totems-artifacts.md#totems-of-the-beginning-and-the-end-built-2026-11-22))
- built: a statue for every totem, decorative ([§](totems-artifacts.md#totem-statue-block-b15-exception))
- built: Gathering beacon makes a floating ring of items and XP for a side hopper ([§](totems-artifacts.md#gathering-in-a-beacon-a-floating-ring-built-2026-11-22-replaced-into-a-chest-below))
- built: Growth plants dropped seeds ([§](totems-artifacts.md#growth-also-plants-seeds-built-2026-11-22-harvest-folded-in-owner))
- built: Totem of Herding ([§](totems-artifacts.md#totem-of-herding-herdingherding-built-2026-11-22))
- built: Totem of Clear Skies ([§](totems-artifacts.md#totem-of-clear-skies-skiesclearskies-built-2026-11-22))
- built: statues give their totem's effect in a tiny radius ([§](totems-artifacts.md#statues-give-their-totems-effect-built-2026-11-22))
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
- built: datapack with 11 recipes ([§](extra-recipes.md))
- built: companion mod bundling the datapack and 16x/32x packs ([§](extra-recipes-mod.md#bundled-packs))
- built: the Glider ([§](extra-recipes-mod.md#glider))
- built: Synthetic Diamond Ore ([§](extra-recipes-mod.md#synthetic-diamond-ore))
- parked: person-made Glider art and mod icon ([§](extra-recipes-mod.md#open))

## Bubba's World Tweaks ([spec](world-tweaks.md))
- built: lava bubbles over magma ([§](world-tweaks.md#as-built-2026-11-22-agent-3))
- built: lava geysers with black smoke ([§](world-tweaks.md#as-built-2026-11-22-agent-3))
- built: lift and speed from lava and sulfur geysers ([§](world-tweaks.md#as-built-2026-11-22-agent-3))

## Repo
- designed (low priority, owner): settings for every mod: game rules in a per-mod category, structure datapacks at world creation, optional Mod Menu button ([§](settings.md))
- built: test world with stations for most features; missing ones listed in [testworld.md](testworld.md)
- parked: compatibility tests before releases (`BACKLOG.md`, Repo)
