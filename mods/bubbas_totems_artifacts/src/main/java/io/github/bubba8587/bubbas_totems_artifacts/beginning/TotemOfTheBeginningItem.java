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

/**
 * Totem of the Beginning (specs/totems-artifacts.md). Used (once: it is used up), the ground round you ripples back,
 * outward, to how it generated: holes, craters and strip mines fill, piles go, the top layer gets its own material
 * back; built things and the ground round them are left alone. That's Reworked Mobs' endermen's target world (its
 * api/Endermen, through compat/SleeperHook): without Reworked Mobs, or where there is no generated terrain to go back
 * to (the Nether, the End), using it does nothing and it isn't used up. In a beacon (compat/BeginningBeaconPayload)
 * the area draws endermen that tidy it. Found only as a cairn's offering.
 */
public class TotemOfTheBeginningItem extends io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem {
	public TotemOfTheBeginningItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel server)) {
			// The client can only guess; the server decides (and says nothing either way if it can't).
			return SleeperHook.get() != SleeperHook.NONE && level.dimension() == Level.OVERWORLD ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		if (!SleeperHook.get().restoreAround(server, player.blockPosition(), TotemsTuning.BEGINNING_RADIUS, TotemsTuning.BEGINNING_TICKS_PER_BLOCK)) {
			return InteractionResult.PASS;
		}
		server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 1.0, 0.6, 0.1);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
		player.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
