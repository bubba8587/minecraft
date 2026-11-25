// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.captain.CaptainSpawning;
import io.github.bubba8587.bubbas_reworked_mobs.captain.CaptainTuning;
import io.github.bubba8587.bubbas_reworked_mobs.captain.Captains;
import io.github.bubba8587.bubbas_reworked_mobs.captain.SoulfireBlaze;
import io.github.bubba8587.bubbas_reworked_mobs.captain.SoulfireCharge;
import io.github.bubba8587.bubbas_reworked_mobs.captain.WitherCaptain;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** The Wither Captain, Soulfire Blazes and the Captain's Helm (specs/wither-captain.md). Milestone; one core smoke test. */
public class CaptainGameTest {
	static WitherCaptain captain(GameTestHelper helper, double x, double z) {
		ServerLevel level = helper.getLevel();
		WitherCaptain captain = Captains.WITHER_CAPTAIN.create(level, EntitySpawnReason.COMMAND);
		Vec3 at = helper.absoluteVec(new Vec3(x, 1.0, z));
		captain.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		captain.finalizeSpawn(level, level.getCurrentDifficultyAt(captain.blockPosition()), EntitySpawnReason.COMMAND, null);
		captain.setPersistenceRequired();
		level.addFreshEntity(captain);
		TestCleanup.whenDone(helper, () -> {
			captain.liveBlazes(level).forEach(SoulfireBlaze::discard);
			captain.discard();
		});
		return captain;
	}

