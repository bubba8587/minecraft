// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestException;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.phys.AABB;

/**
 * When a test fails, logs what was around it (specs/quality.md "Test isolation"): every entity within 40 blocks
 * (inside or outside the test area, position relative to the area's corner, and for mobs their target, running goals,
 * hand and path state), every block entity within 10 blocks, whether it is night and how many players are logged in.
 * One line per failure, "FAILURE CONTEXT" in the game test log. That is how the flaky tests of 2026-11-16 were run
 * down (a skeleton fleeing a sunrise, a drowned stuck under a ledge).
 */
@Mixin(GameTestInfo.class)
abstract class FailureContextMixin {
	@Unique
	private boolean brmTest$logged;

	@Inject(method = "fail(Lnet/minecraft/gametest/framework/GameTestException;)V", at = @At("HEAD"))
	private void brmTest$logContext(GameTestException error, CallbackInfo ci) {
		if (this.brmTest$logged) {
			return;
		}
		this.brmTest$logged = true;
		GameTestInfo self = (GameTestInfo) (Object) this;
		try {
			AABB box = self.getStructureBounds();
			String entities = self.getLevel().getEntitiesOfClass(Entity.class, box.inflate(40.0, 10.0, 40.0), e -> true).stream()
					.map(e -> describe(e, box)).collect(Collectors.joining(", "));
			StringBuilder blockEntities = new StringBuilder();
			BlockPos.betweenClosed((int) box.minX - 10, (int) box.minY - 1, (int) box.minZ - 10, (int) box.maxX + 10, (int) box.maxY + 1, (int) box.maxZ + 10)
					.forEach(p -> {
						BlockEntity be = self.getLevel().getBlockEntity(p);
						if (be != null) {
							blockEntities.append(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getPath())
									.append(String.format("(%d,%d,%d) ", (int) (p.getX() - box.minX), (int) (p.getY() - box.minY), (int) (p.getZ() - box.minZ)));
						}
					});
			System.out.println("FAILURE CONTEXT " + self.id() + " tick " + self.getTick() + " night " + self.getLevel().isDarkOutside() + " players "
					+ self.getLevel().getServer().getPlayerList().getPlayerCount() + " | entities: " + entities + " | block entities: " + blockEntities);
		} catch (RuntimeException e) {
			System.out.println("FAILURE CONTEXT " + self.id() + " unavailable: " + e);
		}
	}

	@Unique
	private static String describe(Entity e, AABB box) {
		String where = (box.inflate(1.0).contains(e.position()) ? "in" : "OUTSIDE")
				+ String.format("(%.1f,%.1f,%.1f)", e.getX() - box.minX, e.getY() - box.minY, e.getZ() - box.minZ);
		if (!(e instanceof Mob m)) {
			return e.getType().toShortString() + "@" + where;
		}
		return e.getType().toShortString() + "@" + where + (m.getTarget() != null ? " target=" + m.getTarget().getType().toShortString() : "")
				+ " goals=" + ((MobAccessor) m).brmTest$goals().getAvailableGoals().stream().filter(g -> g.isRunning())
						.map(g -> g.getGoal().getClass().getSimpleName()).toList()
				+ " hand=" + BuiltInRegistries.ITEM.getKey(m.getMainHandItem().getItem()).getPath() + " navDone=" + m.getNavigation().isDone()
				+ " ground=" + m.onGround() + " age=" + m.tickCount;
	}
}
