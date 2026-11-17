// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Test isolation (specs/quality.md "Test isolation"): on the game test server the clock and the weather stand still,
 * so the time of day is whatever a test's environment sets (night environments: 18000), else the world's start
 * (early morning, clear), and never drifts.
 * <p>
 * The clock_time environment only sets the time when its batch starts; a test then waits for its chunks to load,
 * and the test server ticks as fast as it can meanwhile. 12000 ticks went by once: a "night" test began at
 * sunrise and its skeleton fled the sun instead of walking to the sword. Rain came and went on its own as well.
 */
@Mixin(GameTestServer.class)
abstract class FrozenClockMixin {
	@Inject(method = "startTests", at = @At("HEAD"))
	private void brmTest$freezeClock(ServerLevel level, CallbackInfo ci) {
		MinecraftServer server = level.getServer();
		server.getGlobalGameRules().set(GameRules.ADVANCE_TIME, false, server);
		server.getGlobalGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
		server.setWeatherParameters(12000, 0, false, false);
	}
}
