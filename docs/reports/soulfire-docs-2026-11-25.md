<!-- dte:C1,C4,C6 -->
# Soulfire build: docs report (2026-11-25)

For the docs merge. As-built spec: `specs/soulfire.md` (status built). Below: what players see, numbers, choices, and other specs to touch.

## Player-facing (for player-docs: Reworked Mobs page, Infused Gems page, Totems & Artifacts page)

- **Soul Blaze Rod:** Soulfire Blazes drop these instead of blaze rods (same odds as a blaze: 0-1 when killed by a player, Looting adds up to 1). Burns 24 items in a furnace (a blaze rod: 12).
- **Soul Blaze Powder:** 1 rod makes 2. In a brewing stand it fuels 40 brews (blaze powder: 20).
- **Soul Fire Charge:** gunpowder + soul blaze powder + coal or charcoal makes 3. Use it on a block to light a fire (or a campfire or candle), throw it by using it in the air, or fire it from a dispenser. Where it lands it lights soul fire on soul sand and soul soil, ordinary fire elsewhere. It hits harder than a fire charge (7.5) and sets what it hits burning with Soulfire.
- **Soulfire:** soul fire, lit soul campfires (new: they now set you on fire) and Soulfire charges set you burning with blue flames. It's fire with a soul part: the living take 1.5 damage a second instead of 1; undead take just the normal 1. Touching ordinary fire, lava or a lit ordinary campfire, or being set alight any ordinary way, turns it back to normal burning. You hear souls while it burns.
- **Totems:** dying to Soulfire (the burn, soul fire or a soul campfire) can't be stopped by a Totem of Undying, and a Totem of Keeping won't keep your things, unless you have Soulguard.
- **Soulguard:** full protection from Soulfire: soul fire and soul campfires don't set you alight or hurt you, Soulfire charges still hit but don't set you burning, and totems work again. Ordinary fire and lava still hurt. Three ways to get it:
  - **Enchantment** Soulguard (one level, any armour piece, a treasure enchantment: never from the enchanting table or villagers). Books turn up in bastion chests, nether fortress chests and Nether outpost room chests (about 1 chest in 25). The **Captain's Helm** comes with Soulguard (it used to have its own soul fire protection; now it's the enchantment, shown in its tooltip; the helm now has the enchanted shimmer).
  - **Potion of Soulguard:** awkward potion + soul blaze rod (3:00); + redstone 8:00; splash, lingering and tipped arrows as usual. No level II.
  - **Soulguard gem** (Infused Gems, only with Reworked Mobs installed): brewed from a Soulguard potion like other potion gems. Any Soulguard gem worn gives full Soulguard, however strong the gem; its tooltip just says "+ Soulguard". In a beacon it gives Soulguard to everyone in range.
- Not changed: Fire Resistance still blocks soul fire damage as before (it's fire).

## Numbers (all in `SoulfireTuning` unless noted)

- Soul bonus x1.5 on the burn against the living; undead x1 (owner). Undead contact damage: soul fire 1, soul campfire 1 (vanilla soul blocks 2, the living keep 2).
- Soul sounds: 1 in 2 chance each second.
- Throw cooldown 0.5 s. Charge: 7.5 damage, 5 s Soulfire (CaptainTuning, unchanged).
- Potion 3600 / 9600 ticks; colour #01A7AC (art agent's pick).
- Soulguard book: 4 % per chest, in chests/bastion_treasure, bastion_other, bastion_bridge, bastion_hoglin_stable, nether_bridge and bubbas_reworked_mobs:chests/nether_outpost_room.
- Enchantment: weight 1, anvil cost 4, min cost 15, max 50 (no effect outside anvils; it's treasure).
- Fuel (data): rod 4800 ticks, powder 40 uses.

## Builder's choices (owner may change)

- Undead contact damage follows the burn rule (normal fire's value), not just the burn.
- Soul damage types are in `#minecraft:is_fire`, so Fire Resistance, Fire Protection and fire-immune mobs treat them as fire (vanilla first).
- A soul-burning player's first-person fire overlay is blue too.
- The Captain's Helm glints (it is enchanted). `enchantment_glint_override: false` on the item would hide it if the owner prefers.
- Soulfire charges from Soulfire Blazes now look like the Soul Fire Charge item (they used to look like a fire charge).
- Soulguard gem: rarity "common" in the model (it is never anyone's bonus or drawback, so this only matters for loot gems).

## Other specs to update (docs merge)

- `specs/wither-captain.md`: Captain's Helm "Soul fire immunity" section is replaced by the Soulguard enchantment (default enchantments component; see `specs/soulfire.md`); `CaptainsHelm.java` is deleted; Soulfire Blaze drops are soul blaze rods; Soulfire charges set Soulfire burning and draw as the Soul Fire Charge item; the helm test now sits with the Soulguard tests too.
- `specs/effect-gems.md`: the Soulguard gem: effect from another mod (`requires_mod` in balance.json, `OTHER_MOD_EFFECTS` and `NO_LEVELS` in the model), always-on worn (like Glowing), onoff hit, strength does nothing for it, brewing recipes load-conditioned, never another gem's bonus/drawback, dropped by `Balance` when Reworked Mobs is missing; `ModEffects.vanilla` now resolves any non-own id. Fixed a stale test string ("Gem Power" -> "Base Power" in `EnchantingGameTest`).
- `specs/totems-artifacts.md` (Keeping): skips keeping on a death whose damage is in `#bubbas_reworked_mobs:bypasses_totems` unless the player has Soulguard (by id; `keeping/SoulfireDeaths`, `mixin/KeepingDeathSourceMixin`).
- `docs/art-todo.md`: rows are the art agent's (soul blaze rod, powder, charge, Soulguard effect icon, SoulFlames).

## Tests run (2026-11-25, worktree)

- Core: `:bubbas_reworked_mobs:build` (21 passed), `:bubbas_infused_gems:build` (19), `:bubbas_totems_artifacts:build` (10).
- Milestone: `SoulfireGameTest` 13/13 with `-PwithGems`; `CaptainGameTest` 10/10; Infused Gems full milestone 80/80; Totems Keeping tests 2/2 with `-PwithMobs`.
- Client part `soulfire` and `captain`: screenshots checked (blue flames on a zombie and a player, first-person overlay, icons, potions, effect list, 32x).
