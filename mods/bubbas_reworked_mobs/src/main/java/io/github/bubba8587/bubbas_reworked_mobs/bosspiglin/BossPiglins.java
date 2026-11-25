// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** The Boss Piglin (specs/boss-piglin.md): its entity type, and seating one on an outpost's throne. */
public final class BossPiglins {
	public static final EntityType<BossPiglin> BOSS_PIGLIN = register("boss_piglin",
			EntityType.Builder.of(BossPiglin::new, MobCategory.MONSTER).sized(1.8F, 2.6F).eyeHeight(2.2F).passengerAttachments(2.65F)
					.ridingOffset(-0.7F).clientTrackingRange(8));

	private BossPiglins() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(BOSS_PIGLIN, BossPiglin.createAttributes());
	}

	/**
	 * With an outpost's roster: its boss seated on the throne. A boss of this outpost still alive is sat back down; else
	 * a new one is made while the throne still stands. Not on the roster.
	 */
	public static @Nullable BossPiglin seat(ServerLevel level, CampSite site) {
		CampSite.Throne throne = site.throne();
		if (throne == null) {
			return null;
		}
		Vec3 seat = throne.seat();
		float yaw = throne.facing().toYRot();
		List<BossPiglin> alive = BossPiglin.of(level, site);
		if (!alive.isEmpty()) {
			BossPiglin boss = alive.getFirst();
			boss.seatAt(seat, yaw);
			return boss;
		}
		if (!stands(level, throne)) {
			return null;
		}
		BossPiglin boss = BOSS_PIGLIN.create(level, EntitySpawnReason.STRUCTURE);
		if (boss == null) {
			return null;
		}
		boss.snapTo(seat.x, seat.y, seat.z, yaw, 0.0F);
		boss.setYHeadRot(yaw);
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(seat)), EntitySpawnReason.STRUCTURE, null);
		if (level.environmentAttributes().getValue(EnvironmentAttributes.PIGLINS_ZOMBIFY, seat)) {
			boss.setImmuneToZombification(true); // an outpost built outside the Nether (the command, tests)
		}
		boss.link(site.id(), throne.portal().orElse(null));
		boss.setHomeTo(site.centre(), site.size() == null ? 12 : site.size().radius + BossPiglinTuning.HOME_MARGIN);
		boss.seatAt(seat, yaw);
		return level.addFreshEntity(boss) ? boss : null;
	}

	/** Whether at least half the throne's blocks are still there as built. */
	public static boolean stands(ServerLevel level, CampSite.Throne throne) {
		List<ThroneDesign.Placed> blocks = ThroneDesign.blocks(throne.facing());
		int there = 0;
		for (ThroneDesign.Placed placed : blocks) {
			if (level.getBlockState(throne.anchor().offset(placed.offset())).is(placed.state().getBlock())) {
				there++;
			}
		}
		return there * 2 >= blocks.size();
	}

	/** The throne's purple cushions (vanilla cushion entities) on its seat; {@code inside} limits them to cells being built. */
	public static void placeCushions(net.minecraft.world.level.ServerLevelAccessor level, BlockPos anchor, net.minecraft.core.Direction facing,
			java.util.function.Predicate<BlockPos> inside) {
		for (net.minecraft.world.phys.Vec3 at : ThroneDesign.cushions(facing)) {
			net.minecraft.world.phys.Vec3 pos = net.minecraft.world.phys.Vec3.atLowerCornerOf(anchor).add(at);
			if (!inside.test(BlockPos.containing(pos))) {
				continue;
			}
			net.minecraft.world.entity.decoration.Cushion cushion = net.minecraft.world.entity.EntityTypes.CUSHION.create(level.getLevel(),
					net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
			if (cushion != null) {
				cushion.setColor(net.minecraft.world.item.DyeColor.PURPLE);
				cushion.setPos(pos.x, pos.y, pos.z);
				level.addFreshEntity(cushion);
			}
		}
	}
}
