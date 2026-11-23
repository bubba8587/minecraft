// dte:C6,B18,B19
package io.github.bubba8587.bubbas_totems_artifacts.end;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Totem of the End (specs/totems-artifacts.md). Held in either hand ({@link #held}), endermen treat you as one of
 * their own: eye contact doesn't anger them (mixin/EndermanKinMixin, vanilla endermen too), and with Reworked Mobs
 * breaking or fighting near a cairn doesn't turn them on you (through compat/SleeperHook). Used, it teleports you to
 * the block you aim at ({@link EndTeleport}); reusable, with the item cooldown sweep after each teleport (B19). Aiming
 * at nothing, or at a spot with nowhere safe to land, does nothing and starts no cooldown.
 */
public class TotemOfTheEndItem extends io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem {
	public TotemOfTheEndItem(Properties properties) {
		super(properties);
	}

	/** Whether {@code entity} holds a Totem of the End in either hand ("held" as in totems/Held: players and mobs alike). */
	public static boolean held(LivingEntity entity) {
		return entity.getMainHandItem().is(ModItems.TOTEM_OF_THE_END) || entity.getOffhandItem().is(ModItems.TOTEM_OF_THE_END);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Vec3 to = EndTeleport.landing(level, player, EndTeleport.aim(level, player));
		if (to == null) {
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel server) {
			EndTeleport.teleport(server, player, to);
			player.getCooldowns().addCooldown(stack, TotemsTuning.END_COOLDOWN_TICKS);
		}
		return InteractionResult.SUCCESS;
	}
}
