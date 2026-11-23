// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadCombat;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Combat tuning for our woken undead (specs/undead.md "Tuning"), through the real paths: Mob#doHurtTarget for fists,
 * Player#attack with a Smite sword, real arrows flying into hitboxes, and the zombies' own AI for drifting.
 */
public class UndeadCombatGameTest {
	private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final String LANE = "bubbas_reworked_mobs-test:drift_lane";

	// --- fists ------------------------------------------------------------------------------------

	/** A husk: tough, and unlike a zombie its helmet doesn't wear in sunlight, so all wear on it comes from the hits. */
	private static Mob armoredDummy(GameTestHelper helper, BlockPos pos) {
		Mob dummy = helper.spawnWithNoFreeWill(EntityTypes.HUSK, pos);
		dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
		dummy.setHealth(1000.0F);
		dummy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		dummy.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		dummy.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
		dummy.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
		return dummy;
	}

	private static String wearBySlot(LivingEntity mob) {
		return ARMOR.stream().map(slot -> slot.getName() + "=" + mob.getItemBySlot(slot).getDamageValue()).toList().toString();
	}

	private static int armorWear(LivingEntity mob) {
		return ARMOR.stream().mapToInt(slot -> mob.getItemBySlot(slot).getDamageValue()).sum();
	}

