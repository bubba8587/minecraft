// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import io.github.bubba8587.bubbas_world_tweaks.fortress.ConduitShrine;
import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortressStructure;
import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortresses;
import io.github.bubba8587.bubbas_world_tweaks.fortress.FortressBlocks;
import io.github.bubba8587.bubbas_world_tweaks.fortress.TearPiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
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
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(throne, 3, 5, 8)).is(Blocks.SEA_LANTERN), "no sea lantern in the spawner room");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(throne, 1, 0, 0)).is(BlockTags.STONE_BRICKS), "walls aren't stone bricks");
		BlockState wall = level.getBlockState(DeepseaTestPieces.local(throne, 1, 6, 3));
		helper.assertTrue(wall.is(Blocks.PRISMARINE_WALL) && wall.getValue(BlockStateProperties.WATERLOGGED), "fence isn't a waterlogged prismarine wall");
		helper.assertTrue(spawned(level, DeepseaTestPieces.local(throne, 3, 5, 5)) == EntityTypes.GUARDIAN, "spawner isn't a guardian spawner");

		BlockPos barrelAt = DeepseaTestPieces.local(turn, 3, 2, 3);
		helper.assertTrue(level.getBlockEntity(barrelAt) instanceof BarrelBlockEntity barrel
				&& DeepseaFortresses.BARREL_LOOT.equals(barrel.getLootTable()), "no deep-sea loot barrel in the corridor");
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(turn, 2, 1, 2)).is(Blocks.DARK_PRISMARINE), "no corridor runner");
		for (int[] w : new int[][] { { 4, 3, 1 }, { 4, 4, 1 }, { 4, 3, 3 }, { 4, 4, 3 }, { 1, 3, 4 }, { 1, 4, 4 }, { 3, 3, 4 }, { 3, 4, 4 } }) {
			BlockState window = level.getBlockState(DeepseaTestPieces.local(turn, w[0], w[1], w[2]));
			helper.assertTrue(window.is(Blocks.WATER) || window.is(Blocks.STAINED_GLASS_PANE.cyan()) && window.getValue(BlockStateProperties.WATERLOGGED),
					"corridor window isn't a waterlogged cyan pane or open water: " + window);
		}
		helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(turn, 2, 6, 2)).is(Blocks.SEA_LANTERN) == FortressBlocks.lit(turn),
				"corridor ceiling lantern doesn't match the piece's lit choice");
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

	/** An erosion tear through a spawner room washes out stonework along its path but leaves the spawner and the ground. */
	@GameTest(structure = DeepseaTestPieces.ROOM, maxTicks = 100)
	public void tearErodesStonework(GameTestHelper helper) {
		NetherFortressPieces.MonsterThrone throne = DeepseaTestPieces.throne(helper);
		DeepseaTestPieces.place(helper, throne, true);
		ServerLevel level = helper.getLevel();
		BoundingBox b = throne.getBoundingBox();
		BlockPos spawner = DeepseaTestPieces.local(throne, 3, 5, 5);
		BlockPos ground = new BlockPos(b.minX(), b.minY() - 1, b.minZ());
		BlockState groundBefore = level.getBlockState(ground);
		BlockPos from = new BlockPos(b.minX() - 4, b.maxY() + 2, b.minZ() + 1);
		BlockPos to = new BlockPos(b.maxX() + 4, b.minY() + 2, b.minZ() + 1);
		TearPiece tear = new TearPiece(from, to, 2);
		int stoneBefore = stonework(level, b);
		tear.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(1L), tear.getBoundingBox(),
				new net.minecraft.world.level.ChunkPos(b.minX() >> 4, b.minZ() >> 4), from);
		helper.assertTrue(stonework(level, b) < stoneBefore, "the tear wore nothing away");
		helper.assertTrue(level.getBlockState(spawner).is(Blocks.SPAWNER), "the tear took the spawner");
		helper.assertTrue(level.getBlockState(ground).equals(groundBefore), "the tear cut the ground");
		helper.succeed();
	}

	private static int stonework(ServerLevel level, BoundingBox b) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
			n += level.getBlockState(p).is(BlockTags.STONE_BRICKS) ? 1 : 0;
		}
		return n;
	}

	/** On the shallowest allowed floor every layout fits under the top gap, sinking into the floor as far as it needs. */
	@GameTest(structure = DeepseaTestPieces.ROOM)
	public void tallFortressesSinkToFit(GameTestHelper helper) {
		int sea = 63;
		int floor = sea - DeepseaFortressStructure.MIN_DEPTH;
		int sank = 0;
		for (long seed = 1; seed <= 40; seed++) {
			StructurePiecesBuilder builder = new StructurePiecesBuilder();
			int sink = DeepseaFortressStructure.layOut(builder, RandomSource.create(seed), 0, 0, floor, sea);
			helper.assertTrue(builder.getBoundingBox().maxY() <= sea - DeepseaFortressStructure.TOP_GAP, "top too near the surface, seed " + seed);
			sank += sink > 0 ? 1 : 0;
		}
		helper.assertTrue(sank > 0, "no layout needed to sink: the test floor is too deep to cover sinking");
		helper.succeed();
	}

	/** A whole fortress over a hand-built sea: nothing Nether left, a guardian spawner, below sea level, no air in, beside or under it, pillars down to the floor. */
	@GameTest(structure = DeepseaTestPieces.ROOM, maxTicks = 400)
	public void wholeFortressIsASeaFortress(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int floor = 20;
		int stone = 10;
		int sea = 63;
		int found = 0;
		for (long seed = 1; seed <= 3 && found == 0; seed++) {
			StructureStart start = DeepseaTestPieces.fortress(level, seed, 30000 + (int) seed * 512, 30000, floor, sea, stone);
			helper.assertTrue(start.getBoundingBox().maxY() <= sea - DeepseaFortressStructure.TOP_GAP, "fortress top too near the surface: " + start.getBoundingBox());
			BoundingBox cb = start.getPieces().getFirst().getBoundingBox();
			helper.assertTrue(cb.minY() == floor + 1, "start crossing not seated on the floor");
			int[][] armEnds = { { cb.minX() + 9, cb.minZ() + 1 }, { cb.minX() + 9, cb.maxZ() - 1 }, { cb.minX() + 1, cb.minZ() + 9 },
					{ cb.maxX() - 1, cb.minZ() + 9 } };
			for (int[] c : armEnds) {
				for (int y = cb.minY() - 1; y > stone; y--) {
					BlockState s = level.getBlockState(new BlockPos(c[0], y, c[1]));
					helper.assertTrue(s.is(BlockTags.STONE_BRICKS) || s.is(Blocks.PRISMARINE_BRICKS),
							"pillar stops above the floor at " + c[0] + " " + y + " " + c[1] + ": " + s);
				}
			}
			BoundingBox all = start.getBoundingBox();
			int sand = 0;
			for (BlockPos p : BlockPos.betweenClosed(all.minX(), stone + 1, all.minZ(), all.maxX(), stone + 1, all.maxZ())) {
				sand += level.getBlockState(p).is(Blocks.SAND) ? 1 : 0;
			}
			helper.assertTrue(sand > 0, "no sand piled round the pillar feet");
			for (StructurePiece piece : start.getPieces()) {
				noNether(helper, piece);
				for (BlockPos p : DeepseaTestPieces.seaUnder(piece, stone)) {
					helper.assertTrue(!level.getBlockState(p).isAir(), "air at " + p + " in, beside or under " + piece.getClass().getSimpleName());
				}
				if (piece instanceof NetherFortressPieces.MonsterThrone) {
					BlockPos spawner = BlockPos.ZERO;
					BoundingBox b = piece.getBoundingBox();
					for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
						if (level.getBlockState(p).is(Blocks.SPAWNER)) {
							spawner = p.immutable();
						}
					}
					helper.assertTrue(spawned(level, spawner) == EntityTypes.GUARDIAN, "fortress spawner isn't a guardian spawner");
					found++;
				}
			}
		}
		helper.assertTrue(found > 0, "no spawner room in three fortresses");
		helper.succeed();
	}

	/** The stairs room's lifts form: an up column over soul sand, a down column over magma, both up through the landing. */
	@GameTest(structure = DeepseaTestPieces.ROOM, maxTicks = 100)
	public void stairsRoomLiftsForm(GameTestHelper helper) {
		NetherFortressPieces.StairsRoom room = new NetherFortressPieces.StairsRoom(1, DeepseaTestPieces.box(helper, 7, 11, 7), Direction.SOUTH);
		DeepseaTestPieces.place(helper, room, true);
		ServerLevel level = helper.getLevel();
		helper.succeedWhen(() -> {
			for (int y = 2; y <= 9; y++) {
				BlockState up = level.getBlockState(DeepseaTestPieces.local(room, 2, y, 2));
				BlockState down = level.getBlockState(DeepseaTestPieces.local(room, 4, y, 2));
				helper.assertTrue(up.is(Blocks.BUBBLE_COLUMN) && !up.getValue(BubbleColumnBlock.DRAG_DOWN), "no up column at y " + y + ": " + up);
				helper.assertTrue(down.is(Blocks.BUBBLE_COLUMN) && down.getValue(BubbleColumnBlock.DRAG_DOWN), "no down column at y " + y + ": " + down);
			}
			helper.assertTrue(level.getBlockState(DeepseaTestPieces.local(room, 2, 10, 2)).is(Blocks.SEA_LANTERN), "no lantern over the lift");
		});
	}

	/** A conduit placed in the shrine over the central crossing activates. */
	@GameTest(structure = DeepseaTestPieces.YARD, maxTicks = 200)
	public void conduitShrineActivates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		NetherFortressPieces.StartPiece crossing = new NetherFortressPieces.StartPiece(RandomSource.create(3L), 0, 0);
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
		BoundingBox b = crossing.getBoundingBox();
		crossing.move(at.getX() - b.minX(), at.getY() - b.minY(), at.getZ() - b.minZ());
		DeepseaTestPieces.placeStart(level, crossing);
		BlockPos heart = ConduitShrine.heart(crossing.getBoundingBox());
		helper.assertTrue(level.getBlockState(heart).is(Blocks.WATER), "shrine heart isn't open water");
		level.setBlock(heart, Blocks.CONDUIT.defaultBlockState(), 3);
		helper.succeedWhen(() -> helper.assertTrue(level.getBlockEntity(heart) instanceof ConduitBlockEntity conduit && conduit.isActive(),
				"conduit in the shrine isn't active"));
	}

	/** Dolphins lead to deep-sea fortresses; the shipwreck fortress map table loads. */
	@GameTest(structure = DeepseaTestPieces.ROOM)
	public void dolphinsAndMaps(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(DeepseaFortresses.STRUCTURE).is(StructureTags.DOLPHIN_LOCATED),
				"deep-sea fortress isn't dolphin-located");
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(DeepseaFortresses.FORTRESS_MAP) != LootTable.EMPTY,
				"no fortress map loot table");
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
