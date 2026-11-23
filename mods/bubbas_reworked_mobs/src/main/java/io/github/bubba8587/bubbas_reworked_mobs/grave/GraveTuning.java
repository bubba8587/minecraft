// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** Numbers for graveyards and crypts (specs/graveyards.md). Builder's starting values, owner to playtest. */
public final class GraveTuning {
	/** Yard size range (blocks, odd so the gate and path sit in the middle). */
	public static final int YARD_MIN_WIDTH = 13;
	public static final int YARD_MAX_WIDTH = 19;
	public static final int YARD_MIN_DEPTH = 13;
	public static final int YARD_MAX_DEPTH = 19;
	/** The ground's corners may differ by at most this much (blocks) for a yard to go there. */
	public static final int YARD_MAX_SLOPE = 3;
	/** Share of yards with stairs down to a crypt. */
	public static final float CRYPT_CHANCE = 0.6F;
	/** Depth of the crypt's floor below the yard's ground. */
	public static final int CRYPT_DEPTH = 6;
	/** Gravestones carved with nothing at all (the rest carry a few glyphs; never real names). */
	public static final float BLANK_STONE_CHANCE = 0.4F;
	/** Graves with something buried in the plot, showing (loot on display). */
	public static final float BURIED_LOOT_CHANCE = 0.12F;
	/** Graves with a sleeper in them. */
	public static final float GRAVE_SLEEPER_CHANCE = 0.75F;
	/** Sleepers in a yard's shallow graves that are skeletons (the rest zombies). */
	public static final float SKELETON_SHARE = 0.5F;

	public static final ResourceKey<LootTable> CRYPT_FAMILY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/crypt_family"));

	private GraveTuning() {
	}
}
