// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.skies;

import io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Totem of Clear Skies. */
public class TotemOfClearSkiesItem extends StatueTotemItem {
	public TotemOfClearSkiesItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isRaining() && !level.isThundering()) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = player.getItemInHand(hand);
		server.getServer().setWeatherParameters(ServerLevel.RAIN_DELAY.sample(server.getRandom()), 0, false, false);
		server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 30, 0.5, 0.8, 0.5, 0.08);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7F, 1.4F);
		player.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
