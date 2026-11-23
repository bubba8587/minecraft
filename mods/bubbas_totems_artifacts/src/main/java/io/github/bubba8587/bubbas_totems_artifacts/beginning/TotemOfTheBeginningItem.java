// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.beginning;

import io.github.bubba8587.bubbas_totems_artifacts.compat.SleeperHook;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Totem of the Beginning: right-click pulse through compat/SleeperHook (specs/totems-artifacts.md). */
public class TotemOfTheBeginningItem extends io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem {
	public TotemOfTheBeginningItem(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
			java.util.function.Consumer<net.minecraft.network.chat.Component> builder, net.minecraft.world.item.TooltipFlag flag) {
		builder.accept(net.minecraft.network.chat.Component.translatable("item.bubbas_totems_artifacts.totem_of_the_beginning.use")
				.withStyle(net.minecraft.ChatFormatting.GRAY));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server)) {
			// The client can only guess; the server decides (and says nothing either way if it can't).
			return SleeperHook.get() != SleeperHook.NONE && level.dimension() == Level.OVERWORLD ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		if (!SleeperHook.get().pulseAround(server, player.blockPosition(), TotemsTuning.BEGINNING_PULSE_RADIUS,
				TotemsTuning.BEGINNING_PULSE_TICKS_PER_BLOCK, TotemsTuning.BEGINNING_PULSE_CHANCE, TotemsTuning.BEGINNING_PULSE_ORE_CHANCE,
				TotemsTuning.BEGINNING_PULSE_RECENT_TICKS, player)) {
			return InteractionResult.PASS;
		}
		server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 1.0, 0.6, 0.1);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 0.8F, 0.5F);
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(stack, TotemsTuning.BEGINNING_PULSE_COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}
}
