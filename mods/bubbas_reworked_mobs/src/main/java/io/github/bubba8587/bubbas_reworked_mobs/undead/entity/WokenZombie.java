// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import io.github.bubba8587.bubbas_reworked_mobs.loot.GrabLootGoal;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadCombat;
import io.github.bubba8587.bubbas_reworked_mobs.undead.ZombieGait;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** A zombie that woke from a sleeper block (see {@link WokenSkeleton}). */
public class WokenZombie extends Zombie {
	/** Client only: eases to 1 while the zombie is moving and to 0 once it stands (drives how much it shambles). */
	private float gaitWalk;
	private float gaitWalkO;

	public WokenZombie(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
	}

	/** Plus grabbing what's lying around when idle and empty-handed, and drifting into groups (below the attack goals, above strolling). */
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.BackToSleep(this));
		super.registerGoals();
		this.goalSelector.addGoal(5, new GrabLootGoal(this, 1.0));
		this.goalSelector.addGoal(6, new DriftToHordeGoal(this, 1.0));
	}

	/**
	 * The shamble's real motion (ZombieGait): the move control sets the speed every tick it walks, and we pulse it
	 * (slow drag, quick lurch, same average). Babies and swimmers move normally.
	 */
	@Override
	public void setSpeed(float speed) {
		if (!this.isBaby() && !this.isInWater() && !this.level().isClientSide()) {
			speed *= ZombieGait.moveSpeedFactor(this.getId(), this.level().getGameTime());
		}
		super.setSpeed(speed);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.BackToSleep.noteNight(level, this);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			// Even the slow drag moves about 0.01 a tick; standing (idle, or hitting at arm's reach) moves nothing.
			double dx = this.getX() - this.xo;
			double dz = this.getZ() - this.zo;
			boolean moving = dx * dx + dz * dz > 0.003 * 0.003;
			this.gaitWalkO = this.gaitWalk;
			this.gaitWalk += ((moving ? 1.0F : 0.0F) - this.gaitWalk) * (moving ? 0.25F : 0.1F);
		}
	}

	public float gaitWalk(float partialTick) {
		return this.gaitWalkO + (this.gaitWalk - this.gaitWalkO) * partialTick;
	}

	/** Headshots with arrows hurt more (UndeadCombat). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return super.hurtServer(level, source, UndeadCombat.zombieDamage(this, source, damage));
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}

	/** Stays our type: vanilla would turn it into a vanilla drowned underwater. */
	@Override
	protected boolean convertsInWater() {
		return false;
	}

	/** Stays our type: 26.4 would turn it into a vanilla frostbite in powder snow. */
	@Override
	protected boolean convertsWhenFreezing() {
		return false;
	}
}
