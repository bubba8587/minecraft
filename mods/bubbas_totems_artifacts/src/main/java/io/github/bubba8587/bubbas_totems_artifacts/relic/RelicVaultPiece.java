// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.compat.SleeperHook;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * One relic vault, built in code from its own seed (specs/totems-artifacts.md, "Relic vaults"). Layout, x across, z
 * from the door south: a stair climbs north from the iron door {@link RelicTuning#STAIR_STEPS} steps and stops (a
 * named vault's ends at solid stone; a crypt's comes out just behind the noble crypt's far wall); south of the door, the
 * sealed room.
 */
public class RelicVaultPiece extends StructurePiece {
	/** What kind of vault: its room size, chests, guards and loot. */
	public enum Kind {
		/** A named /4 vault. */
		SMALL(4, 9, 3, 2, "relic_vault_small"),
		/** A named /9 vault. */
		GRAND(6, 11, 6, 4, "relic_vault_grand"),
		/** Behind a noble crypt, unnamed. */
		CRYPT(3, 7, 2, 1, "relic_vault_crypt");

		final int halfWidth;
		final int length;
		final int chests;
		final int stands;
		public final ResourceKey<LootTable> loot;

		Kind(int halfWidth, int length, int chests, int stands, String table) {
			this.halfWidth = halfWidth;
			this.length = length;
			this.chests = chests;
			this.stands = stands;
			this.loot = ResourceKey.create(Registries.LOOT_TABLE, BubbasTotemsArtifacts.id("chests/" + table));
		}
	}

	/** Inside height of the room (feet to ceiling). */
	private static final int HEIGHT = 4;

	private final Kind kind;
	/** The relic's name (empty for a crypt vault). */
	private final String name;
	/** The iron door's lower block; the room's floor is at its height. */
	private final BlockPos door;
	private final long seed;

	public RelicVaultPiece(Kind kind, String name, BlockPos door, long seed) {
		super(Relics.VAULT_PIECE, 0, box(kind, door));
		this.kind = kind;
		this.name = name;
		this.door = door;
		this.seed = seed;
	}

	public RelicVaultPiece(CompoundTag tag) {
		super(Relics.VAULT_PIECE, tag);
		this.kind = Kind.values()[Math.floorMod(tag.getIntOr("Kind", 0), Kind.values().length)];
		this.name = tag.getStringOr("Name", "");
		this.door = BlockPos.of(tag.getLongOr("Door", 0L));
		this.seed = tag.getLongOr("Seed", 0L);
	}

	/** The vault behind a noble crypt whose far wall's middle (floor height) is {@code farWall}: its stair's top is just south of it. */
	public static RelicVaultPiece behindCrypt(BlockPos farWall, long seed) {
		BlockPos door = new BlockPos(farWall.getX(), farWall.getY() - RelicTuning.STAIR_STEPS, farWall.getZ() + 3 + RelicTuning.STAIR_STEPS);
		return new RelicVaultPiece(Kind.CRYPT, "", door, seed);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("Kind", kind.ordinal());
		tag.putString("Name", name);
		tag.putLong("Door", door.asLong());
		tag.putLong("Seed", seed);
	}

	public Kind kind() {
		return kind;
	}

	public String name() {
		return name;
	}

	public BlockPos door() {
		return door;
	}

	/** The top of the stair: the last landing's feet position. */
	public BlockPos stairTop() {
		return new BlockPos(door.getX(), door.getY() + RelicTuning.STAIR_STEPS, topZ(door));
	}

	/** The pedestal's top (where the prime loot is shown). */
	public BlockPos pedestal() {
		return new BlockPos(door.getX(), door.getY() + 1, door.getZ() + kind.length - 1);
	}

	/** The middle of the room's floor (feet height). */
	public BlockPos roomCentre() {
		return new BlockPos(door.getX(), door.getY(), door.getZ() + (kind.length + 1) / 2);
	}

	private static int topZ(BlockPos door) {
		return door.getZ() - 2 - RelicTuning.STAIR_STEPS;
	}

	private static BoundingBox box(Kind kind, BlockPos door) {
		int hw = kind.halfWidth + 1;
		int minZ = topZ(door) - (kind == Kind.CRYPT ? 0 : 1);
		return new BoundingBox(door.getX() - hw, door.getY() - 2, minZ, door.getX() + hw, door.getY() + RelicTuning.STAIR_STEPS + 4,
				door.getZ() + kind.length + 1);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource chunkRandom, BoundingBox chunkBB,
			ChunkPos chunkPos, BlockPos referencePos) {
		Build b = new Build(level, chunkBB);
		RandomSource random = RandomSource.create(RandomSupport.mixStafford13(seed) ^ 0x7E1C5L);
		stair(b, random);
		room(b, random);
	}

	// --- the stair ------------------------------------------------------------------------------------

	private void stair(Build b, RandomSource random) {
		int dx = door.getX();
		int fy = door.getY();
		int top = topZ(door);
		for (int z = top; z <= door.getZ() - 1; z++) {
			int k = door.getZ() - 1 - z;
			int h = fy + Math.min(k, RelicTuning.STAIR_STEPS);
			boolean step = k >= 1 && k <= RelicTuning.STAIR_STEPS;
			for (int x = dx - 2; x <= dx + 2; x++) {
				if (Math.abs(x - dx) == 2) {
					for (int y = h - 2; y <= h + 3; y++) {
						b.put(x, y, z, oldBricks(random));
					}
					continue;
				}
				b.put(x, h - 2, z, oldBricks(random));
				b.put(x, h - 1, z, step ? Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH) : oldBricks(random));
				for (int y = h; y <= h + 2; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				b.put(x, h + 3, z, oldBricks(random));
			}
		}
		if (kind != Kind.CRYPT) {
			int h = fy + RelicTuning.STAIR_STEPS;
			for (int x = dx - 2; x <= dx + 2; x++) {
				for (int y = h - 2; y <= h + 3; y++) {
					b.put(x, y, top - 1, oldBricks(random));
				}
			}
		}
		// A soul lantern hung at the stair's foot, nearly out.
		b.put(dx - 1, fy + 2, door.getZ() - 1, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
	}

	private static BlockState oldBricks(RandomSource random) {
		float r = random.nextFloat();
		return r < 0.2F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : r < 0.35F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
				: Blocks.STONE_BRICKS.defaultBlockState();
	}

	// --- the room -------------------------------------------------------------------------------------

	private void room(Build b, RandomSource random) {
		int dx = door.getX();
		int fy = door.getY();
		int z0 = door.getZ();
		int hw = kind.halfWidth;
		int len = kind.length;
		// Shell, floor and ceiling.
		for (int z = z0; z <= z0 + len + 1; z++) {
			for (int x = dx - hw - 1; x <= dx + hw + 1; x++) {
				int i = x - dx;
				int j = z - z0;
				boolean wall = j == 0 || j == len + 1 || Math.abs(i) == hw + 1;
				boolean pillar = wall && (Math.abs(i) == hw + 1 && (j == 0 || j == len + 1 || j % 4 == 0) || (j == 0 || j == len + 1) && i % 3 == 0 && i != 0);
				b.put(x, fy - 1, z, wall ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : floor(i, j, hw, len));
				for (int y = fy; y < fy + HEIGHT; y++) {
					b.put(x, y, z, wall ? (pillar ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState())
							: Blocks.AIR.defaultBlockState());
				}
				boolean light = !wall && Math.floorMod(i, 3) == 0 && Math.floorMod(j, 3) == 2;
				b.put(x, fy + HEIGHT, z, light ? Blocks.SEA_LANTERN.defaultBlockState() : wall ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
						: Blocks.DEEPSLATE_TILES.defaultBlockState());
			}
		}
		// The iron door, and its lever inside (nothing opens it from the stair).
		b.put(dx, fy, z0, Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		b.put(dx, fy + 1, z0, Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		b.put(dx + 1, fy + 1, z0 + 1, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.SOUTH));
		// A runner from the door to the pedestal.
		int pedZ = z0 + len - 1;
		for (int z = z0 + 1; z < pedZ; z++) {
			b.put(dx, fy, z, Blocks.CARPET.red().defaultBlockState());
		}
		// The pedestal, lit from the floor either side.
		b.put(dx, fy, pedZ, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
		b.put(dx - 1, fy - 1, pedZ, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());
		b.put(dx + 1, fy - 1, pedZ, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());
		chests(b, random);
		furniture(b, random);
		// Entities and the prime loot only from the chunk holding them.
		prime(b, random);
		stands(b, random);
	}

	private static BlockState floor(int i, int j, int hw, int len) {
		if (Math.abs(i) == hw || j == 1 || j == len) {
			return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		}
		return Math.floorMod(i + j, 2) == 0 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.POLISHED_DIORITE.defaultBlockState();
	}

	/** The chests, stacked two high against the back wall from the corners in. */
	public List<BlockPos> chestSpots() {
		int dx = door.getX();
		int fy = door.getY();
		int back = door.getZ() + kind.length;
		int hw = kind.halfWidth;
		List<BlockPos> all = List.of(new BlockPos(dx - hw, fy, back), new BlockPos(dx - hw, fy + 1, back), new BlockPos(dx + hw, fy, back),
				new BlockPos(dx + hw, fy + 1, back), new BlockPos(dx - hw + 1, fy, back), new BlockPos(dx - hw + 1, fy + 1, back));
		return all.subList(0, kind.chests);
	}

	private void chests(Build b, RandomSource random) {
		for (BlockPos pos : chestSpots()) {
			b.put(pos.getX(), pos.getY(), pos.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
			if (b.inside(pos.getX(), pos.getY(), pos.getZ())) {
				RandomizableContainer.setBlockEntityLootTable(b.level, random, pos, kind.loot);
			}
		}
	}

	/** A table (an upper slab) between two stair chairs with cushions, a lantern on it; the grand vault has a second. */
	private void furniture(Build b, RandomSource random) {
		int mid = door.getZ() + (kind.length + 1) / 2;
		List<Integer> sides = kind == Kind.GRAND ? List.of(-1, 1) : List.of(-1);
		DyeColor[] colours = { DyeColor.RED, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.ORANGE };
		for (int side : sides) {
			int x = door.getX() + side * (kind.halfWidth - 1);
			int fy = door.getY();
			int tableZ = side < 0 ? mid : mid - 2;
			b.put(x, fy, tableZ, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
			b.put(x, fy + 1, tableZ, Blocks.LANTERN.defaultBlockState());
			DyeColor colour = colours[random.nextInt(colours.length)];
			for (int dz = -1; dz <= 1; dz += 2) {
				int z = tableZ + dz;
				b.put(x, fy, z, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dz < 0 ? Direction.NORTH : Direction.SOUTH));
				if (b.inside(x, fy, z)) {
					Cushion cushion = EntityTypes.CUSHION.create(b.level.getLevel(), EntitySpawnReason.STRUCTURE);
					if (cushion != null) {
						cushion.setColor(colour);
						cushion.snapTo(x + 0.5, fy + 0.5, z + 0.5, dz < 0 ? 0.0F : 180.0F, 0.0F);
						b.level.addFreshEntity(cushion);
					}
				}
			}
		}
	}

	/** What a vault's pedestal holds. */
	public enum Prime {
		/** A totem statue facing the door; breaking it gives the totem. */
		STATUE,
		/** A fine enchanted weapon on display. */
		WEAPON
	}

	/** What this vault's pedestal holds (fixed by its seed). */
	public Prime prime() {
		return primeRandom().nextFloat() < RelicTuning.PRIME_STATUE_CHANCE ? Prime.STATUE : Prime.WEAPON;
	}

	/** The statue's totem, when {@link #prime()} is a statue (fixed by its seed; the Beginning and the End only in grand vaults). */
	public TotemType primeStatue() {
		RandomSource random = primeRandom();
		random.nextFloat();
		List<TotemType> types = new ArrayList<>(List.of(TotemType.values()));
		if (kind != Kind.GRAND) {
			types.remove(TotemType.BEGINNING);
			types.remove(TotemType.END);
		}
		return types.get(random.nextInt(types.size()));
	}

	private RandomSource primeRandom() {
		return RandomSource.create(RandomSupport.mixStafford13(seed ^ 0x9E3D5A1L));
	}

	/** The prime loot on the pedestal, and (with Reworked Mobs) its sitting guard. */
	private void prime(Build b, RandomSource random) {
		BlockPos top = pedestal();
		if (prime() == Prime.STATUE) {
			b.put(top.getX(), top.getY(), top.getZ(), TotemStatueBlock.stateFor(primeStatue()).setValue(TotemStatueBlock.FACING, Direction.NORTH));
		} else {
			ItemStack weapon = RelicLoot.primeWeapon(b.level, kind, random, top);
			BlockPos front = top.below().north();
			boolean displayed = SleeperHook.get().hasDisplays()
					&& (!b.inside(top.getX(), top.getY(), top.getZ()) || SleeperHook.get().placeStuckDisplay(b.level, top, Direction.NORTH, weapon));
			if (!displayed && b.inside(front.getX(), front.getY(), front.getZ())) {
				GlowItemFrame frame = new GlowItemFrame(b.level.getLevel(), front, Direction.NORTH);
				frame.setItem(weapon, false);
				b.level.addFreshEntity(frame);
			}
		}
		BlockPos guard = top.below().east(2);
		if (kind != Kind.CRYPT && b.inside(guard.getX(), guard.getY(), guard.getZ())) {
			SleeperHook.get().placeSittingSleeper(b.level, guard, Direction.WEST, guardGear(b, random));
		}
	}

	private Map<EquipmentSlot, ItemStack> guardGear(Build b, RandomSource random) {
		Map<EquipmentSlot, ItemStack> gear = new EnumMap<>(EquipmentSlot.class);
		boolean grand = kind == Kind.GRAND;
		gear.put(EquipmentSlot.HEAD, new ItemStack(grand ? Items.DIAMOND_HELMET : Items.IRON_HELMET));
		gear.put(EquipmentSlot.CHEST, new ItemStack(grand ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE));
		gear.put(EquipmentSlot.LEGS, new ItemStack(grand ? Items.DIAMOND_LEGGINGS : Items.IRON_LEGGINGS));
		gear.put(EquipmentSlot.FEET, new ItemStack(grand ? Items.DIAMOND_BOOTS : Items.IRON_BOOTS));
		gear.put(EquipmentSlot.MAINHAND, new ItemStack(grand ? Items.DIAMOND_SWORD : Items.IRON_SWORD));
		RelicLoot.gemmedGear(b.level, random).forEach(gear::put);
		return gear;
	}

	/** Armour stands in gear: the front corners, and (grand) two along the east wall. */
	private void stands(Build b, RandomSource random) {
		int dx = door.getX();
		int fy = door.getY();
		int hw = kind.halfWidth;
		int mid = door.getZ() + (kind.length + 1) / 2;
		List<BlockPos> spots = new ArrayList<>(List.of(new BlockPos(dx - hw, fy, door.getZ() + 1), new BlockPos(dx + hw, fy, door.getZ() + 1)));
		List<Float> yaws = new ArrayList<>(List.of(-45.0F, 45.0F));
		if (kind == Kind.GRAND) {
			spots.add(new BlockPos(dx + hw, fy, mid + 1));
			spots.add(new BlockPos(dx + hw, fy, mid + 3));
			yaws.add(90.0F);
			yaws.add(90.0F);
		}
		Item[][] sets = {
				{ Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS, Items.IRON_SWORD },
				{ Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_AXE },
				{ Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_SWORD },
				{ Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.GOLDEN_SWORD } };
		for (int n = 0; n < kind.stands; n++) {
			BlockPos pos = spots.get(n);
			Item[] set = kind == Kind.CRYPT ? sets[0] : kind == Kind.SMALL ? sets[1 + random.nextInt(2) * 2] : sets[1 + random.nextInt(3)];
			if (!b.inside(pos.getX(), pos.getY(), pos.getZ())) {
				continue;
			}
			ArmorStand stand = EntityTypes.ARMOR_STAND.create(b.level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (stand == null) {
				continue;
			}
			stand.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaws.get(n), 0.0F);
			stand.setShowArms(true);
			stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(set[0]));
			stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(set[1]));
			stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(set[2]));
			stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(set[3]));
			stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(set[4]));
			b.level.addFreshEntity(stand);
		}
	}

	/** Builds this vault now in a live level, chunk by chunk as worldgen would (commands and tests). */
	public void buildNow(net.minecraft.server.level.ServerLevel level) {
		BoundingBox box = getBoundingBox();
		for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
			for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
				ChunkPos chunk = new ChunkPos(cx, cz);
				BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(),
						chunk.getMaxBlockZ());
				postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunkBox, chunk, door);
			}
		}
	}

	// --- placing, clipped to the chunk being generated -------------------------------------------------

	private static final class Build {
		final WorldGenLevel level;
		final BoundingBox chunk;

		Build(WorldGenLevel level, BoundingBox chunk) {
			this.level = level;
			this.chunk = chunk;
		}

		boolean inside(int x, int y, int z) {
			return chunk.isInside(x, y, z);
		}

		void put(int x, int y, int z, BlockState state) {
			if (inside(x, y, z)) {
				level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
			}
		}
	}
}
