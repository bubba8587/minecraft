// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Warden's office chests (specs/mob-factory.md): "the factory's paperwork". 3-6 rolls of paper, books, a book
 * and quill, echo shards, disc fragments, gray candles, iron, redstone and sculk; then one roll: nothing (12),
 * music disc 13 (2), music disc 11 (1) or a Swift Sneak book (3). Data: {@code loot_table/chests/fabricator_office.json}.
 *
 * <p>Spare parts (owner, 2026-11-16): with Bubba's Infused Gems installed the chests use
 * {@code fabricator_office_with_gems} instead: the same, plus a 1-in-3 chance of a harmful gem that would power the
 * debuff beacon (Mining Fatigue 3, Slowness 2, Weakness 2 at Gem Power III, or 1 the beacon's own perfect Mining
 * Fatigue gem). That table only loads with that mod (a Fabric load condition), so neither mod depends on the other.
 */
public final class FabricatorLoot {
	public static final ResourceKey<LootTable> OFFICE = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/fabricator_office"));
	public static final ResourceKey<LootTable> OFFICE_WITH_GEMS = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/fabricator_office_with_gems"));
	/** The reject vault under the deck (provisional: the owner is still deciding what a Fabricator rewards). */
	public static final ResourceKey<LootTable> REJECTS = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/fabricator_rejects"));
	/** The chests sunk in the lower city's floors (provisional, as above). */
	public static final ResourceKey<LootTable> BURIED = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/fabricator_buried"));

	private FabricatorLoot() {
	}

	/** The office chests' table: with spare gems when Infused Gems is installed. */
	public static ResourceKey<LootTable> office() {
		return FabricatorLink.gemsLoaded() ? OFFICE_WITH_GEMS : OFFICE;
	}
}
