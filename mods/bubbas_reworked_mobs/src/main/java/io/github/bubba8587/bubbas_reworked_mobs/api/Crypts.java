// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.api;

import java.util.List;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlock;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.jspecify.annotations.Nullable;

/**
 * The small public hook other mods use to add to Reworked Mobs' crypts, and to place this mod's sleepers and loot
 * displays in their own structures. Bubba's Totems & Artifacts uses it for its relic vaults, only when this mod is
 * installed. Nothing here depends on that mod.
 *
 * <p>Stable: keep these signatures; add, don't change.
 */
public final class Crypts {
	/** Something built beside a noble crypt, behind its far wall. */
	public interface NobleAttachment {
		/**
		 * The piece to add for the noble crypt whose far wall's middle (floor height, in the wall) is {@code farWall};
		 * the crypt runs north of it (toward -z). Null for none (a chance roll from {@code cryptSeed}).
		 */
		@Nullable StructurePiece attach(long cryptSeed, BlockPos farWall);
	}

	private static volatile @Nullable NobleAttachment noble;

	private Crypts() {
	}

	/** Sets the one noble-crypt attachment (last caller wins). */
	public static void setNobleAttachment(@Nullable NobleAttachment attachment) {
		noble = attachment;
	}

	/** The extra pieces for this crypt yard (none without an attachment or a noble crypt). */
	public static List<StructurePiece> attachments(CryptYardPiece piece) {
		NobleAttachment attachment = noble;
		BlockPos wall = piece.nobleFarWall();
		if (attachment == null || wall == null) {
			return List.of();
		}
		StructurePiece extra = attachment.attach(piece.seed(), wall);
		return extra == null ? List.of() : List.of(extra);
	}

	/** Puts {@code stack} on display lying on the floor at {@code pos} (worldgen or live). */
	public static boolean placeDisplay(LevelAccessor level, BlockPos pos, Direction facing, ItemStack stack) {
		level.setBlock(pos, ModBlocks.LOOT_DISPLAY.defaultBlockState().setValue(LootDisplayBlock.PLACEMENT, DisplayPlacement.GROUND)
				.setValue(LootDisplayBlock.FACING, facing), Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof LootDisplayBlockEntity display) {
			display.setItem(stack);
			return true;
		}
		return false;
	}

	/** A sitting skeleton sleeper at {@code pos} facing {@code facing}, wearing {@code gear} (worldgen or live). */
	public static boolean placeSittingSleeper(LevelAccessor level, BlockPos pos, Direction facing, Map<EquipmentSlot, ItemStack> gear) {
		level.setBlock(pos, ModBlocks.sleeper(UndeadVariant.SKELETON).defaultBlockState().setValue(SleeperBlock.POSE, SleeperPose.SITTING)
				.setValue(SleeperBlock.FACING, facing), Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof SleeperBlockEntity sleeper) {
			gear.forEach(sleeper::setGear);
			return true;
		}
		return false;
	}
}
