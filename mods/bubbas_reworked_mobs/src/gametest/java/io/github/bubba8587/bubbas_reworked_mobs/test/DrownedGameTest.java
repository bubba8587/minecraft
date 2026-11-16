// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedAccess;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedMind;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedTuning;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.LurkSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The lurking drowned (specs/drowned.md). Each test builds the same pool on the flat field: land at x 0-6 (top at
 * y 2), 2-deep shallows at x 7-11 (floor top y 0, surface ~1.89), an 8-deep pit at x 12-20 (floor top y -6). Each test
 * has its own environment so its batch is alone (a drowned senses 16 blocks).
 */
public class DrownedGameTest {
	static final String FIELD = CreeperGameTest.FIELD;
	/** The lurk spot the tests use: by the shore, 2 deep. */
	static final BlockPos SPOT = new BlockPos(7, -1, 8);

	/** Builds the pool (relative coordinates; the field's ceiling is at y 4, so the pool is dug into the ground). */
	static void pool(GameTestHelper helper) {
		for (int z = 0; z <= 15; z++) {
			for (int x = 0; x <= 23; x++) {
				boolean wallZ = z == 0 || z == 15;
				if (x <= 6) {
					helper.setBlock(x, 1, z, Blocks.STONE);
				} else if (x <= 11) {
					helper.setBlock(x, -1, z, Blocks.STONE);
					helper.setBlock(x, 0, z, wallZ ? Blocks.STONE : Blocks.WATER);
					helper.setBlock(x, 1, z, wallZ ? Blocks.STONE : Blocks.WATER);
					if (x == 11) {
						for (int y = -7; y <= -2; y++) {
							helper.setBlock(x, y, z, Blocks.STONE);
						}
					}
				} else if (x <= 20) {
					for (int y = -7; y <= 1; y++) {
						boolean wall = wallZ || y == -7;
						helper.setBlock(x, y, z, wall ? Blocks.STONE : Blocks.WATER);
					}
				} else {
					for (int y = -7; y <= 1; y++) {
						helper.setBlock(x, y, z, Blocks.STONE);
					}
				}
			}
		}
	}

	static Drowned drowned(GameTestHelper helper, double x, double y, double z) {
		Drowned drowned = helper.spawn(EntityTypes.DROWNED, new Vec3(x, y, z));
		drowned.setPersistenceRequired();
		drowned.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		return drowned;
	}

	static LurkSpots.Spot spot(GameTestHelper helper) {
		LurkSpots.Spot spot = LurkSpots.at(helper.getLevel(), helper.absolutePos(SPOT.above(2)));
		helper.assertTrue(spot != null, "the pool's shallows should be a lurk spot");
		return spot;
	}

	/** A drowned lurking at SPOT. */
	static Drowned lurker(GameTestHelper helper) {
		LurkSpots.Spot spot = spot(helper);
		Drowned drowned = drowned(helper, SPOT.getX() + 0.5, SPOT.getY() + 1.0, SPOT.getZ() + 0.5);
		mind(drowned).lurkAt(drowned, spot);
		return drowned;
	}

	static DrownedMind mind(Drowned drowned) {
		return DrownedAccess.mind(drowned);
	}

	/** A survival player standing on the land at (x, 2, z), facing {@code yaw} (90: away from the water, toward -x). */
	static ServerPlayer playerOnLand(GameTestHelper helper, double x, double z, float yaw, float pitch) {
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 abs = helper.absoluteVec(new Vec3(x, 2.0, z));
		player.snapTo(abs.x, abs.y, abs.z, yaw, pitch);
		player.setYHeadRot(yaw);
		player.setOnGround(true);
		return player;
	}

	private static void face(ServerPlayer player, float yaw, float pitch) {
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot(pitch);
	}

	// --- core smoke tests (run from ReworkedMobsCoreGameTest) ------------------------------------------

