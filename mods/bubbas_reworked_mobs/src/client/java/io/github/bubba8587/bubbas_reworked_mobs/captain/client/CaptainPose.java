// dte:C1,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Wither Captain summoning pose, blended over the vanilla pose by the summoning amount (0..1). */
public final class CaptainPose {
	// head leaned back
	private static final float HEAD_BACK = -0.55F;
	private static final float HEAD_BOB = 0.04F;
	// sword arm (right) raised overhead
	private static final float SWORD_X = -2.3F;
	private static final float SWORD_Z = 0.3F;
	private static final float SWORD_SWAY_X = 0.08F;
	private static final float SWORD_SWAY_Z = 0.06F;
	// free arm (left) out to the side
	private static final float ARM_OUT_Z = 1.35F;
	private static final float ARM_WAVE = 0.18F;
	private static final float ARM_X = -0.15F;
	private static final float ARM_DRIFT_X = 0.05F;

	private CaptainPose() {
	}

	public static void apply(HumanoidModel<?> model, float summoning, float ageInTicks) {
		float s = Mth.clamp(summoning, 0.0F, 1.0F);
		if (s <= 0.0F) {
			return;
		}
		float t = ageInTicks;

		// head
		blend(model.head, HEAD_BACK + HEAD_BOB * Mth.sin(t * 0.05F), 0.0F, 0.0F, s);

		// sword arm
		blend(model.rightArm,
			SWORD_X + SWORD_SWAY_X * Mth.sin(t * 0.09F),
			0.0F,
			SWORD_Z + SWORD_SWAY_Z * Mth.sin(t * 0.11F + 1.0F), s);

		// free arm
		blend(model.leftArm,
			ARM_X + ARM_DRIFT_X * Mth.sin(t * 0.07F),
			0.0F,
			-(ARM_OUT_Z + ARM_WAVE * Mth.sin(t * 0.12F)), s);

		// legs planted
		blend(model.rightLeg, 0.0F, 0.0F, 0.0F, s);
		blend(model.leftLeg, 0.0F, 0.0F, 0.0F, s);
	}

	private static void blend(ModelPart part, float x, float y, float z, float s) {
		part.xRot = Mth.lerp(s, part.xRot, x);
		part.yRot = Mth.lerp(s, part.yRot, y);
		part.zRot = Mth.lerp(s, part.zRot, z);
	}
}
