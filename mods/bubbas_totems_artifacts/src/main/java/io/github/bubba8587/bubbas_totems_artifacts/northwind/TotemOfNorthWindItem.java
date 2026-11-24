// dte:C6,B19
package io.github.bubba8587.bubbas_totems_artifacts.northwind;

import java.util.function.Consumer;

import io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Totem of the North Wind (northwind/NorthWind). */
public class TotemOfNorthWindItem extends StatueTotemItem {
	public TotemOfNorthWindItem(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("item.bubbas_totems_artifacts.totem_of_north_wind.use").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isFallFlying()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server) {
			ItemStack stack = player.getItemInHand(hand);
			NorthWind.boost(server, player);
			player.getCooldowns().addCooldown(stack, TotemsTuning.NORTH_WIND_COOLDOWN_TICKS);
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		return InteractionResult.SUCCESS;
	}
}
