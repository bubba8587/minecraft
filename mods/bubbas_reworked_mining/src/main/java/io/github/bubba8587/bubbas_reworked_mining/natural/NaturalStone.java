// dte:C7,B15
package io.github.bubba8587.bubbas_reworked_mining.natural;

import java.util.Set;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Tells natural stone from placed stone, so rare finds can't be farmed by placing and mining (specs/reworked-mining.md
 * "Rare finds"). {@code BlockExtraPropertyMixin} adds the boolean {@link #PLACED} to the blocks below while they are
 * constructed (docs/knowledge/item-keyed-block-drops.md approach C), with {@code placed=false} as the default, so
 * worldgen, commands, structures and old saves all count as natural. {@code BlockItemMixin} marks every block placed
 * from an item, and {@code LavaFluidMixin} marks the stone lava makes when it flows onto water (a stone generator).
 * The find pools in {@code MiningLoot} only roll on {@code bubba_placed=false}.
 *
 * <p>Loaded during {@code Blocks}' static init: must not touch any registry.
 */
public final class NaturalStone {
	/** Named to be unlikely to clash with another mod's property on the same blocks (a clash crashes at startup). */
	public static final BooleanProperty PLACED = BooleanProperty.create("bubba_placed");

	/** Natural stone-type blocks that can give finds (builder's list). */
	public static final Set<String> STONE_IDS = Set.of("minecraft:stone", "minecraft:granite", "minecraft:diorite",
			"minecraft:andesite", "minecraft:tuff");
	public static final Set<String> DEEPSLATE_IDS = Set.of("minecraft:deepslate");

	private NaturalStone() {
	}

	public static boolean marks(String blockId) {
		return STONE_IDS.contains(blockId) || DEEPSLATE_IDS.contains(blockId);
	}

	/** The placed version of a state, or the state itself for blocks without the mark. */
	public static BlockState placed(BlockState state) {
		return state.hasProperty(PLACED) ? state.setValue(PLACED, true) : state;
	}

	public static boolean isPlaced(BlockState state) {
		return state.getValueOrElse(PLACED, false);
	}
}
