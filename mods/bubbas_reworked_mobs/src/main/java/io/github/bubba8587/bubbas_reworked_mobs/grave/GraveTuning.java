// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** Numbers for graveyards and crypts (specs/graveyards.md). Builder's starting values, owner to playtest. */
public final class GraveTuning {
	/** The smallest yard (a lone family plot is 7; sizes by kind are in YardKinds.Size). Older saved pieces without a size read this. */
	public static final int YARD_MIN_WIDTH = 13;
	public static final int YARD_MIN_DEPTH = 13;
	/** The ground's corners may differ by at most this much (blocks) for a yard to go there. */
	public static final int YARD_MAX_SLOPE = 3;
	/** Depth of the crypt's floor below the yard's ground. */
	public static final int CRYPT_DEPTH = 6;
	/** Gravestones carved with nothing at all (the rest carry a few glyphs; never real names). */
	public static final float BLANK_STONE_CHANCE = 0.4F;
	/** Graves with something buried in the plot, showing (loot on display). */
	public static final float BURIED_LOOT_CHANCE = 0.12F;
	/** Graves with a sleeper in them. */
	public static final float GRAVE_SLEEPER_CHANCE = 0.75F;
	/** The resting dead (yard graves; always skeletons, never wake) dressed in dyed leather. */
	public static final float RESTING_LEATHER_CHANCE = 0.7F;

	public static final ResourceKey<LootTable> CRYPT_FAMILY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/crypt_family"));
	public static final ResourceKey<LootTable> CRYPT_PAUPER_LOOT = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/crypt_pauper"));
	public static final ResourceKey<LootTable> CRYPT_NOBLE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/crypt_noble"));

	// --- will-o-wisps ------------------------------------------------------------------------------------
	/** Ticks between wisp checks. */
	public static final int WISP_TICKS = 20;
	/** Hint range (blocks to the yard's centre). */
	public static final double WISP_RANGE = 96.0;
	/** Hint chance per yard per check. */
	public static final float WISP_HINT_CHANCE = 0.12F;
	/** Hint distance from the player, nearest. */
	public static final double WISP_HINT_MIN = 6.0;
	/** Hint distance from the player, farthest. */
	public static final double WISP_HINT_MAX = 12.0;
	/** Night wisps range (blocks to the yard's centre). */
	public static final double WISP_NIGHT_RANGE = 48.0;
	/** Night wisp chance per yard per check. */
	public static final float WISP_NIGHT_CHANCE = 0.5F;
	/** Chunks round each player searched for yards. */
	public static final int WISP_DISCOVER_CHUNKS = 6;

	private GraveTuning() {
	}
}
