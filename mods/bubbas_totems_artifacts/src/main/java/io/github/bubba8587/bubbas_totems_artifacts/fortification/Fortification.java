// dte:C6,B17,B18
package io.github.bubba8587.bubbas_totems_artifacts.fortification;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Totem of Fortification (specs/totems-artifacts.md): explosions break no blocks within
 * {@link TotemsTuning#FORTIFY_RADIUS} of anyone holding one (either hand, players and mobs alike, B18), nor anywhere
 * in a Fortification beacon's area ({@link TotemAreas.Kind#FORTIFICATION}). Only explosion block damage: the explosion
 * still bangs, hurts and knocks back as usual, blocks outside are broken as usual, and the mobGriefing rule is never
 * touched (owner: that would also stop villager farming). Explosion-lit fire doesn't start on protected blocks either.
 * Wind charges (which only press buttons and open doors) are left alone.
 *
 * <p>How: {@code ServerExplosionMixin} hands the explosion's list of blocks to {@link #protect} before anything uses
 * it. One entity box query per explosion, and nothing at all when no holder or area is near.
 */
public final class Fortification {
	private Fortification() {
	}

	/** {@code blocks} without the protected ones (a new list if any were taken out). */
	public static List<BlockPos> protect(ServerLevel level, Vec3 center, float radius, Explosion.BlockInteraction interaction, List<BlockPos> blocks) {
		if (blocks.isEmpty() || interaction == Explosion.BlockInteraction.TRIGGER_BLOCK) {
			return blocks;
		}
		List<TotemAreas.Area> areas = TotemAreas.all(level, TotemAreas.Kind.FORTIFICATION);
		// A block an explosion breaks is at most about 1.75 x its radius from the centre (vanilla's ray steps).
		double reach = radius * 2.0 + TotemsTuning.FORTIFY_RADIUS + 1.0;
		List<LivingEntity> holders = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(reach),
				e -> e.isAlive() && !e.isSpectator() && e.isHolding(ModItems.TOTEM_OF_FORTIFICATION));
		if (holders.isEmpty() && areas.isEmpty()) {
			return blocks;
		}
		double r2 = TotemsTuning.FORTIFY_RADIUS * TotemsTuning.FORTIFY_RADIUS;
		List<BlockPos> kept = new ArrayList<>(blocks.size());
		int saved = 0;
		for (BlockPos pos : blocks) {
			if (isProtected(pos, holders, areas, r2)) {
				saved++;
			} else {
				kept.add(pos);
			}
		}
		if (saved == 0) {
			return blocks;
		}
		// A dull clang and a glint of wax where the blast was turned aside.
		level.playSound(null, center.x, center.y, center.z, SoundEvents.SHIELD_BLOCK.value(), SoundSource.BLOCKS, 1.0F, 0.7F);
		level.sendParticles(ParticleTypes.WAX_ON, center.x, center.y, center.z, Math.min(40, 8 + saved / 4), radius * 0.6, radius * 0.6, radius * 0.6, 0.5);
		return kept;
	}

	private static boolean isProtected(BlockPos pos, List<LivingEntity> holders, List<TotemAreas.Area> areas, double r2) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		for (LivingEntity holder : holders) {
			if (holder.distanceToSqr(x, y, z) <= r2) {
				return true;
			}
		}
		for (TotemAreas.Area area : areas) {
			if (area.box().contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}
}
