// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicMaps;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicNames;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicShred;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicShredRecipe;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicSites;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicTuning;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicVaultPiece;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicVaultPlacement;
import io.github.bubba8587.bubbas_totems_artifacts.relic.Relics;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModBlocks;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Relic maps and vaults (specs/totems-artifacts.md, "Relic maps"). Milestone; the core suite runs two. */
public class RelicMapsGameTest {
	private static final String CRYPT_FIELD = TestSupport.NS + ":crypt_field";
	private static final String A = "wondrous_crab";
	private static final String B = "egregious_zealot";

	private static ItemStack s(String name, int size, int count) {
		return RelicMaps.shred(name, size, count);
	}

	private static List<ItemStack> grid(int slots, ItemStack... items) {
		List<ItemStack> list = new ArrayList<>(List.of(items));
		while (list.size() < slots) {
			list.add(ItemStack.EMPTY);
		}
		return list;
	}

	private static RelicShred combine(int w, ItemStack... items) {
		return RelicShredRecipe.combine(CraftingInput.of(w, w, grid(w * w, items)));
	}

	/** Same name and size combine (2x2 and 3x3), counts adding up to the whole; anything else doesn't. */
	public void shredsCombineOnlySameNameAndSize(GameTestHelper helper) {
		RelicShred four = combine(2, s(A, 4, 1), s(A, 4, 1), s(A, 4, 1), s(A, 4, 1));
		helper.assertTrue(four != null && four.complete() && four.count() == 4, "four 1/4 in the 2x2 grid: " + four);
		RelicShred mixed = combine(2, s(A, 9, 2), s(A, 9, 2), s(A, 9, 5));
		helper.assertTrue(mixed != null && mixed.complete() && mixed.count() == 9, "2/9 + 2/9 + 5/9: " + mixed);
		ItemStack[] nine = new ItemStack[9];
		for (int i = 0; i < 9; i++) {
			nine[i] = s(A, 9, 1);
		}
		RelicShred table = combine(3, nine);
		helper.assertTrue(table != null && table.complete(), "nine 1/9 in a crafting table: " + table);
		RelicShred part = combine(3, s(A, 9, 1), s(A, 9, 3));
		helper.assertTrue(part != null && part.count() == 4 && !part.complete(), "1/9 + 3/9 makes 4/9: " + part);
		helper.assertTrue(combine(2, s(A, 4, 1), s(A, 9, 1)) == null, "a /4 and a /9 of the same name combined");
		helper.assertTrue(combine(2, s(A, 4, 1), s(B, 4, 1)) == null, "two names combined");
		helper.assertTrue(combine(2, s(A, 4, 3), s(A, 4, 2)) == null, "3/4 + 2/4 (more than the whole) combined");
		helper.assertTrue(combine(2, s(A, 4, 1)) == null, "a lone shred matched");
		helper.assertTrue(combine(2, s(A, 4, 1), new ItemStack(Items.PAPER)) == null, "a shred and paper matched");
		// The recipe is loaded and found for the inventory grid.
		ServerLevel level = helper.getLevel();
		CraftingInput input = CraftingInput.of(2, 2, grid(4, s(A, 4, 2), s(A, 4, 1)));
		var found = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
		helper.assertTrue(found.isPresent() && found.get().value() instanceof RelicShredRecipe, "the relic shred recipe isn't loaded");
		ItemStack out = found.get().value().assemble(input);
		RelicShred got = out.get(Relics.SHRED);
		helper.assertTrue(out.is(Relics.RELIC_MAP_SHRED) && got != null && got.count() == 3 && got.size() == 4, "assembled " + out);
		helper.succeed();
	}