	/** A player at the edge with their back to the water is grabbed, held (can't sneak off) and dragged backwards to deep water. */
	public void grabsAndDragsToDeepWater(GameTestHelper helper) {
		pool(helper);
		Drowned drowned = lurker(helper);
		ServerPlayer player = playerOnLand(helper, 5.5, 8.5, 90.0F, 0.0F);
		double startY = player.getY();
		boolean[] grabbed = {false};
		int[] tick = {0};
		helper.onEachTick(() -> {
			tick[0]++;
			face(player, 90.0F, 0.0F);
			if (!grabbed[0] && player.getVehicle() == drowned) {
				grabbed[0] = true;
				tick[0] = 1000;
				player.setShiftKeyDown(true);
			}
			if (tick[0] == 1140) {
				helper.assertTrue(player.getVehicle() == drowned, "sneaking shouldn't get you off, state " + mind(drowned).state());
				Vec3 rel = helper.relativeVec(drowned.position());
				helper.assertTrue(rel.x > 11.5, "it should have dragged the player out over the deep pit, at " + rel);
				helper.assertTrue(player.getY() < startY - 2.0, "the player should be pulled under, y " + player.getY());
				helper.assertTrue(helper.getLevel().getFluidState(BlockPos.containing(player.getEyePosition())).is(net.minecraft.tags.FluidTags.WATER), "the player's eyes should be under water");
				helper.assertTrue(!drowned.isSwimming(), "it drags upright in water");
				mind(drowned).release(drowned, false);
				drowned.discard();
				CreeperGameTest.remove(helper, player);
				helper.succeed();
			}
			if (tick[0] == 80 && !grabbed[0]) {
				helper.fail("the drowned should have grabbed the player within 4 s, state " + mind(drowned).state() + " stared " + mind(drowned).isStared());
			}
		});
	}

	/** Fast hard hits (an axe) fill the grip-break meter and it lets go; one weak hit doesn't; the meter drains. */
	public void gripBreakMeterBreaksTheHold(GameTestHelper helper) {
		pool(helper);
		Drowned drowned = drowned(helper, 9.5, 0.0, 8.5);
		Villager villager = helper.spawn(EntityTypes.VILLAGER, new Vec3(9.5, 0.0, 9.5));
		ServerPlayer friend = playerOnLand(helper, 5.5, 8.5, -90.0F, 0.0F);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(2, () -> {
			mind(drowned).grab(drowned, level, villager);
			helper.assertTrue(villager.getVehicle() == drowned, "the villager should be held");
		});
		helper.runAtTickTime(4, () -> {
			drowned.hurtServer(level, level.damageSources().playerAttack(friend), 1.0F);
			float grip = mind(drowned).grip();
			helper.assertTrue(grip > 5 && grip < 15 && villager.getVehicle() == drowned, "a fist hit should add a little, grip " + grip);
		});
		helper.runAtTickTime(60, () -> {
			helper.assertTrue(mind(drowned).grip() == 0.0F, "the meter should drain, grip " + mind(drowned).grip());
			friend.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
			drowned.hurtServer(level, level.damageSources().playerAttack(friend), 9.0F);
			helper.assertTrue(villager.getVehicle() == drowned, "one axe hit shouldn't free it yet, grip " + mind(drowned).grip());
		});
		helper.runAtTickTime(72, () -> {
			drowned.hurtServer(level, level.damageSources().playerAttack(friend), 9.0F);
			helper.assertTrue(villager.getVehicle() == null, "two fast axe hits should break the grip, grip " + mind(drowned).grip());
			helper.assertTrue(mind(drowned).state() != DrownedMind.State.DRAG, "it should have let go");
			drowned.discard();
			villager.discard();
			CreeperGameTest.remove(helper, friend);
			helper.succeed();
		});
	}

	/** Lurking, its eyes are just above the water; stared at, it sinks below; looked away from (out of reach), it rises again. */
	public void staredAtItSinks(GameTestHelper helper) {
		pool(helper);
		Drowned drowned = lurker(helper);
		double surface = spot(helper).surfaceY();
		ServerPlayer player = playerOnLand(helper, 1.5, 8.5, -90.0F, 10.0F);
		int[] tick = {0};
		helper.onEachTick(() -> {
			tick[0]++;
			// Look straight at its head for 40 ticks, then away.
			if (tick[0] <= 40) {
				lookAt(player, DrownedMind.lurkPoint(spot(helper)));
			} else {
				face(player, 90.0F, 0.0F);
			}
			if (tick[0] == 40) {
				helper.assertTrue(mind(drowned).isSunk(), "stared at, it should sink");
				helper.assertTrue(drowned.getY() + drowned.getBbHeight() < surface + 0.1, "its head should be at or under the surface: top "
						+ (drowned.getY() + drowned.getBbHeight()) + " surface " + surface);
			}
			if (tick[0] == 90) {
				helper.assertTrue(!mind(drowned).isSunk() && mind(drowned).state() == DrownedMind.State.LURK, "looked away from, it should lurk again, state "
						+ mind(drowned).state());
				helper.assertTrue(drowned.getEyeY() > surface, "its eyes should be above the water: eyes " + drowned.getEyeY() + " surface " + surface);
				helper.assertTrue(!drowned.isSwimming(), "it lurks upright");
				drowned.discard();
				CreeperGameTest.remove(helper, player);
				helper.succeed();
			}
		});
	}

