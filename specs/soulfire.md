<!-- dte:C1,C4,C6,B15,B16,B17 -->
# Soulfire, Soulguard and soul blaze materials (spec)

**Status: to build** (owner design, 2026-11-25; *(default)* marks the orchestrator's picks for calls the owner left open). Spans Reworked Mobs (most of it), Infused Gems (the Soulguard gem) and Totems & Artifacts (Keeping), with no compile dependency between them (data tags and registry lookups by id).

## Soul blaze materials (owner)

- **Soul Blaze Rod:** dropped by Soulfire Blazes instead of blaze rods (vanilla blaze odds; summoned ones too, owner). Furnace fuel for **24 items** (a blaze rod smelts 12).
- **Soul Blaze Powder:** 1 rod crafts 2 powder. **Brewing stand fuel for 40 brews** (blaze powder: 20).
- **Soul Fire Charge:** soul blaze powder + gunpowder + coal (or charcoal), like a fire charge. Thrown (use) or fired from a dispenser it flies like a Soulfire Blaze's charge (`captain/SoulfireCharge`); used on a block it lights fire like a fire charge. Either way the fire is **soul fire** on soul sand or soul soil and normal fire elsewhere (vanilla `BaseFireBlock.getState`). No changes to wards (owner).
- No soul end rods (owner).

## Soulfire burning (owner)

A second kind of burning, from soul fire.
- **Sources:** standing in soul fire, touching a **lit soul campfire** (new: soul campfires now set you burning with Soulfire), and Soulfire Blaze charges / Soul Fire Charges. Normal fire, lava, fire charges and fire aspect still give normal burning; touching normal fire while soul-burning turns it into normal burning.
- **Damage (owner):** fire with a soul component: the living take normal burn damage plus a soul bonus; **undead ignore the soul component** and take exactly normal fire damage.
- **Totems:** an entity that dies from Soulfire burning (the burn or the soul fire / soul campfire contact) is **not saved by a Totem of Undying**, and a **Totem of Keeping does not keep** its inventory.
  - Shared by id, no compile dependency: Reworked Mobs defines damage types for soul fire contact, soul campfire contact and the soul burn, and a damage type tag `bubbas_reworked_mobs:bypasses_totems` holding them; the vanilla totem check (a Reworked Mobs mixin) and Totems & Artifacts' Keeping both skip their save when the killing damage is in that tag and the victim lacks Soulguard.
- **Feel:** soul sounds: vanilla soul sounds (soul escape, soul sand/soil sounds, soul fire ambience) while soul-burning (throttled, not every tick); **blue flames** drawn on a soul-burning entity instead of the orange ones *(default)* (art: a soul version of vanilla's burning-entity flames, from vanilla's soul fire textures).
- Synced to clients so the blue flames show for others too.

## Soulguard (owner)

One effect, no levels: **full immunity to Soulfire burning and soul fire / soul campfire damage, and it restores totems against Soulfire deaths.** Normal fire and lava still hurt.
- **Mob effect** `bubbas_reworked_mobs:soulguard` (beneficial; an effect icon).
- **Enchantment** `bubbas_reworked_mobs:soulguard`: on any armour piece *(default)*, one level, a **treasure enchantment** (owner): not from the enchanting table or villagers; found as books in Nether chests (bastions, fortresses, Nether outposts' room chest; builder picks odds, rare). Wearing it gives the immunity.
  - The **Captain's Helm** carries Soulguard as its enchantment (replacing its hard-coded soul fire perk), so it shows in its tooltip.
- **Potions:** awkward potion + **Soul Blaze Rod** → Potion of Soulguard (3:00) *(default)*; redstone → 8:00; no level II; splash and lingering as vanilla; potion colour soul cyan. Tipped arrows follow vanilla.
- **Gem** (Infused Gems): a Soulguard gem, brewable from the Soulguard potion like other potion gems (loads only with Reworked Mobs, load conditions). **No levels:** any Soulguard gem worn (any strength, any piece) gives full immunity; its tooltip shows just "+ Soulguard". In a beacon it gives Soulguard in the area. Its Infusion/Enchant rows still show; strength does nothing for it (state this in the gem spec).

## Backlog (owner)

- **Eye of the Soul:** something with soul blaze powder + an ender pearl; not a locator; design to come.

## Milestone test objectives

- Soul blaze rods drop from Soulfire Blazes (natural and summoned); powder fuels 40 brews; a rod smelts 24 items.
- Soul Fire Charges: crafted; thrown and dispensed they place soul fire on soul sand, fire on netherrack.
- Standing on a lit soul campfire sets an entity Soulfire-burning; normal fire turns it to normal burning.
- Undead take normal fire damage from Soulfire burning; the living take more.
- A player killed by Soulfire burning holding a Totem of Undying dies; with Soulguard it doesn't burn at all; with a Totem of Keeping and Soulfire death the inventory drops.
- The Soulguard enchantment, potion and gem each make a player immune to soul fire; the Captain's Helm carries the enchantment.
- Soulguard books appear in Nether chests at the stated odds and never at the enchanting table.
- Client: a soul-burning mob and player with blue flames; the new items' icons; the potion; the Soulguard effect icon; at 16x and 32x.