	/**
	 * One fist hit barely scratches armor; a streak of quick hits tears into it (0, 1, 3, 6, 6 extra); a pause longer
	 * than the streak window starts over. Mobs take no vanilla armor wear, so all wear here is ours.
	 */
	@GameTest(maxTicks = 200)
	public void fistStreakShredsArmorAndPauseResetsIt(GameTestHelper helper) {
		Monster undead = helper.spawnWithNoFreeWill(ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(2, 1, 2));
		undead.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no burning, so no stray fire
		Mob victim = armoredDummy(helper, new BlockPos(3, 1, 2));
		// A vanilla zombie's fists do nothing extra (the rule is scoped to our undead).
		Mob vanilla = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 1, 5));
		vanilla.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		Mob vanillaVictim = armoredDummy(helper, new BlockPos(3, 1, 5));
		ServerLevel level = helper.getLevel();
		int gap = 12; // past the victim's 10 invulnerable ticks, well inside the streak window
		int[] expected = {0, 1, 4, 10, 16};
		for (int i = 0; i < 5; i++) {
			int hit = i;
			helper.runAfterDelay(1 + gap * i, () -> {
				helper.assertTrue(undead.doHurtTarget(level, victim), "fist hit " + (hit + 1) + " did not land");
				helper.assertValueEqual(UndeadCombat.fistStreak(undead, victim), hit + 1, "streak after hit " + (hit + 1));
				helper.assertValueEqual(armorWear(victim), expected[hit], "armor wear after hit " + (hit + 1) + " " + wearBySlot(victim));
				helper.assertTrue(vanilla.doHurtTarget(level, vanillaVictim), "vanilla hit did not land");
			});
		}
		int afterPause = 1 + gap * 4 + UndeadTuning.FIST_STREAK_WINDOW_TICKS + 5;
		helper.runAfterDelay(afterPause, () -> {
			helper.assertValueEqual(UndeadCombat.fistStreak(undead, victim), 0, "streak lapsed after a pause");
			helper.assertTrue(undead.doHurtTarget(level, victim), "fist hit after the pause did not land");
			helper.assertValueEqual(UndeadCombat.fistStreak(undead, victim), 1, "streak restarted");
			helper.assertValueEqual(armorWear(victim), 16, "a first hit again adds no extra wear");
			helper.assertValueEqual(armorWear(vanillaVictim), 0, "vanilla zombie fists shredded armor");
			helper.succeed();
		});
	}

	// --- smite ------------------------------------------------------------------------------------

	private static ItemStack smiteSword(GameTestHelper helper, int level) {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE), level);
		return sword;
	}

	/**
	 * Smite XXV makes the kill chance 100 % (4 % per level), so this is deterministic. Our zombie (80 health, far more
	 * than the weak hit deals) dies to the purge damage type; a vanilla zombie with lots of health and a pig survive.
	 */
	@GameTest
	public void smiteKillsOurUndeadOutrightOnly(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, smiteSword(helper, 25));
		Monster ours = helper.spawnWithNoFreeWill(ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(2, 1, 2));
		Mob vanilla = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 1, 2));
		vanilla.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
		vanilla.setHealth(1000.0F);
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(6, 1, 2));

		player.attack(ours);
		helper.assertTrue(ours.isDeadOrDying(), "our zombie survived a certain Smite kill (health " + ours.getHealth() + ")");
		DamageSource last = ours.getLastDamageSource();
		helper.assertTrue(last != null && UndeadCombat.isPurge(last), "killed by " + last);
		helper.assertTrue(last.getEntity() == player, "the kill is credited to the player");

		player.attack(vanilla);
		helper.assertTrue(vanilla.isAlive() && vanilla.getHealth() < 1000.0F, "vanilla zombie: hit but not purged");
		player.attack(pig);
		helper.assertTrue(pig.isAlive(), "a pig was purged");
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	// --- arrows -----------------------------------------------------------------------------------

	/** A real arrow flying +x at {@code height} above the mob's feet, aimed through its middle. */
	private static void shoot(GameTestHelper helper, LivingEntity target, double height) {
		ServerLevel level = helper.getLevel();
		Arrow arrow = new Arrow(level, target.getX() - 2.5, target.getY() + height, target.getZ(), new ItemStack(Items.ARROW), null);
		arrow.setDeltaMovement(2.0, 0.0, 0.0);
		arrow.setCritArrow(false);
		level.addFreshEntity(arrow);
	}

	private static Mob target(GameTestHelper helper, net.minecraft.world.entity.EntityType<? extends Mob> type, BlockPos pos) {
		return helmeted(helper.spawnWithNoFreeWill(type, pos));
	}

	/** A mob with its own AI. */
	private static Mob walker(GameTestHelper helper, net.minecraft.world.entity.EntityType<? extends Mob> type, BlockPos pos) {
		return helmeted(helper.spawn(type, pos));
	}

	private static Mob helmeted(Mob mob) {
		mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // same (tiny) armor on every target, no burning
		return mob;
	}

	@GameTest(maxTicks = 40)
	public void skeletonsShrugOffArrows(GameTestHelper helper) {
		Mob ours = target(helper, ModEntities.woken(UndeadVariant.SKELETON), new BlockPos(4, 1, 2));
		Mob vanilla = target(helper, EntityTypes.SKELETON, new BlockPos(4, 1, 5));
		float oursBefore = ours.getHealth();
		float vanillaBefore = vanilla.getHealth();
		shoot(helper, ours, 1.0);
		shoot(helper, vanilla, 1.0);
		helper.runAfterDelay(10, () -> {
			float oursLost = oursBefore - ours.getHealth();
			float vanillaLost = vanillaBefore - vanilla.getHealth();
			helper.assertTrue(vanillaLost > 0.0F, "the arrow missed the vanilla skeleton");
			helper.assertValueInBetween(UndeadTuning.SKELETON_ARROW_DAMAGE_FACTOR - 0.02F, oursLost / vanillaLost,
					UndeadTuning.SKELETON_ARROW_DAMAGE_FACTOR + 0.02F, "arrow damage ratio (ours " + oursLost + ", vanilla " + vanillaLost + ")");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void zombieHeadshotsHurtMore(GameTestHelper helper) {
		Mob head = target(helper, ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(4, 1, 2));
		Mob body = target(helper, ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(4, 1, 5));
		float before = head.getHealth();
		shoot(helper, head, 1.75); // eye height 1.74: the head cube
		shoot(helper, body, 0.9); // the chest
		helper.runAfterDelay(10, () -> {
			float headLost = before - head.getHealth();
			float bodyLost = before - body.getHealth();
			helper.assertTrue(bodyLost > 0.0F, "the body shot missed");
			helper.assertValueInBetween(UndeadTuning.ZOMBIE_HEADSHOT_FACTOR - 0.1F, headLost / bodyLost,
					UndeadTuning.ZOMBIE_HEADSHOT_FACTOR + 0.1F, "headshot / body shot damage (head " + headLost + ", body " + bodyLost + ")");
			helper.succeed();
		});
	}

	// --- drifting into groups ---------------------------------------------------------------------

	/** An idle zombie 16 blocks from another walks over to it (each test in its own batch: the lane is 20 long). */
	@GameTest(environment = "bubbas_reworked_mobs-test:drift_idle", structure = LANE, maxTicks = 800)
	public void idleZombiesDriftTogether(GameTestHelper helper) {
		Mob walker = walker(helper, ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(1, 1, 2));
		Mob group = target(helper, ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(17, 1, 2));
		helper.succeedWhen(() -> helper.assertTrue(walker.distanceTo(group) < UndeadTuning.DRIFT_STOP_DISTANCE + 2.0,
				"still " + walker.distanceTo(group) + " blocks apart"));
	}

	/** A zombie with a target goes for it and doesn't drift toward the others. */
	@GameTest(environment = "bubbas_reworked_mobs-test:drift_target", structure = LANE, maxTicks = 500)
	public void zombieWithTargetDoesNotDrift(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
		pig.setHealth(1000.0F);
		Mob hunter = walker(helper, ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(8, 1, 2));
		hunter.setTarget(pig);
		Mob group = target(helper, ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(17, 1, 2));
		double start = hunter.distanceTo(group);
		helper.succeedWhen(() -> {
			helper.assertTrue(hunter.distanceTo(pig) < 3.0F, "never reached its target");
			helper.assertTrue(hunter.getTarget() == pig, "lost its target: " + hunter.getTarget());
			helper.assertTrue(hunter.distanceTo(group) > start, "drifted toward the group while hunting (" + hunter.distanceTo(group) + " < " + start + ")");
		});
	}
}
