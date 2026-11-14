// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import net.minecraft.client.model.HumanoidModel;

/**
 * The pose table: part rotations (applied after the vanilla model's own setupAnim, so body and armor models line up)
 * and where the posed model sits in the block. One source for the renderer now and for baked pose models later.
 */
public final class SleeperPoses {
	private SleeperPoses() {
	}

	/** Part rotations in radians. */
	public static void apply(HumanoidModel<?> model, SleeperPose pose) {
		switch (pose) {
			case SITTING -> {
				// Slumped against a wall: chin on the chest, legs straight out, hands in the lap.
				set(model, 0.45F, 0.1F, 0.08F, 0.0F,
						-0.55F, 0.12F, 0.05F, -0.45F, -0.12F, -0.05F,
						-1.47F, 0.2F, 0.05F, -1.47F, -0.2F, -0.05F);
			}
			case LYING -> {
				// On its back, arms at its sides, head lolled to one side.
				set(model, 0.0F, 0.35F, 0.0F, 0.0F,
						0.0F, 0.0F, 0.08F, 0.0F, 0.0F, -0.08F,
						0.0F, 0.02F, 0.03F, 0.0F, -0.02F, -0.03F);
			}
		}
	}

	private static void set(HumanoidModel<?> m, float headX, float headY, float headZ, float bodyX,
			float rArmX, float rArmY, float rArmZ, float lArmX, float lArmY, float lArmZ,
			float rLegX, float rLegY, float rLegZ, float lLegX, float lLegY, float lLegZ) {
		m.head.setRotation(headX, headY, headZ);
		m.body.setRotation(bodyX, 0.0F, 0.0F);
		m.rightArm.setRotation(rArmX, rArmY, rArmZ);
		m.leftArm.setRotation(lArmX, lArmY, lArmZ);
		m.rightLeg.setRotation(rLegX, rLegY, rLegZ);
		m.leftLeg.setRotation(lLegX, lLegY, lLegZ);
	}

	/**
	 * Moves the pose stack (already at the block's bottom centre, turned so the sleeper's front is -Z) to where the
	 * model's feet go, oriented for the pose. The caller then applies the usual entity flip.
	 */
	public static void placeInBlock(PoseStack poseStack, SleeperPose pose) {
		switch (pose) {
			case SITTING -> poseStack.translate(0.0F, -0.6F, 0.12F);
			case LYING -> {
				// Feet toward the front, head toward the back, lying on its back; the body overhangs half a block each way.
				poseStack.translate(0.0F, 0.25F, -1.0F);
				poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
			}
		}
	}
}
