// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.bubba8587.bubbas_totems_artifacts.fortification.Fortification;
import io.github.bubba8587.bubbas_totems_artifacts.gathering.Gathering;
import io.github.bubba8587.bubbas_totems_artifacts.growth.Growth;
import io.github.bubba8587.bubbas_totems_artifacts.keeping.Keeping;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModComponents;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.SeekKinds;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.Seeker;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.TotemOfSeekingItem;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsLoot;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The five totems of 2026-11-16 (specs/totems-artifacts.md): Keeping, Gathering, Fortification, Growth, Seeking. Core
 * tests (one per totem) run through BubbasTotemsArtifactsCoreGameTest; the rest is milestone. The beacon side:
 * TotemsBeaconGameTest.
 */
public class TotemsGameTest {
	private static final String FIELD = TestSupport.FIELD;

	// --- Keeping ---------------------------------------------------------------------------------------

	/**
	 * Core. With keep_inventory off, a player carrying a Totem of Keeping (in the backpack) dies: nothing drops, no XP
	 * orbs, one totem is used up; on respawn the inventory and XP levels are back and the mark is gone. The next death,
	 * with no totem left, drops as usual.
	 */
	public void keepingKeepsEverythingOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		level.getGameRules().set(GameRules.KEEP_INVENTORY, false, level.getServer());
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		player.getInventory().setItem(20, new ItemStack(ModItems.TOTEM_OF_KEEPING));
		player.getInventory().setItem(3, new ItemStack(Items.DIRT, 10));
		player.setExperienceLevels(5);

