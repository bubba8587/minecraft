<!-- dte:C1,C4,C6,B15,B16,B17 -->
# Soulfire, Soulguard and soul blaze materials (spec)

**Status: built** (owner design, 2026-11-25; *(default)* marks the orchestrator's picks for calls the owner left open; *(builder)* the builder's). Spans Reworked Mobs (most of it, package `soulfire/`, numbers in `soulfire/SoulfireTuning`), Infused Gems (the Soulguard gem) and Totems & Artifacts (Keeping), with no compile dependency between them (data tags and registry lookups by id).

## Soul blaze materials (owner)

- **Soul Blaze Rod** (`soul_blaze_rod`): dropped by Soulfire Blazes instead of blaze rods, at vanilla blaze odds (0-1, killed by a player, Looting +0-1; `loot_table/entities/soulfire_blaze.json`), natural and summoned alike (a summoned one that burns out when its captain dies still drops nothing). Furnace fuel for **24 items**: the vanilla `cooking_fuel` component with its own burn time, `context_int_provider/cooking/time_soul_blaze_rod.json` (4800 ticks; a blaze rod 2400).
- **Soul Blaze Powder** (`soul_blaze_powder`): 1 rod crafts 2 powder. **Brewing stand fuel for 40 brews**: 26.3's `brewing_fuel` component, uses from `context_int_provider/brewing/uses_soul_blaze_powder.json` (40; blaze powder 20). No mixin: the stand reads the component.
- **Soul Fire Charge** (`soul_fire_charge`): soul blaze powder + gunpowder + coal or charcoal makes 3, like a fire charge. A `FireChargeItem` subclass: used on a block it lights fire (or lights a campfire/candle) as a fire charge does; used in the air it throws a Soulfire Charge (`captain/SoulfireCharge`) along the look direction, 0.5 s cooldown *(builder, a wind charge's)*; fired from a dispenser it flies the same way (vanilla's projectile dispense behaviour, registered for the item). The fire it places is vanilla `BaseFireBlock.getState`: **soul fire** on soul sand or soul soil, normal fire elsewhere. No changes to wards (owner). Soulfire charges (the blaze's too) now draw as the Soul Fire Charge item.
- No soul end rods (owner).

## Soulfire burning (owner)

A second kind of burning, from soul fire: "fire with a soul component" (owner, 2026-11-25).
- **State:** a per-entity attachment `bubbas_reworked_mobs:soul_burning` (saved; synced to everyone tracking the entity). It means "this burning is Soulfire" and only counts while the entity is on fire; it is cleared when the fire runs out, the entity is fire-immune, or it has Soulguard.
- **Sources:** standing in soul fire, touching a **lit soul campfire** (new: soul campfires now set you burning, with vanilla fire contact's ignite rules), and Soulfire Blaze charges / Soul Fire Charges (5 s, as before).
- **Back to normal:** every other ignition is normal burning (any call to vanilla's `Entity.igniteForTicks` outside a soul source: fire, lava, fire charges, Fire Aspect, flaming arrows, lightning), and touching a lit ordinary campfire turns it to normal burning too. In soul fire the soul ignite runs just before vanilla's shared fire ignite, so ordinary fire or lava touched in the same tick wins.
- **Damage:**
  - Burn (once a second, where vanilla's `on_fire` would hit): damage type `bubbas_reworked_mobs:soul_burn`, **normal burn damage plus a soul bonus against the living (x1.5, `SOUL_BONUS`; 1.5 a second)**; **undead ignore the soul component and take exactly normal fire damage (1 a second)** (owner, 2026-11-25). Fire-immune entities stay immune.
  - Contact: `bubbas_reworked_mobs:soul_fire` (soul fire block) and `bubbas_reworked_mobs:soul_campfire` (lit soul campfire) replace vanilla's `in_fire` / `campfire` there; the living take vanilla's soul block damage (2), undead the ordinary fire / campfire's (1) *(builder: the same "undead ignore the soul part" rule)*.
  - The three types are in `#minecraft:is_fire` and the other tags vanilla's fire types are in (bypasses shields, no knockback, armour stands, panic), so Fire Resistance, fire-immune mobs and Fire Protection treat them as fire *(builder, vanilla first)*.
- **Totems:** an entity that dies from Soulfire (any of the three types) is **not saved by a Totem of Undying** (`mixin/TotemBypassMixin` on `LivingEntity.checkTotemDeathProtection`), and a **Totem of Keeping does not keep** its inventory (Totems & Artifacts `keeping/SoulfireDeaths`, reading the killing damage at the start of the death drops), unless the victim has Soulguard. Shared by the damage type tag `bubbas_reworked_mobs:bypasses_totems`; Totems & Artifacts names it and the Soulguard effect and enchantment by id (absent without Reworked Mobs: nothing changes).
- **Feel:** soul sounds while soul-burning: a 1 in 2 chance every second of soul escape, soul sand step or soul soil hit *(builder)*. **Blue flames** on a soul-burning entity instead of the orange ones, for everyone (client: the soul flag goes onto the entity render state, and vanilla's flame feature renderer draws that entity with `soulfire/client/SoulFlames` sprites); a soul-burning player's first-person fire overlay uses them too *(builder)*. Art by the art agent (`docs/art-todo.md`).

## Soulguard (owner)

One effect, no levels: **full immunity to Soulfire burning and soul fire / soul campfire damage, and it restores totems against Soulfire deaths.** Normal fire and lava still hurt.
- "Has Soulguard" = the effect, or the enchantment on any equipped piece in its slots (`SoulBurning.hasSoulguard`). With it: a soul source doesn't set you alight (soul fire and soul campfires put your flames out, as the Captain's Helm did), all three Soulfire damage types are cancelled, and soul-burning stops at once if you gain it while burning.
- **Mob effect** `bubbas_reworked_mobs:soulguard` (beneficial, colour #01A7AC, vanilla soul fire cyan; icon by the art agent).
- **Enchantment** `bubbas_reworked_mobs:soulguard` (`data/.../enchantment/soulguard.json`): any armour piece (`#minecraft:enchantable/armor`, slots armor), one level, weight 1, anvil cost 4 *(builder)*. A **treasure enchantment**: in `#minecraft:treasure` only, so not in `#in_enchanting_table`, `#tradeable` or `#on_random_loot` (no table, villager or random-loot rolls). Found as books: a pool with **4 %** *(builder)* chance of one Soulguard book added to bastion treasure, other, bridge and hoglin stable chests, nether fortress chests, and the Nether outpost room chest (`SoulfireTuning.BOOK_CHANCE`, `BOOK_CHESTS`; Fabric loot table modify).
  - The **Captain's Helm** carries Soulguard I as its default enchantments component (an item default, so every helm has it and its tooltip shows it; it glints like any enchanted item). Its old hard-coded soul fire perk (`captain/CaptainsHelm`) is gone.
- **Potions** (`data/.../recipe/brewing/`, vanilla `minecraft:brewing` recipes): awkward + **Soul Blaze Rod** -> Potion of Soulguard (3:00); + redstone -> 8:00 (`long_soulguard`); no level II; gunpowder -> splash, dragon's breath -> lingering, from either duration; colour #01A7AC. Tipped arrows by vanilla's own recipe. Potion name key `bubbas_reworked_mobs.soulguard`.
- **Gem** (Infused Gems): a Soulguard gem, brewed from the Soulguard potions like other potion gems (36 recipes, generated by `tools/gem-balance.py` with a `fabric:all_mods_loaded` check on Reworked Mobs). The balance model marks it `requires_mod: bubbas_reworked_mobs`; without Reworked Mobs the mod drops the effect, so it is not brewable, not in loot and not in `/gems`. **No levels:** worn, it is an always-on effect like Glowing: any Soulguard gem in worn armour (any strength) keeps the real Soulguard effect on the wearer (hidden, refreshed every second), so all the rules above apply; a beacon holding one gives it the same way; on a weapon a hit gives the target Soulguard for a few seconds. Its tooltip shows "+ Soulguard"; the Infusion/Enchant rows still show; strength does nothing for it. Never another gem's bonus or drawback roll.

## Backlog (owner)

- **Eye of the Soul:** something with soul blaze powder + an ender pearl; not a locator; design to come.

## Tests

- Core (`ReworkedMobsCoreGameTest`): a pig on a lit soul campfire becomes Soulfire-burning.
- Milestone `SoulfireGameTest` (Reworked Mobs): soul fire then ordinary fire turns burning normal; burn damage (pig 1.5x normal, zombie = normal); Soulfire death skips the Totem of Undying, ordinary fire doesn't, Soulguard blocks soul fire damage; enchanted boots, the potion effect and the Captain's Helm each immune in soul fire / on a soul campfire (Soulguard still burns in ordinary fire); the helm carries Soulguard I, it is treasure and in no vanilla random pool, fits any armour; 1500 rolls of each book chest at 4 % +-40 %; potion brewing (all five steps, durations); rods from natural and summoned blazes at 35-65 %, never blaze rods; crafting; powder 40 brews and rod 4800 ticks in real blocks; charges thrown by a player and dispensed place soul fire on soul sand and fire on netherrack; a charge hit sets Soulfire burning, not with Soulguard; with `-PwithGems`: the Soulguard potion brews a Soulguard Infused Gem and a Base Power I gem on leather boots (`/gems kit`) gives the effect and immunity.
- Milestone `TotemsGameTest.keepingSkippedOnSoulfireDeath` (Totems & Artifacts; real with `-PwithMobs`): a Soulfire death isn't kept, with Soulguard it is.
- Milestone `SoulguardGemGameTest` (Infused Gems): without Reworked Mobs there is no Soulguard gem.
- Client part `soulfire` (`SoulfireClientGameTest`): soulfire-zombie (soul beside normal burning), soulfire-player (third person), soulfire-player-first-person (overlay), soulfire-icons and soulfire-effect (hotbar items, potions, arrow, helm, book; survival inventory effect list), the last two with the 32x pack too.

## Milestone test objectives

- Soul blaze rods drop from Soulfire Blazes (natural and summoned); powder fuels 40 brews; a rod smelts 24 items.
- Soul Fire Charges: crafted; thrown and dispensed they place soul fire on soul sand, fire on netherrack.
- Standing on a lit soul campfire sets an entity Soulfire-burning; normal fire turns it to normal burning.
- Undead take normal fire damage from Soulfire; the living take more (the soul bonus).
- A player killed by Soulfire burning holding a Totem of Undying dies; with Soulguard it doesn't burn at all; with a Totem of Keeping and Soulfire death the inventory drops. (The Totem of Undying test uses zombies: test players are invulnerable until their client loads.)
- The Soulguard enchantment, potion and gem each make a player immune to soul fire; the Captain's Helm carries the enchantment. (Tested on pigs, for the same reason.)
- Soulguard books appear in Nether chests at the stated odds and never at the enchanting table.
- Client: a soul-burning mob and player with blue flames; the new items' icons; the potion; the Soulguard effect icon; at 16x and 32x.

## Open

- No test yet for a Soulguard gem in a beacon, or for the soul sounds.
- Fire Resistance blocks Soulfire damage as it does any fire *(builder, vanilla first)*; Soulguard adds no-ignite and the totem rule. The owner may want the soul part to get through Fire Resistance.
