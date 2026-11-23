// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import io.github.bubba8587.bubbas_reworked_mobs.loot.GrabLootGoal;
import io.github.bubba8587.bubbas_reworked_mobs.loot.LootGrabbing;
import net.minecraft.world.item.ItemStack;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

/**
 * A skeleton that woke from a sleeper block. Its own entity type (tagged as a skeleton and undead) so tuning and
 * later behaviour stay scoped to this mod's undead; rendered with the vanilla skeleton renderer.
 */
public class WokenSkeleton extends Skeleton implements LootGrabbing.DeferredHold {
	/** Loot grabbed during a goal tick, equipped at the start of the next tick (see LootGrabbing.hold). */
	private ItemStack pendingHold = ItemStack.EMPTY;

	public WokenSkeleton(EntityType<? extends Skeleton> type, Level level) {
		super(type, level);
	}

	@Override
	public void bubbas$holdNextTick(ItemStack stack) {
		this.pendingHold = stack;
	}

	private void equipPending() {
		if (!this.pendingHold.isEmpty()) {
			ItemStack stack = this.pendingHold;
			this.pendingHold = ItemStack.EMPTY;
			LootGrabbing.equip(this, stack);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.BackToSleep.noteNight(level, this); // for going back to sleep at dawn
	}

	@Override
	public void tick() {
		this.equipPending(); // before AI runs, so the weapon goal swap never happens mid goal-tick
		super.tick();
	}

	/** A grabbed item never vanishes: if it dies before its next tick, it holds (and drops) the item. */
	@Override
	public void die(DamageSource source) {
		this.equipPending();
		super.die(source);
	}

	/** Gear comes from the sleeper, never from the vanilla spawn roll (a skeleton would get a bow). */
	/** Plus grabbing what's lying around when idle and empty-handed (below the attack goals, above strolling). */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.BackToSleep(this)); // back to sleep at dawn
		super.registerGoals();
		this.goalSelector.addGoal(5, new GrabLootGoal(this, 1.0));
	}

	/** Arrows mostly pass between the bones (UndeadCombat). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return super.hurtServer(level, source, UndeadCombat.skeletonDamage(source, damage));
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}
}
