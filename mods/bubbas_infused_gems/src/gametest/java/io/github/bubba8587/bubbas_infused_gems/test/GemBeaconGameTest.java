// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Gems in Bubba's Reworked Beacons (owner, 2026-11-16): a gem beacon's pulse gives a pig and a player in range the gem's
 * worn effect at BEACON_SHARE of a full netherite set, its Gem Power capped by the pyramid. Without Reworked Beacons
 * (the default test run) it passes at once; run with -PwithBeacons -PmilestoneTests to test it for real.
 */
public class GemBeaconGameTest {
	@GameTest(maxTicks = 40)
	public void gemBeaconReachesMobsAndPlayers(GameTestHelper helper) {
		if (!FabricLoader.getInstance().isModLoaded("bubbas_reworked_beacons")) {
			helper.succeed();
			return;
		}
		check(helper, new io.github.bubba8587.bubbas_infused_gems.compat.GemBeaconPayload().replacesStandalonePayments(),
				"Infused Gems must switch the standalone gem-base payments off (owner)");
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 1));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 3.5));
		player.teleportTo(at.x, at.y, at.z);
		BlockPos beacon = helper.absolutePos(new BlockPos(2, 1, 2));
		WithBeacons.pulse(helper, beacon, 4);
		Balance b = Balance.get();
		double full = b.beaconShare() * b.effect("speed").wornCap();
		check(helper, Math.abs(WornEffects.of(pig).value("speed") - full) < 1e-6, "pig speed from a perfect gem beacon: " + WornEffects.of(pig).value("speed") + ", expected " + full);
		check(helper, Math.abs(WornEffects.of(player).value("speed") - full) < 1e-6, "player speed from a perfect gem beacon: " + WornEffects.of(player).value("speed"));
		check(helper, pig.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
				> pig.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), "the pig isn't faster");
		// A 1-layer pyramid caps the perfect gem at Gem Power I.
		WithBeacons.pulse(helper, beacon, 1);
		double capped = full * b.infusion(1).factor();
		check(helper, Math.abs(WornEffects.of(pig).value("speed") - capped) < 1e-6, "a 1-layer beacon gave " + WornEffects.of(pig).value("speed") + ", expected " + capped);
		pig.discard();
		GemKit.remove(helper, player);
		helper.succeed();
	}

	/** Touches the beacons API; loaded only when that mod is installed. */
	private static final class WithBeacons {
		static void pulse(GameTestHelper helper, BlockPos beacon, int levels) {
			var level = helper.getLevel();
			var stack = GemKit.item(GemKit.gem("speed", 1.0));
			var payload = new io.github.bubba8587.bubbas_infused_gems.compat.GemBeaconPayload();
			var context = new io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext(level, beacon,
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode.SKY, levels, payload.power(stack),
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext.skyArea(level, beacon, levels));
			payload.apply(level, stack, context);
		}
	}
}
