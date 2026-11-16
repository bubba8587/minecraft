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
 */
public final class FabricatorLoot {
	public static final ResourceKey<LootTable> OFFICE = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/fabricator_office"));

	private FabricatorLoot() {
	}
}
