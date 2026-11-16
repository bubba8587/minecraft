// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.drowned;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.Mth;

/**
 * The drowned's two postures (specs/drowned.md, owner 2026-11-16): upright (lurking, dragging in water: vanilla's
 * standing model) or lying down. Lying is vanilla's synced swimming flag (its swim amount eases in and out); the
 * server sets it. In water it lies flat and swims (vanilla's swim strokes); on land it crawls: a sidewinding,
 * staggered drag, the body snaking side to side and rolling onto each shoulder as one arm pulls, then the other.
 */
public final class DrownedPoses {
	/** The hint shown when a drowned grabs you (instead of vanilla's "Press Shift to dismount"). */
	public static final String GRABBED_HINT = "bubbas_reworked_mobs.drowned.grabbed";
	/** Crawl cycles per block moved (each cycle is two pulls, one per arm). */
	private static final float CRAWL_CYCLES_PER_BLOCK = 0.55F;
	/** Side-to-side yaw of the whole body at full crawl (degrees). */
	private static final float SWAY_YAW = 22.0F;
	/** Roll onto each shoulder (degrees). */
	private static final float SWAY_ROLL = 12.0F;
	/** Sideways shift of the hips with the sway (blocks). */
	private static final float SWAY_SHIFT = 0.12F;

	private DrownedPoses() {
	}

	/** The crawl's phase, 0..1 per cycle: lurching (fast pull, slow drag) rather than an even swing. */
	private static float phase(ZombieRenderState state) {
		float cycle = state.walkAnimationPos * CRAWL_CYCLES_PER_BLOCK;
		float t = cycle - Mth.floor(cycle);
		// Staggered: the first 35 % of the cycle covers half the motion (the pull), the rest the slow drag.
		return t < 0.35F ? t / 0.35F * 0.5F : 0.5F + (t - 0.35F) / 0.65F * 0.5F;
	}

	/** How much it is crawling (0 still, 1 moving). */
	private static float moving(ZombieRenderState state) {
		return Mth.clamp(state.walkAnimationSpeed * 2.5F, 0.0F, 1.0F);
	}

	/** Replaces vanilla's 10-degree swim tilt: flat like a swimming player, with the crawl's sway on land. */
	public static void lieDown(ZombieRenderState state, PoseStack poseStack) {
		float lying = state.swimAmount;
		if (!state.isInWater) {
			float s = Mth.sin(phase(state) * Mth.TWO_PI) * moving(state) * lying;
			poseStack.translate(s * SWAY_SHIFT, 0.0F, 0.0F);
			poseStack.rotateDegrees(Axis.YP, s * SWAY_YAW);
			poseStack.rotateDegrees(Axis.XP, -90.0F * lying);
			poseStack.rotateDegrees(Axis.YP, s * SWAY_ROLL);
		} else {
			poseStack.rotateDegrees(Axis.XP, -85.0F * lying);
		}
		// Vanilla's swimming player offset: the body lies at the bottom of its box.
		poseStack.translate(0.0F, -1.0F * lying, 0.3F * lying);
	}

	/** After vanilla's swim strokes: on land, the arms pull alternately and the legs drag; the head looks ahead. */
	public static void limbs(DrownedModel model, ZombieRenderState state, float swimAmount) {
		if (state.isInWater) {
			model.head.xRot = -0.9F * swimAmount;
			return;
		}
		float p = phase(state) * Mth.TWO_PI;
		float m = moving(state);
		// -PI: reaching straight ahead of the head (the body is face down). One arm reaches while the other pulls back.
		float reach = (float) -Math.PI * 0.92F;
		model.rightArm.xRot = Mth.lerp(swimAmount, model.rightArm.xRot, reach + 0.55F * m * Mth.sin(p));
		model.leftArm.xRot = Mth.lerp(swimAmount, model.leftArm.xRot, reach - 0.55F * m * Mth.sin(p));
		model.rightArm.zRot = Mth.lerp(swimAmount, model.rightArm.zRot, -0.35F - 0.15F * m * Mth.cos(p));
		model.leftArm.zRot = Mth.lerp(swimAmount, model.leftArm.zRot, 0.35F - 0.15F * m * Mth.cos(p));
		// Legs drag, splayed a little, twitching with the pulls.
		model.rightLeg.xRot = Mth.lerp(swimAmount, model.rightLeg.xRot, 0.08F * m * Mth.sin(p + 0.8F));
		model.leftLeg.xRot = Mth.lerp(swimAmount, model.leftLeg.xRot, -0.08F * m * Mth.sin(p + 0.8F));
		model.rightLeg.zRot = Mth.lerp(swimAmount, model.rightLeg.zRot, 0.12F);
		model.leftLeg.zRot = Mth.lerp(swimAmount, model.leftLeg.zRot, -0.12F);
		model.head.xRot = -1.15F * swimAmount;
		model.head.zRot = 0.2F * m * Mth.sin(p - 0.6F) * swimAmount;
	}
}
