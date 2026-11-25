// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

/**
 * The Captain's Helm's soul fire immunity. Vanilla's fire damage carries no block: soul fire and fire both hurt with
 * {@code in_fire}, soul and plain campfires with {@code campfire}, and the burning afterwards with {@code on_fire}. So
 * the wearer's {@code in_fire}/{@code campfire} damage is cancelled when the blocks they touch hold soul fire or a lit
 * soul campfire and no fire, lit campfire or lava; that also puts out the flames soul fire lit on them. A wearer
 * whose last fire was soul fire is "soul-lit": their {@code on_fire} damage is cancelled too, until they touch
 * ordinary fire or lava. A Soulfire Charge doesn't set a wearer alight at all (SoulfireCharge).
 */
public final class CaptainsHelm {
	private static final Set<LivingEntity> SOUL_LIT = Collections.newSetFromMap(new WeakHashMap<>());

	private CaptainsHelm() {
	}

	public static boolean isWornBy(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CAPTAINS_HELM);
	}

	static void initialize() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(CaptainsHelm::allowDamage);
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		boolean touchFire = source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.CAMPFIRE);
		boolean burning = source.is(DamageTypes.ON_FIRE);
		if (!touchFire && !burning && !source.is(DamageTypes.LAVA)) {
			return true;
		}
		if (!isWornBy(entity)) {
			SOUL_LIT.remove(entity);
			return true;
		}
		Touching touching = touching(entity);
		if (touching == Touching.ORDINARY || source.is(DamageTypes.LAVA)) {
			SOUL_LIT.remove(entity);
			return true;
		}
		if (touching == Touching.SOUL) {
			SOUL_LIT.add(entity);
			entity.clearFire();
			return false;
		}
		if (burning && SOUL_LIT.contains(entity)) {
			entity.clearFire();
			return false;
		}
		return true;
	}

	private enum Touching {
		NONE, SOUL, ORDINARY
	}

	/** The fire blocks inside the entity's box: any ordinary fire or lava wins over soul fire. */
	private static Touching touching(LivingEntity entity) {
		Level level = entity.level();
		AABB box = entity.getBoundingBox().deflate(1.0E-5);
		Touching found = Touching.NONE;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.SOUL_FIRE) || state.is(Blocks.SOUL_CAMPFIRE) && state.getValue(CampfireBlock.LIT)) {
				found = Touching.SOUL;
			} else if (state.is(Blocks.FIRE) || state.is(Blocks.CAMPFIRE) && state.getValue(CampfireBlock.LIT) || state.getFluidState().is(FluidTags.LAVA)) {
				return Touching.ORDINARY;
			}
		}
		return found;
	}
}