	/** A survival player that won't die in the fight (resistance V), taken off again when the test is done. */
	static ServerPlayer player(GameTestHelper helper, double x, double z) {
		ServerPlayer player = TestPlayers.survivalAt(helper, x, z);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60, 4, false, false));
		TestCleanup.whenDone(helper, () -> TestPlayers.remove(helper, player));
		return player;
	}

	/** Core: a captain spawned by command that starts fighting a player enters the summoning phase and summons a Soulfire Blaze. */
	public void summonsWhenItFightsAPlayer(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerPlayer player = player(helper, 1.5, 1.5);
		WitherCaptain captain = captain(helper, 5.5, 5.5);
		captain.setTarget(player);
		helper.succeedWhen(() -> {
			helper.assertTrue(captain.hasEngaged() && captain.isSummoning(), "not summoning");
			helper.assertTrue(!captain.liveBlazes(helper.getLevel()).isEmpty(), "no Soulfire Blaze yet");
		});
	}

	/** In the phase it takes a quarter damage and summons 1-3 blazes, then returns to normal, and summons again after 4 hits. */
	@GameTest(maxTicks = 400)
	public void phaseCutsDamageEndsAndComesBackAfterHits(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, 1.5, 1.5);
		WitherCaptain captain = captain(helper, 5.5, 5.5);
		captain.setTarget(player);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(captain.isSummoning(), "not summoning"))
				.thenExecute(() -> {
					float before = captain.getHealth();
					captain.setInvulnerableTime(0);
					captain.hurtServer(level, level.damageSources().magic(), 8.0F);
					float lost = before - captain.getHealth();
					helper.assertTrue(Math.abs(lost - 8.0F * CaptainTuning.SUMMONING_DAMAGE_FACTOR) < 0.01F, "took " + lost + " of 8");
				})
				.thenWaitUntil(() -> helper.assertTrue(!captain.isSummoning(), "still summoning"))
				.thenExecute(() -> {
					int n = captain.blazesSummoned();
					helper.assertTrue(n >= CaptainTuning.BLAZES_MIN && n <= CaptainTuning.BLAZES_MAX, "summoned " + n);
					for (int hit = 1; hit <= CaptainTuning.HITS_TO_SUMMON; hit++) {
						helper.assertTrue(!captain.isSummoning(), "summoning after " + (hit - 1) + " hits");
						// rising damage: each hit beats the last within the damage cooldown
						captain.hurtServer(level, level.damageSources().playerAttack(player), hit);
					}
					helper.assertTrue(captain.isSummoning(), "not summoning after " + CaptainTuning.HITS_TO_SUMMON + " hits");
				})
				.thenSucceed();
	}

	/** Its melee damage is within 1.5x a wither skeleton's (the same stone sword on both). */
	@GameTest(maxTicks = 5)
	public void meleeDamageCloseToAWitherSkeleton(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WitherCaptain captain = Captains.WITHER_CAPTAIN.create(level, EntitySpawnReason.COMMAND);
		WitherSkeleton skeleton = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.COMMAND);
		captain.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), EntitySpawnReason.COMMAND, null);
		skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), EntitySpawnReason.COMMAND, null);
		double ours = captain.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
		double theirs = skeleton.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
		helper.assertTrue(ours >= theirs && ours <= theirs * 1.5, "captain " + ours + " vs wither skeleton " + theirs);
		helper.assertTrue(captain.getMainHandItem().is(Items.STONE_SWORD), "sword " + captain.getMainHandItem());
		helper.assertTrue(captain.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CAPTAINS_HELM), "helm " + captain.getItemBySlot(EquipmentSlot.HEAD));
		helper.succeed();
	}

	/** Over many loot rolls: 1-2 shards every time, the helm about 1 in 4, wither skeleton bones too. */
	@GameTest(maxTicks = 5)
	public void dropsShardsAndSometimesTheHelm(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WitherCaptain captain = Captains.WITHER_CAPTAIN.create(level, EntitySpawnReason.COMMAND);
		captain.snapTo(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
		ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("entities/wither_captain"));
		int rolls = 400;
		int helms = 0;
		int bones = 0;
		for (int i = 0; i < rolls; i++) {
			List<ItemStack> drops = new ArrayList<>();
			captain.dropFromLootTable(level, level.damageSources().generic(), false, table, drops::add);
			int shards = drops.stream().filter(s -> s.is(ModItems.NETHER_STAR_SHARD)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(shards >= 1 && shards <= 2, "roll " + i + " gave " + shards + " shards");
			helms += drops.stream().filter(s -> s.is(ModItems.CAPTAINS_HELM)).mapToInt(ItemStack::getCount).sum();
			bones += drops.stream().filter(s -> s.is(Items.BONE)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(drops.stream().noneMatch(s -> s.is(Items.WITHER_SKELETON_SKULL)), "a skull without a player kill");
		}
		helper.assertTrue(helms >= rolls * 0.15 && helms <= rolls * 0.35, helms + " helms in " + rolls);
		helper.assertTrue(bones > 0, "no bones");
		// Player kills: a skull about 1 in 3.
		ServerPlayer killer = helper.makeMockServerPlayerInLevel();
		captain.setLastHurtByPlayer(killer, 100);
		int withSkull = 0;
		for (int i = 0; i < rolls; i++) {
			List<ItemStack> drops = new ArrayList<>();
			captain.dropFromLootTable(level, level.damageSources().playerAttack(killer), true, table, drops::add);
			if (drops.stream().anyMatch(s -> s.is(Items.WITHER_SKELETON_SKULL))) {
				withSkull++;
			}
		}
		helper.assertTrue(withSkull >= rolls * 0.24 && withSkull <= rolls * 0.44, withSkull + " skulls in " + rolls + " player kills");
		helper.succeed();
	}

	/** Four shards craft one nether star. */
	@GameTest(maxTicks = 5)
	public void fourShardsCraftANetherStar(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack shard = new ItemStack(ModItems.NETHER_STAR_SHARD);
		CraftingInput input = CraftingInput.of(2, 2, List.of(shard.copy(), shard.copy(), shard.copy(), shard.copy()));
		var holder = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
		helper.assertTrue(holder != null, "no recipe for four shards");
		@SuppressWarnings("unchecked")
		ItemStack out = ((Recipe<CraftingInput>) holder.value()).assemble(input);
		helper.assertTrue(out.is(Items.NETHER_STAR) && out.getCount() == 1, "made " + out);
		helper.succeed();
	}

	/** A Soulfire Blaze fighting a player shoots Soulfire Charges, not vanilla fireballs. */
	@GameTest(maxTicks = 300)
	public void soulfireBlazeShootsCharges(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerPlayer player = player(helper, 1.5, 1.5);
		SoulfireBlaze blaze = helper.spawn(Captains.SOULFIRE_BLAZE, new Vec3(6.5, 1.0, 6.5));
		blaze.setTarget(player);
		AABB around = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(24.0);
		helper.succeedWhen(() -> {
			ServerLevel level = helper.getLevel();
			helper.assertTrue(level.getEntitiesOfClass(SmallFireball.class, around, f -> f.getType() == EntityTypes.SMALL_FIREBALL).isEmpty(),
					"a vanilla fireball");
			helper.assertTrue(!level.getEntitiesOfClass(SoulfireCharge.class, around, f -> true).isEmpty(), "no charge yet");
		});
	}

	private static void dropOnto(GameTestHelper helper, AbstractHurtingProjectile projectile, double x, double y, double z) {
		Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
		projectile.snapTo(at.x, at.y, at.z, 0.0F, 90.0F);
		projectile.setDeltaMovement(0.0, -projectile.accelerationPower, 0.0);
		helper.getLevel().addFreshEntity(projectile);
	}

	/** A charge hits harder than a blaze fireball (first hit, before any burning). */
	@GameTest(maxTicks = 60)
	public void chargeHitsHarderThanAFireball(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerLevel level = helper.getLevel();
		Pig chargePig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(1.5, 1.0, 4.5));
		Pig fireballPig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(6.5, 1.0, 4.5));
		dropOnto(helper, Captains.SOULFIRE_CHARGE.create(level, EntitySpawnReason.TRIGGERED), 1.5, 3.0, 4.5);
		dropOnto(helper, new SmallFireball(EntityTypes.SMALL_FIREBALL, level), 6.5, 3.0, 4.5);
		helper.succeedWhen(() -> {
			float charge = chargePig.getMaxHealth() - chargePig.getHealth();
			float fireball = fireballPig.getMaxHealth() - fireballPig.getHealth();
			helper.assertTrue(fireball > 0.0F && charge > 0.0F, "not both hit yet");
			helper.assertTrue(charge >= CaptainTuning.CHARGE_DAMAGE - 0.01F && charge > fireball, "charge " + charge + " vs fireball " + fireball);
		});
	}

	/** Where a charge lands it places soul fire on soul sand and fire on netherrack. */
	@GameTest(maxTicks = 60)
	public void chargePlacesSoulFireOnSoulSandFireOnNetherrack(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(1, 0, 4, Blocks.SOUL_SAND);
		helper.setBlock(6, 0, 4, Blocks.NETHERRACK);
		dropOnto(helper, Captains.SOULFIRE_CHARGE.create(level, EntitySpawnReason.TRIGGERED), 1.5, 3.0, 4.5);
		dropOnto(helper, Captains.SOULFIRE_CHARGE.create(level, EntitySpawnReason.TRIGGERED), 6.5, 3.0, 4.5);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.SOUL_FIRE, 1, 1, 4);
			helper.assertBlockPresent(Blocks.FIRE, 6, 1, 4);
		});
	}

	private static Pig penned(GameTestHelper helper, int x, int z, Block floor, Block fire) {
		helper.setBlock(x, 0, z, floor);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					helper.setBlock(x + dx, 1, z + dz, Blocks.GLASS);
					helper.setBlock(x + dx, 2, z + dz, Blocks.GLASS);
				}
			}
		}
		helper.setBlock(x, 1, z, fire);
		Pig pig = helper.spawn(EntityTypes.PIG, new Vec3(x + 0.5, 1.0, z + 0.5));
		pig.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CAPTAINS_HELM));
		return pig;
	}

	/** Wearing the helm: no damage standing in soul fire; ordinary fire still hurts. */
	@GameTest(maxTicks = 100)
	public void helmWearerSafeInSoulFireNotInFire(GameTestHelper helper) {
		TestFloors.stone(helper);
		Pig soul = penned(helper, 2, 2, Blocks.SOUL_SAND, Blocks.SOUL_FIRE);
		Pig plain = penned(helper, 5, 5, Blocks.NETHERRACK, Blocks.FIRE);
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(soul.isAlive() && soul.getHealth() == soul.getMaxHealth(), "the soul fire pig took damage: " + soul.getHealth());
			helper.assertTrue(plain.getHealth() < plain.getMaxHealth(), "the fire pig took none");
			helper.succeed();
		});
	}

	/** Its summoned blazes burn out when it dies. */
	@GameTest(maxTicks = 200)
	public void summonedBlazesBurnOutWhenItDies(GameTestHelper helper) {
		TestFloors.stone(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, 1.5, 1.5);
		WitherCaptain captain = captain(helper, 5.5, 5.5);
		captain.setTarget(player);
		List<SoulfireBlaze> blazes = new ArrayList<>();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!captain.liveBlazes(level).isEmpty(), "no blaze yet"))
				.thenExecute(() -> {
					blazes.addAll(captain.liveBlazes(level));
					captain.kill(level);
				})
				.thenWaitUntil(() -> helper.assertTrue(blazes.stream().allMatch(SoulfireBlaze::isRemoved), "a blaze is still burning"))
				.thenSucceed();
	}

	/** Outside a fortress a wither skeleton spawn never becomes a captain. */
	@GameTest(maxTicks = 5)
	public void noCaptainOutsideAFortress(GameTestHelper helper) {
		helper.assertTrue(!CaptainSpawning.captainAllowedAt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))), "allowed outside a fortress");
		helper.succeed();
	}
}