		die(player);
		helper.assertTrue(player.isDeadOrDying(), "player didn't die");
		AABB around = new AABB(at, at).inflate(6.0);
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "a kept death dropped items");
		helper.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class, around).isEmpty(), "a kept death dropped XP");
		helper.assertTrue(Keeping.isKept(player), "the dead player isn't marked kept");
		helper.assertTrue(count(player.getInventory(), ModItems.TOTEM_OF_KEEPING) == 0, "the totem wasn't used up");

		ServerPlayer reborn = level.getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
		helper.assertTrue(count(reborn.getInventory(), Items.DIRT) == 10, "dirt not kept: " + count(reborn.getInventory(), Items.DIRT));
		helper.assertTrue(reborn.experienceLevel == 5, "XP levels not kept: " + reborn.experienceLevel);
		helper.assertFalse(Keeping.isKept(reborn), "the respawned player is still marked kept");

		// No totem left: the next death drops as usual.
		die(reborn);
		// The respawn point may be in a chunk that isn't loaded, so look at the inventory: dropping empties it.
		boolean dropped = count(reborn.getInventory(), Items.DIRT) == 0 && !Keeping.isKept(reborn);
		level.getServer().getPlayerList().remove(reborn);
		helper.assertTrue(dropped, "a death without a totem kept the inventory");
		helper.succeed();
	}

	/** Milestone. With keep_inventory on, the totem isn't used (vanilla keeps everything anyway). */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void keepingNotUsedWithKeepInventory(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		level.getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());
		try {
			ServerPlayer player = TestSupport.survivalPlayer(helper);
			Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
			player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
			player.getInventory().setItem(20, new ItemStack(ModItems.TOTEM_OF_KEEPING));
			die(player);
			helper.assertFalse(Keeping.isKept(player), "used the totem under keep_inventory");
			ServerPlayer reborn = level.getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
			helper.assertTrue(count(reborn.getInventory(), ModItems.TOTEM_OF_KEEPING) == 1, "the totem is gone");
			level.getServer().getPlayerList().remove(reborn);
		} finally {
			level.getGameRules().set(GameRules.KEEP_INVENTORY, false, level.getServer());
		}
		helper.succeed();
	}

	/** Dies at once (a fake player counts as still loading the world, which makes it immune to damage). */
	/**
	 * Milestone. A Soulfire death (Reworked Mobs' {@code #bubbas_reworked_mobs:bypasses_totems}) isn't kept by a Totem of
	 * Keeping; with Soulguard it is. Passes trivially without Reworked Mobs (-PwithMobs runs it for real).
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void keepingSkippedOnSoulfireDeath(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var soulBurn = ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Identifier.parse("bubbas_reworked_mobs:soul_burn"));
		var damageTypes = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
		if (damageTypes.get(soulBurn).isEmpty()) {
			helper.succeed();
			return;
		}
		level.getGameRules().set(GameRules.KEEP_INVENTORY, false, level.getServer());
		for (boolean guarded : new boolean[] {false, true}) {
			ServerPlayer player = TestSupport.survivalPlayer(helper);
			Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
			player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
			player.getInventory().setItem(20, new ItemStack(ModItems.TOTEM_OF_KEEPING));
			player.getInventory().setItem(3, new ItemStack(Items.DIRT, 10));
			if (guarded) {
				player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
						BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("bubbas_reworked_mobs:soulguard")).orElseThrow(), 600));
			}
			player.setHealth(0.0F);
			player.die(level.damageSources().source(soulBurn));
			boolean kept = Keeping.isKept(player);
			ServerPlayer reborn = level.getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
			int dirt = count(reborn.getInventory(), Items.DIRT);
			level.getServer().getPlayerList().remove(reborn);
			helper.assertTrue(kept == guarded && (dirt == 10) == guarded,
					(guarded ? "with Soulguard" : "without Soulguard") + ": kept " + kept + ", dirt " + dirt);
		}
		helper.succeed();
	}

	private static void die(ServerPlayer player) {
		player.setHealth(0.0F);
		player.die(player.damageSources().genericKill());
	}

	private static int count(Inventory inventory, Item item) {
		int n = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(item)) {
				n += inventory.getItem(i).getCount();
			}
		}
		return n;
	}

	// --- Gathering -------------------------------------------------------------------------------------

	private static ArmorStand holder(GameTestHelper helper, BlockPos pos, Item totem) {
		ArmorStand stand = helper.spawn(EntityTypes.ARMOR_STAND, pos);
		stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(totem));
		return stand;
	}

	/**
	 * Core. Held (by an armor stand: any living holder, B18), the totem pulls a dropped item 6 blocks away to the
	 * holder; one 12 blocks away stays put.
	 */
	public void gatheringPullsNearbyItems(GameTestHelper helper) {
		ArmorStand stand = holder(helper, new BlockPos(4, 1, 8), ModItems.TOTEM_OF_GATHERING);
		ItemEntity near = new ItemEntity(helper.getLevel(), helper.absoluteVec(new Vec3(10.5, 1.1, 8.5)).x, helper.absoluteVec(new Vec3(10.5, 1.1, 8.5)).y,
				helper.absoluteVec(new Vec3(10.5, 1.1, 8.5)).z, new ItemStack(Items.DIRT));
		near.setDeltaMovement(Vec3.ZERO);
		helper.getLevel().addFreshEntity(near);
		Vec3 farAt = helper.absoluteVec(new Vec3(16.5, 1.1, 8.5));
		ItemEntity far = new ItemEntity(helper.getLevel(), farAt.x, farAt.y, farAt.z, new ItemStack(Items.STONE));
		far.setDeltaMovement(Vec3.ZERO);
		helper.getLevel().addFreshEntity(far);
		helper.succeedWhen(() -> {
			helper.assertTrue(near.distanceTo(stand) < 1.5, "near item still " + near.distanceTo(stand) + " away");
			helper.assertTrue(far.position().distanceTo(farAt) < 0.5, "far item moved to " + far.position());
		});
	}

	/** Milestone. Items the holder just threw (still on pickup delay) aren't pulled back; ones that can never be picked up neither. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void gatheringSkipsOwnThrowsAndDisplays(GameTestHelper helper) {
		ArmorStand stand = holder(helper, new BlockPos(4, 1, 8), ModItems.TOTEM_OF_GATHERING);
		Vec3 at = helper.absoluteVec(new Vec3(8.5, 1.1, 8.5));
		ItemEntity thrown = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.DIRT));
		thrown.setThrower(stand);
		thrown.setPickUpDelay(40);
		ItemEntity display = new ItemEntity(helper.getLevel(), at.x, at.y, at.z + 2, new ItemStack(Items.DIRT));
		display.setNeverPickUp();
		ItemEntity other = new ItemEntity(helper.getLevel(), at.x, at.y, at.z - 2, new ItemStack(Items.DIRT));
		other.setPickUpDelay(40); // someone else's throw: pulled
		for (ItemEntity e : new ItemEntity[] {thrown, display, other}) {
			e.setDeltaMovement(Vec3.ZERO);
			helper.getLevel().addFreshEntity(e);
		}
		helper.assertTrue(Gathering.pull(helper.getLevel(), stand) == 1, "pulled something other than the one item it should");
		helper.assertTrue(other.getDeltaMovement().lengthSqr() > 0.0, "someone else's throw wasn't pulled");
		helper.succeed();
	}

	// --- Fortification ---------------------------------------------------------------------------------

	/** Fills a 5x3x5 dirt cube (soft, so a blast inside breaks most of it) with its bottom centre at {@code centre}. */
	private static void cube(GameTestHelper helper, BlockPos centre) {
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, 0, -2), centre.offset(2, 2, 2))) {
			helper.setBlock(p, Blocks.DIRT);
		}
	}

	private static int dirtLeft(GameTestHelper helper, BlockPos centre) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, 0, -2), centre.offset(2, 2, 2))) {
			if (helper.getBlockState(p).is(Blocks.DIRT)) {
				n++;
			}
		}
		return n;
	}

	/** Core. An explosion inside a stone cube next to someone holding the totem breaks no block (and the explosion still happens). */
	public void fortificationStopsBlockDamage(GameTestHelper helper) {
		BlockPos centre = new BlockPos(6, 1, 8);
		cube(helper, centre);
		ArmorStand stand = holder(helper, new BlockPos(10, 1, 8), ModItems.TOTEM_OF_FORTIFICATION);
		Vec3 blast = helper.absoluteVec(new Vec3(6.5, 2.5, 8.5));
		helper.getLevel().explode(null, blast.x, blast.y, blast.z, 3.0F, Level.ExplosionInteraction.TNT);
		helper.assertTrue(dirtLeft(helper, centre) == 75, "the explosion broke " + (75 - dirtLeft(helper, centre)) + " blocks next to a holder");
		helper.succeed();
	}

	/** Milestone. The same explosion with nobody holding a totem nearby breaks blocks (the test above means something). */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void explosionWithoutTotemBreaksBlocks(GameTestHelper helper) {
		BlockPos centre = new BlockPos(6, 1, 8);
		cube(helper, centre);
		Vec3 blast = helper.absoluteVec(new Vec3(6.5, 2.5, 8.5));
		helper.getLevel().explode(null, blast.x, blast.y, blast.z, 3.0F, Level.ExplosionInteraction.TNT);
		helper.assertTrue(dirtLeft(helper, centre) < 60, "the control explosion broke only " + (75 - dirtLeft(helper, centre)));
		helper.succeed();
	}

	/** Milestone. Only blocks within 8 blocks of the holder are spared: a long cube reaching past that loses its far end. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void fortificationOnlyNearTheHolder(GameTestHelper helper) {
		var level = helper.getLevel();
		var blocks = new java.util.ArrayList<BlockPos>();
		BlockPos near = helper.absolutePos(new BlockPos(6, 1, 8));
		BlockPos far = helper.absolutePos(new BlockPos(19, 1, 8));
		blocks.add(near);
		blocks.add(far);
		ArmorStand stand = holder(helper, new BlockPos(2, 1, 8), ModItems.TOTEM_OF_FORTIFICATION);
		var kept = Fortification.protect(level, Vec3.atCenterOf(helper.absolutePos(new BlockPos(12, 1, 8))), 10.0F,
				net.minecraft.world.level.Explosion.BlockInteraction.DESTROY, blocks);
		helper.assertTrue(kept.size() == 1 && kept.getFirst().equals(far), "kept for breaking: " + kept + " (holder at " + stand.blockPosition() + ")");
		helper.succeed();
	}

	// --- Growth ----------------------------------------------------------------------------------------

	/** Plants a 3x3 wheat patch (age 0) on farmland with its centre at {@code centre} (the crop layer). */
	private static void wheat(GameTestHelper helper, BlockPos centre) {
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 0, 1))) {
			helper.setBlock(p.below(), Blocks.FARMLAND);
			helper.setBlock(p, Blocks.WHEAT);
		}
	}

	private static int ages(GameTestHelper helper, BlockPos centre) {
		int sum = 0;
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 0, 1))) {
			var state = helper.getBlockState(p);
			sum += state.is(Blocks.WHEAT) ? state.getValue(CropBlock.AGE) : 0;
		}
		return sum;
	}

	/**
	 * Core. Extra random ticks in the held totem's box make wheat inside it grow; wheat outside the box doesn't (the
	 * same number of ticks, many times the held rate so the test is quick and sure).
	 */
	public void growthSpeedsUpCrops(GameTestHelper helper) {
		BlockPos inside = new BlockPos(5, 1, 8);
		BlockPos outside = new BlockPos(20, 1, 8);
		wheat(helper, inside);
		wheat(helper, outside);
		var box = Growth.heldBox(helper.absolutePos(inside));
		int grown = 0;
		for (int i = 0; i < 40; i++) {
			grown += Growth.boost(helper.getLevel(), box, 400);
		}
		helper.assertTrue(grown > 0 && ages(helper, inside) > 0, "no wheat grew in the box (" + grown + ")");
		helper.assertTrue(ages(helper, outside) == 0, "wheat outside the box grew");
		helper.succeed();
	}

	/** Milestone. A real held totem (armor stand) for 200 ticks against vanilla's own rate: the patch in range is ahead. */
	@GameTest(structure = FIELD, maxTicks = 420)
	public void heldGrowthOutpacesVanilla(GameTestHelper helper) {
		helper.getLevel().getGameRules().set(GameRules.RANDOM_TICK_SPEED, 100, helper.getLevel().getServer());
		BlockPos inside = new BlockPos(5, 1, 8);
		BlockPos outside = new BlockPos(20, 1, 8);
		wheat(helper, inside);
		wheat(helper, outside);
		holder(helper, new BlockPos(5, 1, 10), ModItems.TOTEM_OF_GROWTH);
		helper.runAfterDelay(400, () -> {
			helper.getLevel().getGameRules().set(GameRules.RANDOM_TICK_SPEED, 3, helper.getLevel().getServer());
			int in = ages(helper, inside);
			int out = ages(helper, outside);
			helper.assertTrue(in > out, "with the totem " + in + ", without " + out);
			helper.succeed();
		});
	}

	// --- Seeking ---------------------------------------------------------------------------------------

	/**
	 * Core. A biome search finds the player's own biome at least the minimum distance away, and a search skipping that
	 * spot lands well clear of it; arriving at a target records the find (Found: 1/32) and the next target is never
	 * the biome underfoot; the memory keeps only the newest 32.
	 */
	public void seekingFindsAndRemembers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Seeker.clear();
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		BlockPos here = player.blockPosition();
		SeekKinds.Kind biome = new SeekKinds.Kind(level.getBiome(here).unwrapKey().orElseThrow().identifier(), true);
		Optional<BlockPos> first = Seeker.lookupNow(level, biome, here, List.of());
		helper.assertTrue(first.isPresent(), "no " + biome.id() + " found around a " + biome.id() + " test world");
		double min = TotemsTuning.SEEK_MIN_DISTANCE;
		helper.assertTrue(flat(first.get(), here) >= min, "found " + first.get() + ", closer than " + min + " to " + here);
		Optional<BlockPos> second = Seeker.lookupNow(level, biome, here, List.of(first.get()));
		helper.assertTrue(second.isPresent() && flat(second.get(), first.get()) >= TotemsTuning.SEEK_SKIP_BIOME,
				"skipping " + first.get() + " found " + second);

		ItemStack totem = new ItemStack(ModItems.TOTEM_OF_SEEKING);
		BlockPos spot = here.offset(10, 0, 0);
		totem.set(ModComponents.SEEKING_TARGET, new TotemOfSeekingItem.Target(biome, GlobalPos.of(level.dimension(), here), Optional.of(spot)));
		TotemOfSeekingItem.update(totem, level, player);
		var found = TotemOfSeekingItem.finds(totem);
		helper.assertTrue(found.size() == 1 && found.getFirst().pos().pos().equals(spot), "arriving recorded " + found);
		var next = totem.get(ModComponents.SEEKING_TARGET);
		helper.assertTrue(next == null || !next.kind().equals(biome), "picked the biome underfoot again: " + next);
		helper.assertFalse(totem.has(DataComponents.LODESTONE_TRACKER), "the needle doesn't spin while the next search runs");

		for (int i = 0; i < TotemsTuning.SEEK_MEMORY + 8; i++) {
			TotemOfSeekingItem.remember(totem, new TotemOfSeekingItem.Found(biome, GlobalPos.of(level.dimension(), new BlockPos(i * 1000, 0, 0))));
		}
		found = TotemOfSeekingItem.finds(totem);
		helper.assertTrue(found.size() == TotemsTuning.SEEK_MEMORY && found.getLast().pos().pos().getX() == (TotemsTuning.SEEK_MEMORY + 7) * 1000,
				"memory holds " + found.size() + ", newest " + found.getLast());
		level.getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	private static double flat(BlockPos a, BlockPos b) {
		return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getZ() - b.getZ(), 2));
	}

	/**
	 * Milestone. For every structure kind of the dimension, the stepped search is no further than vanilla's /locate
	 * (findNearestMapStructure, no new references) whenever vanilla's answer is within the totem's distance band.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void seekingMatchesVanillaLocate(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Seeker.clear();
		BlockPos origin = helper.absolutePos(new BlockPos(12, 1, 8));
		int compared = 0;
		StringBuilder log = new StringBuilder();
		for (SeekKinds.Kind kind : SeekKinds.structures(level)) {
			var holder = SeekKinds.structure(level, kind);
			if (holder.isEmpty()) {
				continue;
			}
			var vanilla = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder.get()), origin, 20, false);
			if (vanilla == null || flat(vanilla.getFirst(), origin) < TotemsTuning.SEEK_MIN_DISTANCE || flat(vanilla.getFirst(), origin) > TotemsTuning.SEEK_REACH) {
				continue;
			}
			Optional<BlockPos> ours = Seeker.lookupNow(level, kind, origin, List.of());
			compared++;
			log.append(kind.id().getPath()).append(": ours ").append(ours.orElse(null)).append(" vanilla ").append(vanilla.getFirst()).append("; ");
			helper.assertTrue(ours.map(p -> flat(p, origin)).orElse(Double.MAX_VALUE) <= flat(vanilla.getFirst(), origin) + 1.0,
					kind.id() + " ours " + ours + " is further than vanilla's " + vanilla.getFirst());
		}
		io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts.LOGGER.info("Seeking vs /locate ({} kinds): {}", compared, log);
		helper.succeed();
	}

	/** Milestone. The ring walk visits every cell of each square ring exactly once. */
	@GameTest(maxTicks = 5)
	public void seekingRingCoversEveryCell(GameTestHelper helper) {
		for (int r = 0; r <= 6; r++) {
			java.util.Set<Long> seen = new java.util.HashSet<>();
			int cells = r == 0 ? 1 : 8 * r;
			for (int i = 0; i < cells; i++) {
				int[] d = Seeker.ringCell(r, i);
				helper.assertTrue(Math.max(Math.abs(d[0]), Math.abs(d[1])) == r, "cell " + i + " of ring " + r + " is off the ring");
				seen.add(((long) d[0] << 32) ^ (d[1] & 0xffffffffL));
			}
			helper.assertTrue(seen.size() == cells, "ring " + r + " visits " + seen.size() + " of " + cells);
		}
		helper.succeed();
	}

	// --- loot ------------------------------------------------------------------------------------------

	/** Milestone. Every listed chest gives its totem at about its chance (300 rolls each, generous bounds). */
	@GameTest(maxTicks = 20)
	public void totemsAreFoundInLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (Map.Entry<Item, Map<ResourceKey<LootTable>, Float>> totem : TotemsLoot.all().entrySet()) {
			for (Map.Entry<ResourceKey<LootTable>, Float> table : totem.getValue().entrySet()) {
				LootTable loot = level.getServer().reloadableRegistries().getLootTable(table.getKey());
				if (table.getKey().identifier().getPath().startsWith("entities/")) {
					continue;
				}
				if (loot == LootTable.EMPTY) {
					continue; // another mod's table (Reworked Mobs' crypts) that isn't installed in this run
				}
				int found = 0;
				int rolls = 300;
				for (int i = 0; i < rolls; i++) {
					LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
					for (ItemStack stack : loot.getRandomItems(params)) {
						if (stack.is(totem.getKey())) {
							found++;
						}
					}
				}
				double expected = rolls * table.getValue();
				helper.assertTrue(found > expected * 0.3 && found < expected * 2.0 + 5,
						totem.getKey() + " in " + table.getKey().identifier() + ": " + found + " of " + rolls + " (expected about " + expected + ")");
			}
		}
		// Evokers: Keeping at about EVOKER_KEEPING beside their Undying, never Warding; rolled as entity loot with the evoker as the victim.
		var evoker = helper.spawnWithNoFreeWill(EntityTypes.EVOKER, new BlockPos(1, 1, 1));
		LootTable evokerLoot = level.getServer().reloadableRegistries().getLootTable(EntityTypes.EVOKER.getDefaultLootTable().orElseThrow());
		int fromEvokers = 0;
		int wardingFromEvokers = 0;
		int undying = 0;
		for (int i = 0; i < 1000; i++) {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, evoker)
					.withParameter(LootContextParams.ORIGIN, evoker.position())
					.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic()).create(LootContextParamSets.ENTITY);
			for (ItemStack stack : evokerLoot.getRandomItems(params)) {
				if (stack.is(ModItems.TOTEM_OF_KEEPING)) {
					fromEvokers++;
				} else if (stack.is(ModItems.TOTEM_OF_WARDING)) {
					wardingFromEvokers++;
				} else if (stack.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
					undying++;
				}
			}
		}
		evoker.discard();
		double keeping = 1000 * io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning.EVOKER_KEEPING;
		helper.assertTrue(fromEvokers > keeping * 0.7 && fromEvokers < keeping * 1.3, "evokers dropping a Totem of Keeping: " + fromEvokers + " of 1000");
		helper.assertTrue(wardingFromEvokers == 0, "evokers no longer drop Warding: " + wardingFromEvokers);
		helper.assertTrue(undying == 1000, "evokers still drop their Totem of Undying every time: " + undying);
		helper.succeed();
	}

	/** Every totem stacks to one and is uncommon, like the Totem of Undying; the tuning stays in sensible bounds. */
	@GameTest(maxTicks = 5)
	public void totemsAreArtifacts(GameTestHelper helper) {
		for (io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType type : io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType.values()) {
			ItemStack stack = new ItemStack(type.item());
			helper.assertTrue(stack.getMaxStackSize() == 1, type + " stacks");
			helper.assertTrue(stack.getRarity() != net.minecraft.world.item.Rarity.COMMON, type + " is common");
		}
		helper.assertTrue(new ItemStack(ModItems.GRAVE_TALISMAN).getMaxStackSize() == 1, "the talisman stacks");
		helper.succeed();
	}

	/**
	 * Saved worlds: items saved under their old Bubba's Reworked Mobs ids load as this mod's items (Fabric registry
	 * aliases), both by registry lookup and through the item stack codec a world save uses.
	 */
	@GameTest
	public void oldReworkedMobsIdsLoadAsTheseItems(GameTestHelper helper) {
		checkOldId(helper, "grave_talisman", ModItems.GRAVE_TALISMAN);
		checkOldId(helper, "totem_of_warding", ModItems.TOTEM_OF_WARDING);
		helper.succeed();
	}

	private static void checkOldId(GameTestHelper helper, String name, Item expected) {
		Identifier old = Identifier.fromNamespaceAndPath(ModItems.OLD_NAMESPACE, name);
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(old) == expected, old + " doesn't look up as the new item");
		CompoundTag saved = new CompoundTag();
		saved.putString("id", old.toString());
		saved.putInt("count", 1);
		ItemStack stack = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), saved)
				.getOrThrow(e -> new AssertionError("old " + name + " stack didn't load: " + e));
		helper.assertTrue(stack.is(expected), "old " + name + " stack loaded as " + stack);
		// Saved again, it is written under the new id.
		var written = ItemStack.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
		helper.assertTrue(written instanceof CompoundTag tag && tag.getStringOr("id", "").equals("bubbas_totems_artifacts:" + name),
				"re-saved " + name + " as " + written);
	}
}
