// dte:C1,B15,B17
package io.github.bubba8587.bubbas_reworked_mobs.soulfire;

import io.github.bubba8587.bubbas_reworked_mobs.captain.Captains;
import io.github.bubba8587.bubbas_reworked_mobs.captain.SoulfireCharge;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Soul Fire Charge: used on a block it lights fire as a fire charge does (vanilla {@link FireChargeItem}: soul fire on
 * soul sand and soul soil); used in the air, or fired from a dispenser, it flies as a Soulfire Charge.
 */
public class SoulFireChargeItem extends FireChargeItem {
	public SoulFireChargeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			Vec3 look = player.getLookAngle();
			SoulfireCharge charge = SoulfireCharge.shoot(level, player, look);
			charge.setPos(player.getX() + look.x * 0.8, player.getEyeY() - 0.2 + look.y * 0.8, player.getZ() + look.z * 0.8);
			charge.setItem(stack.copyWithCount(1));
			level.addFreshEntity(charge);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6F,
				(level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F + 0.8F);
		player.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	public Projectile asProjectile(Level level, Position position, ItemStack itemStack, Direction direction) {
		RandomSource random = level.getRandom();
		Vec3 dir = new Vec3(random.triangle(direction.getStepX(), 0.11485), random.triangle(direction.getStepY(), 0.11485),
				random.triangle(direction.getStepZ(), 0.11485)).normalize();
		SoulfireCharge charge = new SoulfireCharge(Captains.SOULFIRE_CHARGE, level);
		charge.snapTo(position.x(), position.y(), position.z(), charge.getYRot(), charge.getXRot());
		charge.setDeltaMovement(dir.scale(charge.accelerationPower));
		charge.setItem(itemStack);
		return charge;
	}
}
