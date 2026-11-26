// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortresses;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.tags.BlockTags;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Deep-sea fortresses: the block swap, barrels, the guardian spawner, loot, a whole fortress. */
public class DeepseaFortressGameTest {
	/** A spawner room and a chest corridor placed with the swap are a flooded sea fortress. */
	public static void swapBuildsASeaRoom(GameTestHelper helper) {
		NetherFortressPieces.MonsterThrone throne = DeepseaTestPieces.throne(helper);
		NetherFortressPieces.CastleSmallCorridorLeftTurnPiece turn = DeepseaTestPieces.chestTurn(helper);
		DeepseaTestPieces.place(helper, throne, true);
		DeepseaTestPieces.place(helper, turn, true);
		ServerLevel level = helper.getLevel();

		noNether(helper, throne);
		noNether(helper, turn);
		BoundingBox box = throne.getBoundingBox();
		for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY() + 2, box.minZ(), box.maxX(), box.maxY(), box.maxZ() - 1)) {
			helper.assertTrue(!level.getBlockState(p).isAir(), "air left in the spawner room at " + p);
		}
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(throne, 3, 1, 1)).is(Blocks.DARK_PRISMARINE), "spawner room floor isn't dark prismarine");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(throne, 0, 5, 8)).is(Blocks.SEA_LANTERN), "no sea lantern in the spawner room");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(throne, 1, 0, 0)).is(BlockTags.STONE_BRICKS), "walls aren't stone bricks");
		BlockState wall = level.getBlockState(DeepseaTestPieces.local(throne, 1, 6, 3));
		helper.assertTrue(wall.is(Blocks.PRISMARINE_WALL) && wall.getValue(BlockStateProperties.WATERLOGGED), "fence isn't a waterlogged prismarine wall");
		helper.assertTrue(spawned(level, DeepseaTestPieces.local(throne, 3, 5, 5)) == EntityTypes.GUARDIAN, "spawner isn't a guardian spawner");

		BlockPos barrelAt = DeepseaTestPieces.local(turn, 3, 2, 3);
		helper.assertTrue(level.getBlockEntity(barrelAt) instanceof BarrelBlockEntity barrel
				&& DeepseaFortresses.BARREL_LOOT.equals(barrel.getLootTable()), "no deep-sea loot barrel in the corridor");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(turn, 2, 1, 2)).is(Blocks.DARK_PRISMARINE), "no corridor runner");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(turn, 2, 6, 2)).is(Blocks.SEA_LANTERN), "no corridor ceiling lantern");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(turn, 2, 3, 2)).is(Blocks.WATER), "corridor isn't flooded");
		helper.succeed();
	}

	/** The same vanilla pieces placed outside a deep-sea fortress stay Nether fortress pieces. */
	@GameTest(structure = DeepseaTestPieces.ROOM)
	public void netherFortressPiecesUntouched(GameTestHelper helper) {
		NetherFortressPieces.MonsterThrone throne = DeepseaTestPieces.throne(helper);
		NetherFortressPieces.CastleSmallCorridorLeftTurnPiece turn = DeepseaTestPieces.chestTurn(helper);
		DeepseaTestPieces.place(helper, throne, false);
		DeepseaTestPieces.place(helper, turn, false);
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(throne, 1, 0, 0)).is(Blocks.NETHER_BRICKS), "Nether walls swapped");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(throne, 3, 3, 1)).isAir(), "Nether air swapped");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(throne, 1, 6, 3)).is(Blocks.NETHER_BRICK_FENCE), "Nether fence swapped");
		helper.assertTrue(spawned(level, DeepseaTestPieces.local(throne, 3, 5, 5)) == EntityTypes.BLAZE, "blaze spawner swapped");
		helper.assertTrue(level.getBlockEntity(DeepseaTestPieces.local(turn, 3, 2, 3)) instanceof ChestBlockEntity chest
				&& BuiltInLootTables.NETHER_BRIDGE.equals(chest.getLootTable()), "Nether chest swapped");
		helper.succeed();
	}

	/** The barrel loot table loads and fills a barrel. */
	@GameTest(structure = DeepseaTestPieces.ROOM)
	public void barrelLootLoads(GameTestHelper helper) {
		helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(DeepseaFortresses.BARREL_LOOT) != LootTable.EMPTY,
				"no loot table " + DeepseaFortresses.BARREL_LOOT);
		BlockPos at = new BlockPos(1, 1, 1);
		helper.setBlock(at, Blocks.BARREL);
		BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(at, BarrelBlockEntity.class);
		barrel.setLootTable(DeepseaFortresses.BARREL_LOOT, 42L);
		barrel.unpackLootTable(null);
		helper.assertTrue(!barrel.isEmpty(), "barrel loot rolled nothing");
		helper.succeed();
	}

	/** A whole fortress placed like worldgen: nothing Nether left, a guardian spawner, below sea level, on its floor. */
	@GameTest(structure = DeepseaTestPieces.ROOM, maxTicks = 400)
	public void wholeFortressIsASeaFortress(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int floor = 20;
		int sea = 63;
		int found = 0;
		for (long seed = 1; seed <= 6 && found == 0; seed++) {
			StructureStart start = DeepseaTestPieces.fortress(level, seed, 30000 + (int) seed * 512, 30000, floor, sea);
			helper.assertTrue(start.getBoundingBox().maxY() <= sea - 4, "fortress top above sea level: " + start.getBoundingBox());
			helper.assertTrue(start.getPieces().getFirst().getBoundingBox().minY() <= floor + 1, "start not on the floor");
			for (StructurePiece piece : start.getPieces()) {
				noNether(helper, piece);
				if (piece instanceof NetherFortressPieces.MonsterThrone) {
					BlockPos spawner = BlockPos.containing(0, 0, 0);
					for (BlockPos p : BlockPos.betweenClosed(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(), piece.getBoundingBox().minZ(),
							piece.getBoundingBox().maxX(), piece.getBoundingBox().maxY(), piece.getBoundingBox().maxZ())) {
						if (level.getBlockState(p).is(Blocks.SPAWNER)) {
							spawner = p.immutable();
						}
					}
					helper.assertTrue(spawned(level, spawner) == EntityTypes.GUARDIAN, "fortress spawner isn't a guardian spawner");
					found++;
				}
			}
		}
		helper.assertTrue(found > 0, "no spawner room in six fortresses");
		helper.succeed();
	}

	private static void noNether(GameTestHelper helper, StructurePiece piece) {
		BoundingBox b = piece.getBoundingBox();
		ServerLevel level = helper.getLevel();
		for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
			BlockState state = level.getBlockState(p);
			for (Block nether : DeepseaTestPieces.NETHER) {
				helper.assertTrue(!state.is(nether), "Nether block " + state + " at " + p + " in " + piece.getClass().getSimpleName());
			}
		}
	}

	private static EntityType<?> spawned(ServerLevel level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
			Entity shown = spawner.getSpawner().getOrCreateDisplayEntity(level, pos);
			return shown == null ? null : shown.getType();
		}
		return null;
	}
}
