// dte:C6,B19
package io.github.bubba8587.bubbas_totems_artifacts.northwind;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Totem of the North Wind: the gust (a firework rocket boost) and the beacon and statue areas. */
public final class NorthWind {
	private NorthWind() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(NorthWind::tick);
	}

	/** The cooldown stack: the totem's own cooldown group, shared by hand use and areas. */
	private static ItemStack cooldownKey() {
		return new ItemStack(ModItems.TOTEM_OF_NORTH_WIND);
	}

	/** Whether the player's North Wind cooldown is running. */
	public static boolean onCooldown(Player player) {
		return player.getCooldowns().isOnCooldown(cooldownKey());
	}

	/** One gust: vanilla's rocket boost (an attached, silent, explosion-free rocket), with wind sound and snow. */
	public static void boost(ServerLevel level, Player player) {
		if (player.dropAllLeashConnections(null)) {
			level.playSound(null, player, SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
		rocket.set(DataComponents.FIREWORKS, new Fireworks(TotemsTuning.NORTH_WIND_FLIGHT, List.of()));
		FireworkRocketEntity entity = new FireworkRocketEntity(level, rocket, player);
		entity.setSilent(true);
		Projectile.spawnProjectile(entity, level, rocket);
		// gliding in water: water movement smothers a rocket's push, so the gust throws the player out along their look
		if (player.isInWater()) {
			player.setDeltaMovement(player.getLookAngle().scale(TotemsTuning.NORTH_WIND_WATER_LAUNCH).add(0.0, TotemsTuning.NORTH_WIND_WATER_LIFT, 0.0));
			player.needsSync = true;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST.value(),SoundSource.PLAYERS, 0.6F, 1.3F);
		level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.5, player.getZ(), 16, 0.4, 0.3, 0.4, 0.05);
		level.sendParticles(ParticleTypes.SMALL_GUST, player.getX(), player.getY() + 0.5, player.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
	}

	/** Beacon and statue areas: each gliding player inside, off cooldown, gets a gust and the cooldown. */
	private static void tick(ServerLevel level) {
		List<TotemAreas.Area> areas = TotemAreas.all(level, TotemAreas.Kind.NORTH_WIND);
		if (areas.isEmpty()) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!player.isFallFlying() || player.isSpectator() || onCooldown(player)) {
				continue;
			}
			for (TotemAreas.Area area : areas) {
				if (area.box().contains(player.position())) {
					boost(level, player);
					player.getCooldowns().addCooldown(cooldownKey(), TotemsTuning.NORTH_WIND_AREA_COOLDOWN_TICKS);
					break;
				}
			}
		}
	}
}
