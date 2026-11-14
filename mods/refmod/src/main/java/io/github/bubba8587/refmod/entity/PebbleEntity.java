// dte:A2,A4
package io.github.bubba8587.refmod.entity;

import io.github.bubba8587.refmod.registry.ModEntities;
import io.github.bubba8587.refmod.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Thrown pebble: 1 damage on hit, then disappears. */
public class PebbleEntity extends ThrowableItemProjectile {
	public static final float DAMAGE = 1.0F;

	public PebbleEntity(EntityType<? extends PebbleEntity> type, Level level) {
		super(type, level);
	}

	public PebbleEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(ModEntities.PEBBLE, owner, level, stack);
	}

	public PebbleEntity(Level level, double x, double y, double z, ItemStack stack) {
		super(ModEntities.PEBBLE, x, y, z, level, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.PEBBLE;
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (this.level() instanceof ServerLevel serverLevel) {
			hitResult.getEntity().hurtServer(serverLevel, this.damageSources().thrown(this, this.getOwner()), DAMAGE);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide()) {
			this.discard();
		}
	}
}