	/** A crafter puts two shreds together. */
	public void crafterCombinesShreds(GameTestHelper helper) {
		BlockPos crafter = new BlockPos(5, 2, 5);
		helper.setBlock(crafter, Blocks.CRAFTER);
		CrafterBlockEntity entity = helper.getBlockEntity(crafter, CrafterBlockEntity.class);
		entity.setItem(0, s(B, 9, 2));
		entity.setItem(1, s(B, 9, 3));
		helper.setBlock(crafter.east(), Blocks.REDSTONE_BLOCK);
		helper.succeedWhen(() -> {
			List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(crafter)).inflate(4));
			helper.assertTrue(items.stream().anyMatch(e -> {
				RelicShred got = e.getItem().get(Relics.SHRED);
				return got != null && got.count() == 5 && got.size() == 9 && got.name().equals(B);
			}), "the crafter made no 5/9 shred");
		});
	}

	/** Sites: one per name and size, stable for a seed, different for another, in their bands and apart. */
	public void sitesAreFixedPerSeed(GameTestHelper helper) {
		List<RelicSites.Site> first = RelicSites.sites(1234L, RelicSites.Land.ANY);
		List<RelicSites.Site> other = RelicSites.sites(98765L, RelicSites.Land.ANY);
		List<RelicSites.Site> again = RelicSites.sites(1234L, RelicSites.Land.ANY);
		helper.assertTrue(first.equals(again), "the same seed gave different sites");
		helper.assertTrue(!first.equals(other), "two seeds gave the same sites");
		helper.assertTrue(first.size() == RelicNames.ALL.size() * 2, first.size() + " sites, expected " + RelicNames.ALL.size() * 2);
		Set<Long> chunks = new HashSet<>();
		int close = 0;
		for (RelicSites.Site site : first) {
			helper.assertTrue(chunks.add(site.chunk().pack()), "two sites share a chunk: " + site);
			double d = Math.sqrt((double) site.centre().getX() * site.centre().getX() + (double) site.centre().getZ() * site.centre().getZ());
			int min = site.size() == 9 ? RelicTuning.GRAND_MIN_DISTANCE : RelicTuning.SMALL_MIN_DISTANCE;
			int max = site.size() == 9 ? RelicTuning.GRAND_MAX_DISTANCE : RelicTuning.SMALL_MAX_DISTANCE;
			helper.assertTrue(d >= min - 16 && d <= max + 16, site + " is " + (int) d + " blocks out");
			helper.assertTrue(RelicSites.site(1234L, RelicSites.Land.ANY, site.name(), site.size()).equals(site), "lookup by name differs for " + site);
			for (RelicSites.Site o : first) {
				int dx = o.chunk().x() - site.chunk().x();
				int dz = o.chunk().z() - site.chunk().z();
				if (o != site && dx * dx + dz * dz < RelicTuning.SITE_SEPARATION_CHUNKS * RelicTuning.SITE_SEPARATION_CHUNKS) {
					close++;
				}
			}
		}
		helper.assertTrue(close == 0, close / 2 + " pairs of sites closer than " + RelicTuning.SITE_SEPARATION_CHUNKS + " chunks");
		// Sites keep off spots the land rule forbids (oceans in a real world).
		RelicSites.Land east = (x, z) -> x >= 0;
		List<RelicSites.Site> eastOnly = RelicSites.sites(1234L, east);
		helper.assertTrue(eastOnly.stream().allMatch(s -> s.centre().getX() >= 0), "a site sits where the land rule forbids");
		// The structure set is loaded with the placement, and it marks exactly the site chunks.
		ServerLevel level = helper.getLevel();
		StructureSet set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET)
				.getValueOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts.id("relic_vaults")));
		helper.assertTrue(set.placement() instanceof RelicVaultPlacement, "relic_vaults isn't placed by relic_sites");
		RelicSites.Site site = RelicSites.sites(level.getSeed(), RelicSites.Land.of(level)).getFirst();
		helper.assertTrue(RelicSites.at(level.getSeed(), RelicSites.Land.of(level), site.chunk().x(), site.chunk().z()) == site
				&& RelicSites.at(level.getSeed(), RelicSites.Land.of(level), site.chunk().x() + 1, site.chunk().z()) == null, "site lookup by chunk is off");
		helper.succeed();
	}

	/** A complete shred turns into a filled map with a red X on its name's vault, when carried. */
	public void completeMapPointsAtVault(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack whole = s(A, 9, 9);
		player.getInventory().setItem(3, whole);
		whole.getItem().inventoryTick(whole, level, player, null);
		ItemStack map = player.getInventory().getItem(3);
		helper.assertTrue(map.is(Items.FILLED_MAP), "the whole shred didn't turn into a map: " + map);
		RelicSites.Site site = RelicSites.site(level.getSeed(), RelicSites.Land.of(level), A, 9);
		var decorations = map.get(DataComponents.MAP_DECORATIONS);
		helper.assertTrue(decorations != null && decorations.decorations().values().stream()
				.anyMatch(d -> (int) d.x() == site.centre().getX() && (int) d.z() == site.centre().getZ()), "the map's X isn't on " + site);
		ItemStack other = RelicMaps.map(level, new RelicShred(A, 4, 4));
		var otherDecorations = other.get(DataComponents.MAP_DECORATIONS);
		helper.assertTrue(otherDecorations.decorations().values().stream().noneMatch(d -> (int) d.x() == site.centre().getX()),
				"the /4 and /9 maps of one name lead to the same place");
		// A creative-tab shred has no relic until it reaches an inventory, then picks one.
		ItemStack blank = RelicMaps.shred("", 4, 1);
		player.getInventory().setItem(5, blank);
		blank.getItem().inventoryTick(blank, level, player, null);
		RelicShred rolled = blank.get(Relics.SHRED);
		helper.assertTrue(rolled != null && RelicNames.ALL.contains(rolled.name()) && rolled.size() == 4 && rolled.count() == 1,
				"the blank shred didn't pick a relic: " + rolled);
		helper.succeed();
	}

	/** Each vault kind: sealed room under the door's level, an iron door, a stair that stops partway, its chests and guards. */
	public void vaultBuildsSealedWithPartwayStair(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos door = helper.absolutePos(new BlockPos(16, 1, 14));
		for (RelicVaultPiece.Kind kind : RelicVaultPiece.Kind.values()) {
			RelicVaultPiece piece = new RelicVaultPiece(kind, kind == RelicVaultPiece.Kind.CRYPT ? "" : A, door, 42L);
			piece.buildNow(level);
			helper.assertTrue(level.getBlockState(door).is(Blocks.IRON_DOOR) && level.getBlockState(door.above()).is(Blocks.IRON_DOOR), kind + ": no iron door");
			BlockPos top = piece.stairTop();
			helper.assertTrue(top.getY() - door.getY() == RelicTuning.STAIR_STEPS, kind + ": the stair climbs " + (top.getY() - door.getY()));
			helper.assertTrue(level.getBlockState(top).isAir() && !level.getBlockState(top.above(3)).isAir(), kind + ": the stair top isn't a roofed landing");
			if (kind != RelicVaultPiece.Kind.CRYPT) {
				helper.assertTrue(!level.getBlockState(top.north()).isAir() && !level.getBlockState(top.north().above()).isAir(), kind + ": the stair goes on past its top");
			}
			Set<BlockPos> room = flood(level, piece.roomCentre(), piece.getBoundingBox());
			helper.assertTrue(room != null, kind + ": the room isn't sealed");
			helper.assertTrue(!room.contains(top), kind + ": the room is open to the stair");
			for (BlockPos chest : piece.chestSpots()) {
				helper.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity, kind + ": no chest at " + chest);
			}
			AABB box = AABB.of(piece.getBoundingBox());
			int stands = level.getEntitiesOfClass(ArmorStand.class, box).size();
			int cushions = level.getEntitiesOfClass(Cushion.class, box).size();
			boolean prime = primeShown(level, piece);
			helper.assertTrue(stands >= 1 && cushions >= 2 && prime, kind + ": " + stands + " stands, " + cushions + " cushions, prime shown " + prime);
			level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box.inflate(1), e -> !(e instanceof net.minecraft.world.entity.player.Player))
					.forEach(net.minecraft.world.entity.Entity::discard);
		}
		helper.assertTrue(RelicTuning.NAMED_DEPTH == 5 + RelicTuning.STAIR_STEPS, "a named vault's stair top isn't at crypt floor depth");
		helper.succeed();
	}

	/** The pedestal holds its prime: the statue (facing the door), or the weapon on a display or in a glow frame on the pedestal's front. */
	static boolean primeShown(ServerLevel level, RelicVaultPiece piece) {
		BlockPos top = piece.pedestal();
		BlockState state = level.getBlockState(top);
		if (piece.prime() == RelicVaultPiece.Prime.STATUE) {
			return state.is(ModBlocks.TOTEM_STATUE) && TotemStatueBlock.type(state) == piece.primeStatue()
					&& state.getValue(TotemStatueBlock.FACING) == net.minecraft.core.Direction.NORTH;
		}
		return primeWeapon(level, piece).isEnchanted();
	}

	/** The weapon on display on the pedestal (empty when none). */
	static ItemStack primeWeapon(ServerLevel level, RelicVaultPiece piece) {
		BlockPos top = piece.pedestal();
		if (TestSupport.withMobs()) {
			return Mobs.stuckDisplay(level, top);
		}
		return level.getEntitiesOfClass(GlowItemFrame.class, new AABB(top.below().north()), f -> f.getDirection() == net.minecraft.core.Direction.NORTH).stream()
				.map(GlowItemFrame::getItem).findFirst().orElse(ItemStack.EMPTY);
	}

	/** Reworked Mobs' classes, named only here (loaded only with -PwithMobs). */
	private static final class Mobs {
		static ItemStack stuckDisplay(ServerLevel level, BlockPos pos) {
			return level.getBlockEntity(pos) instanceof io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlockEntity display
					&& level.getBlockState(pos).getValue(io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlock.PLACEMENT)
							== io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement.STUCK ? display.getItem() : ItemStack.EMPTY;
		}
	}

	/**
	 * The pedestal holds a totem statue about half the time, else a fine weapon (netherite in grand vaults, diamond
	 * otherwise); the Beginning and the End only stand in grand vaults; breaking a statue gives its totem.
	 */
	public void pedestalHoldsStatueOrWeapon(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos door = helper.absolutePos(new BlockPos(16, 1, 14));
		for (RelicVaultPiece.Kind kind : RelicVaultPiece.Kind.values()) {
			int statues = 0;
			long statueSeed = -1;
			long weaponSeed = -1;
			for (long seed = 0; seed < 400; seed++) {
				RelicVaultPiece piece = new RelicVaultPiece(kind, A, door, seed);
				if (piece.prime() == RelicVaultPiece.Prime.STATUE) {
					statues++;
					statueSeed = statueSeed < 0 ? seed : statueSeed;
					TotemType type = piece.primeStatue();
					helper.assertTrue(kind == RelicVaultPiece.Kind.GRAND || type != TotemType.BEGINNING && type != TotemType.END,
							kind + ": a " + type + " statue outside a grand vault");
				} else {
					weaponSeed = weaponSeed < 0 ? seed : weaponSeed;
				}
			}
			helper.assertTrue(statues > 150 && statues < 250, kind + ": " + statues + " statues in 400 vaults");
			for (long seed : new long[] { statueSeed, weaponSeed }) {
				RelicVaultPiece piece = new RelicVaultPiece(kind, A, door, seed);
				if (piece.prime() == RelicVaultPiece.Prime.WEAPON) {
					buildSplitAt(level, piece, piece.pedestal().getZ());
				} else {
					piece.buildNow(level);
				}
				helper.assertTrue(primeShown(level, piece), kind + " seed " + seed + ": the " + piece.prime() + " isn't on the pedestal");
				if (piece.prime() == RelicVaultPiece.Prime.STATUE) {
					BlockPos top = piece.pedestal();
					List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(top), level, top, null);
					helper.assertTrue(drops.stream().anyMatch(d -> d.is(piece.primeStatue().item())), kind + ": breaking the statue gives " + drops);
				} else {
					ItemStack weapon = primeWeapon(level, piece);
					boolean netherite = weapon.is(Items.NETHERITE_SWORD) || weapon.is(Items.NETHERITE_AXE);
					boolean diamond = weapon.is(Items.DIAMOND_SWORD) || weapon.is(Items.DIAMOND_AXE);
					helper.assertTrue(kind == RelicVaultPiece.Kind.GRAND ? netherite : diamond, kind + ": the prime weapon is " + weapon);
				}
				AABB box = AABB.of(piece.getBoundingBox());
				level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box.inflate(1), e -> !(e instanceof net.minecraft.world.entity.player.Player))
						.forEach(net.minecraft.world.entity.Entity::discard);
			}
		}
		helper.succeed();
	}

	/** Builds {@code piece} as two chunks meeting at {@code z} (the pedestal in one, the block in front of it in the other). */
	static void buildSplitAt(ServerLevel level, RelicVaultPiece piece, int z) {
		BoundingBox all = piece.getBoundingBox();
		BoundingBox[] halves = { new BoundingBox(all.minX(), level.getMinY(), all.minZ(), all.maxX(), level.getMaxY(), z - 1),
				new BoundingBox(all.minX(), level.getMinY(), z, all.maxX(), level.getMaxY(), all.maxZ()) };
		for (BoundingBox half : halves) {
			piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), half,
					net.minecraft.world.level.ChunkPos.containing(piece.door()), piece.door());
		}
	}

	/** Air reachable from {@code start} without leaving {@code box}; null when it reaches the box's edge (not sealed). */
	static Set<BlockPos> flood(ServerLevel level, BlockPos start, BoundingBox box) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			if (!seen.add(pos)) {
				continue;
			}
			if (!box.isInside(pos)) {
				return null;
			}
			for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
				BlockPos n = pos.relative(d);
				BlockState state = level.getBlockState(n);
				if (!seen.contains(n) && !state.isCollisionShapeFullBlock(level, n) && !state.is(Blocks.IRON_DOOR)) {
					queue.add(n);
				}
			}
			if (seen.size() > 20000) {
				return null;
			}
		}
		return seen;
	}

	/** Our tables at the stated odds: a vanilla dungeon's shred chance, and a grand vault's totems and shreds. */
	public void lootTablesHoldRelicsAtStatedOdds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
		ResourceKey<LootTable> dungeon = ResourceKey.create(Registries.LOOT_TABLE, net.minecraft.resources.Identifier.parse("minecraft:chests/simple_dungeon"));
		int rolls = 1000;
		int withShred = 0;
		LootTable table = level.getServer().reloadableRegistries().getLootTable(dungeon);
		for (int i = 0; i < rolls; i++) {
			if (table.getRandomItems(params, i).stream().anyMatch(st -> st.is(Relics.RELIC_MAP_SHRED))) {
				withShred++;
			}
		}
		float expected = RelicTuning.SHRED_LOOT.get(dungeon);
		helper.assertTrue(Math.abs(withShred / (float) rolls - expected) < 0.06F, "dungeon shreds: " + withShred + "/" + rolls + ", expected " + expected);
		LootTable grand = level.getServer().reloadableRegistries().getLootTable(RelicVaultPiece.Kind.GRAND.loot);
		Set<net.minecraft.world.item.Item> totems = Set.of(ModItems.TOTEM_OF_KEEPING, ModItems.TOTEM_OF_GATHERING, ModItems.TOTEM_OF_FORTIFICATION,
				ModItems.TOTEM_OF_GROWTH, ModItems.TOTEM_OF_SEEKING, ModItems.TOTEM_OF_HERDING, ModItems.TOTEM_OF_CLEAR_SKIES, ModItems.TOTEM_OF_WARDING,
				ModItems.GRAVE_TALISMAN, ModItems.TOTEM_OF_THE_BEGINNING, ModItems.TOTEM_OF_THE_END);
		int withTotem = 0;
		int shreds = 0;
		for (int i = 0; i < rolls; i++) {
			var items = grand.getRandomItems(params, i);
			if (items.stream().anyMatch(st -> totems.contains(st.getItem()))) {
				withTotem++;
			}
			shreds += (int) items.stream().filter(st -> st.is(Relics.RELIC_MAP_SHRED)).count();
		}
		helper.assertTrue(Math.abs(withTotem / (float) rolls - 0.45F) < 0.06F, "grand vault chests with a totem: " + withTotem + "/" + rolls);
		helper.assertTrue(shreds >= rolls * 2, "grand vault shreds: " + shreds + " in " + rolls + " chests (at least 2 each)");
		helper.succeed();
	}

	/** With Reworked Mobs: some noble crypts have a vault whose stair top is just behind the far wall (still standing). */
	@GameTest(structure = CRYPT_FIELD, maxTicks = 40)
	public void nobleCryptGetsVaultBehindItsWall(GameTestHelper helper) {
		if (!TestSupport.withMobs()) {
			helper.succeed();
			return;
		}
		Crypt.check(helper);
	}

	@GameTest(structure = CRYPT_FIELD, maxTicks = 20)
	public void pedestalHoldsStatueOrWeaponTest(GameTestHelper helper) {
		pedestalHoldsStatueOrWeapon(helper);
	}

	@GameTest(maxTicks = 40)
	public void crafterCombinesShredsTest(GameTestHelper helper) {
		crafterCombinesShreds(helper);
	}

	@GameTest
	public void sitesAreFixedPerSeedTest(GameTestHelper helper) {
		sitesAreFixedPerSeed(helper);
	}

	@GameTest
	public void completeMapPointsAtVaultTest(GameTestHelper helper) {
		completeMapPointsAtVault(helper);
	}

	@GameTest(maxTicks = 100)
	public void lootTablesHoldRelicsAtStatedOddsTest(GameTestHelper helper) {
		lootTablesHoldRelicsAtStatedOdds(helper);
	}

	/** Reworked Mobs' side; only loaded with that mod. */
	private static final class Crypt {
		private Crypt() {
		}

		static void check(GameTestHelper helper) {
			ServerLevel level = helper.getLevel();
			BlockPos gate = helper.absolutePos(new BlockPos(16, 7, 1));
			var noble = io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Wealth.NOBLE;
			long seed = 1;
			while (io.github.bubba8587.bubbas_reworked_mobs.api.Crypts.attachments(piece(level, gate, seed)).isEmpty()) {
				seed++;
				helper.assertTrue(seed < 200, "no noble crypt seed under 200 has a vault");
			}
			var crypt = io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand.build(level, gate, seed, 15, 13, true,
					io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.State.OVERGROWN, noble);
			var extra = io.github.bubba8587.bubbas_reworked_mobs.api.Crypts.attachments(crypt);
			helper.assertTrue(extra.size() == 1 && extra.getFirst() instanceof RelicVaultPiece, "the crypt's attachment: " + extra);
			RelicVaultPiece vault = (RelicVaultPiece) extra.getFirst();
			BlockPos wall = crypt.nobleFarWall();
			helper.assertTrue(vault.kind() == RelicVaultPiece.Kind.CRYPT, "a crypt vault of kind " + vault.kind());
			helper.assertTrue(vault.stairTop().equals(wall.south()), "the stair top " + vault.stairTop() + " isn't just behind the wall " + wall);
			helper.assertTrue(!level.getBlockState(wall).isAir() && !level.getBlockState(wall.above()).isAir(), "the crypt's far wall is open");
			helper.assertTrue(level.getBlockState(wall.south()).isAir() && level.getBlockState(wall.south().above()).isAir(), "no stair landing behind the wall");
			helper.assertTrue(vault.door().getY() < wall.getY() && level.getBlockState(vault.door()).is(Blocks.IRON_DOOR), "the vault isn't under the crypt");
			helper.assertTrue(flood(level, vault.roomCentre(), vault.getBoundingBox()) != null, "the crypt vault isn't sealed");
			int chance = 0;
			for (long s = 1; s <= 400; s++) {
				if (!io.github.bubba8587.bubbas_reworked_mobs.api.Crypts.attachments(piece(level, gate, s)).isEmpty()) {
					chance++;
				}
			}
			helper.assertTrue(Math.abs(chance / 400F - RelicTuning.NOBLE_CRYPT_VAULT_CHANCE) < 0.08F, chance + "/400 noble crypts have a vault");
			helper.succeed();
		}

		private static io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece piece(ServerLevel level, BlockPos gate, long seed) {
			return new io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece(seed, gate, 15, 13, true,
					io.github.bubba8587.bubbas_reworked_mobs.grave.YardTrees.forBiome(level.getBiome(gate)),
					io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.State.OVERGROWN,
					io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Wealth.NOBLE,
					io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Climate.TEMPERATE);
		}
	}
}