	static void lookAt(ServerPlayer player, Vec3 target) {
		Vec3 d = target.subtract(player.getEyePosition());
		float yaw = (float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI) - 90.0F;
		float pitch = (float) -(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180.0 / Math.PI);
		face(player, yaw, pitch);
	}

	// --- milestone -------------------------------------------------------------------------------

	/** Spawned in the deep pit with a player on the shore, it swims to a 2-deep lurk spot by the shore and lurks there facing it. */
	@GameTest(environment = "bubbas_reworked_mobs-test:drowned_travel", structure = FIELD, maxTicks = 400)
	public void swimsToALurkSpotNearThePlayer(GameTestHelper helper) {
		pool(helper);
		Drowned drowned = drowned(helper, 17.5, -6.0, 8.5);
		ServerPlayer player = playerOnLand(helper, 2.5, 8.5, 90.0F, 0.0F);
		helper.succeedWhen(() -> {
			face(player, 90.0F, 0.0F);
			DrownedMind mind = mind(drowned);
			helper.assertTrue(mind.state() == DrownedMind.State.LURK, "should reach a lurk spot, state " + mind.state());
			Vec3 rel = helper.relativeVec(drowned.position());
			helper.assertTrue(rel.x >= 7.0 && rel.x < 10.0, "the lurk spot should be in the shallows by the shore, at " + rel);
			helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(drowned.getYRot() - 90.0F)) < 50.0F, "it should face the shore (-x), yaw " + drowned.getYRot());
			drowned.discard();
			CreeperGameTest.remove(helper, player);
		});
	}

	/** Facing the water, nobody is grabbed (back turned only); a creative player isn't grabbed either. */
	@GameTest(environment = "bubbas_reworked_mobs-test:drowned_facing", structure = FIELD, maxTicks = 100)
	public void onlyBacksTurnedAreGrabbed(GameTestHelper helper) {
		pool(helper);
		Drowned drowned = lurker(helper);
		ServerPlayer facing = playerOnLand(helper, 5.5, 7.5, -90.0F, 0.0F);
		@SuppressWarnings("deprecation")
		ServerPlayer creative = helper.makeMockServerPlayerInLevel();
		creative.setGameMode(GameType.CREATIVE);
		Vec3 abs = helper.absoluteVec(new Vec3(5.5, 2.0, 10.5));
		creative.snapTo(abs.x, abs.y, abs.z, 90.0F, 0.0F);
		creative.setYHeadRot(90.0F);
		helper.assertTrue(creative.isCreative(), "the creative player should be in creative");
		helper.runAtTickTime(80, () -> {
			helper.assertTrue(facing.getVehicle() == null, "a player facing it shouldn't be grabbed");
			helper.assertTrue(creative.getVehicle() == null, "a creative player shouldn't be grabbed");
			helper.assertTrue(mind(drowned).state() == DrownedMind.State.LURK, "it should still lurk, state " + mind(drowned).state());
			drowned.discard();
			CreeperGameTest.remove(helper, facing);
			CreeperGameTest.remove(helper, creative);
			helper.succeed();
		});
	}

	/** Its small hits knock air out; Water Breathing stops that (vanilla: no air lost); Respiration (oxygen bonus) halves it at level 1. */
	@GameTest(environment = "bubbas_reworked_mobs-test:drowned_air", structure = FIELD, maxTicks = 40)
	public void hitsKnockAirOutUnlessWaterBreathing(GameTestHelper helper) {
		helper.assertTrue(DrownedTuning.HIT_AIR_LOSS > 0, "hits should knock air out");
		pool(helper);
		ServerLevel level = helper.getLevel();
		Drowned a = drowned(helper, 9.5, 0.0, 4.5);
		Drowned b = drowned(helper, 9.5, 0.0, 11.5);
		Villager plain = helper.spawn(EntityTypes.VILLAGER, new Vec3(9.5, 0.0, 5.5));
		Villager breathing = helper.spawn(EntityTypes.VILLAGER, new Vec3(9.5, 0.0, 12.5));
		breathing.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600));
		helper.runAtTickTime(2, () -> {
			mind(a).grab(a, level, plain);
			mind(b).grab(b, level, breathing);
			plain.setAirSupply(300);
			breathing.setAirSupply(300);
			DrownedMind.hit(a, level, plain);
			DrownedMind.hit(b, level, breathing);
			helper.assertTrue(plain.getAirSupply() <= 300 - DrownedTuning.HIT_AIR_LOSS, "a hit should knock air out, air " + plain.getAirSupply());
			helper.assertTrue(breathing.getAirSupply() == 300, "Water Breathing: no air lost, air " + breathing.getAirSupply());
			mind(a).release(a, false);
			mind(b).release(b, false);
			a.discard();
			b.discard();
			plain.discard();
			breathing.discard();
			helper.succeed();
		});
	}

	/** Axes, Impaling and Knockback count extra on the grip-break meter. */
	@GameTest(maxTicks = 5)
	public void gripPointsFavourAxesImpalingKnockback(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		float sword = DrownedMind.gripPoints(level, new ItemStack(Items.IRON_SWORD), 6.0F);
		float axe = DrownedMind.gripPoints(level, new ItemStack(Items.IRON_AXE), 6.0F);
		ItemStack impaling = new ItemStack(Items.TRIDENT);
		impaling.enchant(enchantments.getOrThrow(Enchantments.IMPALING), 3);
		ItemStack knockback = new ItemStack(Items.IRON_SWORD);
		knockback.enchant(enchantments.getOrThrow(Enchantments.KNOCKBACK), 2);
		helper.assertTrue(axe == sword * DrownedTuning.GRIP_AXE_MULTIPLIER, "axe " + axe + " sword " + sword);
		helper.assertTrue(DrownedMind.gripPoints(level, impaling, 6.0F) == sword + 3 * DrownedTuning.GRIP_IMPALING_POINTS_PER_LEVEL, "impaling");
		helper.assertTrue(DrownedMind.gripPoints(level, knockback, 6.0F) == sword + 2 * DrownedTuning.GRIP_KNOCKBACK_POINTS_PER_LEVEL, "knockback");
		helper.succeed();
	}

	/** Much stronger: tuned health and armor; never spawns with a trident. */
	@GameTest(maxTicks = 5)
	public void strongerAndNoTridents(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < 200; i++) {
			Drowned drowned = EntityTypes.DROWNED.create(level, EntitySpawnReason.NATURAL);
			helper.assertTrue(drowned != null, "drowned");
			drowned.finalizeSpawn(level, level.getCurrentDifficultyAt(helper.absolutePos(BlockPos.ZERO)), EntitySpawnReason.NATURAL, null);
			helper.assertTrue(!drowned.getMainHandItem().is(Items.TRIDENT), "no drowned should spawn with a trident");
			if (i == 0) {
				helper.assertTrue(drowned.getAttributeValue(Attributes.MAX_HEALTH) == DrownedTuning.MAX_HEALTH && drowned.getHealth() == (float) DrownedTuning.MAX_HEALTH, "health");
				helper.assertTrue(drowned.getAttributeValue(Attributes.ARMOR) == DrownedTuning.ARMOR, "armor");
			}
			drowned.discard();
		}
		helper.succeed();
	}

	/** Buried treasure can hold a trident (rare). */
	@GameTest(maxTicks = 5)
	public void buriedTreasureCanHoldATrident(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.BURIED_TREASURE);
		int found = 0;
		for (int i = 0; i < 400; i++) {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO)).create(LootContextParamSets.CHEST);
			for (ItemStack stack : table.getRandomItems(params)) {
				if (stack.is(Items.TRIDENT)) {
					found++;
				}
			}
		}
		helper.assertTrue(found > 10 && found < 90, "about 10 % of buried treasure should hold a trident, found " + found + " in 400");
		helper.succeed();
	}
}
