// dte:C8,B18
package io.github.bubba8587.bubbas_world_tweaks.lava;

import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.phys.Vec3;

/** Geysers lift. */
public final class GeyserLift {
	private GeyserLift() {
	}

	public static void apply(Player player) {
		if (player.isSpectator() || player.getAbilities().flying || player.isPassenger()) {
			return;
		}
		Level level = player.level();
		boolean lava = !level.getEntitiesOfClass(LavaGeyser.class, player.getBoundingBox().inflate(WorldTweaksTuning.GEYSER_RADIUS, 0.0,
				WorldTweaksTuning.GEYSER_RADIUS).expandTowards(0.0, -WorldTweaksTuning.GEYSER_HEIGHT, 0.0),
				g -> g.column().intersects(player.getBoundingBox())).isEmpty();
		boolean sulfur = !lava && overEruptingSulfur(level, player);
		if (!lava && !sulfur) {
			return;
		}
		Vec3 v = player.getDeltaMovement();
		double vy = v.y;
		if (lava && vy < WorldTweaksTuning.LIFT_MAX_RISE) {
			vy += WorldTweaksTuning.LIFT_PUSH;
			player.resetFallDistance();
		}
		double h = Math.sqrt(v.x * v.x + v.z * v.z);
		double vx = v.x;
		double vz = v.z;
		if (h > 1.0E-3 && h < WorldTweaksTuning.BOOST_MAX) {
			double scale = Math.min(WorldTweaksTuning.BOOST_FACTOR, WorldTweaksTuning.BOOST_MAX / h);
			vx *= scale;
			vz *= scale;
		}
		player.setDeltaMovement(vx, vy, vz);
	}

	/** An erupting potent sulfur block somewhere below the player's feet, within SULFUR_SEARCH_DEPTH. */
	private static boolean overEruptingSulfur(Level level, Player player) {
		BlockPos.MutableBlockPos p = player.blockPosition().mutable();
		for (int i = 0; i < WorldTweaksTuning.SULFUR_SEARCH_DEPTH; i++) {
			BlockState state = level.getBlockState(p);
			if (state.is(Blocks.POTENT_SULFUR)) {
				return state.getValue(PotentSulfurBlock.STATE) == PotentSulfurState.ERUPTING;
			}
			p.move(0, -1, 0);
		}
		return false;
	}
}
