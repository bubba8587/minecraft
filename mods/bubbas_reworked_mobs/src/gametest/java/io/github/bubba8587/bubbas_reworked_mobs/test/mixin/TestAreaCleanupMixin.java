// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import java.util.List;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test isolation (specs/quality.md "Test isolation"): once a test is done, pass or fail, its area is wiped (blocks,
 * scheduled ticks, entities) and the mock players it made are logged out.
 * <p>
 * The vanilla game test server never clears a finished test: batches run one after another along a grid 5 blocks
 * apart, and on a pass it only discards the entities inside the area (players excepted, and nothing at all on a
 * fail). So sleepers, loot displays, dropped items and barrier-free areas of earlier tests sat right next to later
 * ones: a leftover sleeper 5 blocks away heard a later test and woke, a later mob walked off for a leftover display
 * (notice range 8), a mock player left by a failed test drew natural spawns and targets. Each test now starts next
 * to empty air.
 */
@Mixin(GameTestInfo.class)
abstract class TestAreaCleanupMixin {
	@Unique
	private boolean brmTest$cleaned;

	@Inject(method = "tick", at = @At("RETURN"))
	private void brmTest$cleanWhenDone(GameTestRunner runner, CallbackInfo ci) {
		GameTestInfo self = (GameTestInfo) (Object) this;
		if (this.brmTest$cleaned || !self.isDone() || self.getTestBlockPos() == null) {
			return;
		}
		this.brmTest$cleaned = true;
		ServerLevel level = self.getLevel();
		BoundingBox box = self.getTestInstanceBlockEntity().getTestBoundingBox();
		AABB area = AABB.of(box).inflate(2.0);
		List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers().stream()
				.filter(p -> p.getGameProfile().name().startsWith("test-") && area.contains(p.position())).toList();
		players.forEach(p -> level.getServer().getPlayerList().remove(p));
		StructureUtils.clearSpaceForStructure(box, level);
	}
}
