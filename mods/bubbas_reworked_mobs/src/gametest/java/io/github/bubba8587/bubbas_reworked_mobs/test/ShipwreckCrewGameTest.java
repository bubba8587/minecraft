// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Drowned sleepers in shipwrecks. Milestone. */
public class ShipwreckCrewGameTest {
	/** A vanilla full shipwreck, built as worldgen builds it, carries a drowned crew below decks. */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 20)
	public void shipwrecksHaveADrownedCrew(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(12, 8, 6));
		var piece = new ShipwreckPieces.ShipwreckPiece(level.getStructureTemplateManager(), Identifier.withDefaultNamespace("shipwreck/rightsideup_full"), at,
				Rotation.NONE, false);
		BoundingBox box = piece.getBoundingBox();
		for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
			for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
				ChunkPos chunk = new ChunkPos(cx, cz);
				BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(),
						chunk.getMaxBlockZ());
				piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunkBox, chunk, at);
			}
		}
		int crew = 0;
		int wood = 0;
		int hull = 0;
		BoundingBox built = piece.getBoundingBox();
		for (BlockPos pos : BlockPos.betweenClosed(built.minX(), built.minY(), built.minZ(), built.maxX(), built.maxY(), built.maxZ())) {
			if (level.getBlockState(pos).is(ModBlocks.sleeper(UndeadVariant.DROWNED))) {
				crew++;
			}
			if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.PLANKS)) {
				wood++;
			}
			if (io.github.bubba8587.bubbas_reworked_mobs.undead.ShipwreckCrew.inHull(level, pos)) {
				hull++;
			}
		}
		helper.assertTrue(crew > 0, "a shipwreck should carry drowned sleepers below decks, box " + built + " wood " + wood + " hull " + hull);
		helper.succeed();
	}

	/** A drowned sleeper wakes as a drowned, underwater too. */
	@GameTest(maxTicks = 40)
	public void drownedSleeperWakesAsADrowned(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		helper.setBlock(at.below(), net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(at, ModBlocks.sleeper(UndeadVariant.DROWNED).defaultBlockState()
				.setValue(io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock.WATERLOGGED, true));
		helper.getBlockEntity(at, io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.class).scheduleWake(level(helper), null, null, 1);
		helper.succeedWhen(() -> {
			var mob = helper.findOneEntity(io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities.woken(UndeadVariant.DROWNED));
			helper.assertTrue(mob instanceof net.minecraft.world.entity.monster.zombie.Drowned, "it wakes as a drowned");
			mob.discard();
		});
	}

	private static ServerLevel level(GameTestHelper helper) {
		return helper.getLevel();
	}
}
