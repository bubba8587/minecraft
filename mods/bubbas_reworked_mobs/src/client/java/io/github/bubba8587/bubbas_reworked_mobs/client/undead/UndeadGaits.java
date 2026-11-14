// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;

/**
 * Walk animations for our woken undead (specs/undead.md "Tuning", owner's brief). Applied at the end of our own model
 * subclasses' setupAnim, so the matching armor models (same subclasses, same state) move identically and vanilla
 * zombies and skeletons are untouched.
 * <ul>
 * <li>Zombies shamble: arms hang, the left leg drags (small slow swing, turned out, trailing), the right leg steps but
 * shorter than a normal walk, and the whole body leans toward the dragging side, lurching further on each drag.</li>
 * <li>Skeletons walk like vanilla zombies: both arms reaching straight out, grabbing (unless drawing a bow).</li>
 * </ul>
 */
public final class UndeadGaits {
	/** Model units from the root pivot (the neck) down to the soles of the feet. */
	private static final float FEET_Y = 24.0F;

	private UndeadGaits() {
	}

	public static void shamble(HumanoidModel<?> model, HumanoidRenderState state) {
		float phase = state.walkAnimationPos * 0.6662F;
		// Our zombies are slow, so vanilla's walk speed is small: boost it so the gait reads.
		float walk = Mth.clamp(state.walkAnimationSpeed * 2.0F, 0.0F, 1.0F);
		float step = Mth.cos(phase);
		// The good (right) leg steps, a bit shorter than vanilla's 1.4.
		model.rightLeg.xRot = step * 0.9F * walk;
		// The bad (left) leg drags: barely swings, trails behind, foot turned out and scraping.
		model.leftLeg.xRot = -step * 0.25F * walk + 0.1F * walk;
		model.leftLeg.yRot = -0.25F;
		model.leftLeg.zRot = -0.06F;
		// Arms hang and swing loosely with the lurch; a hit swings the right arm up and down.
		float swing = Mth.sin(state.swingAnimation * Mth.PI);
		model.rightArm.xRot = -step * 0.25F * walk - swing * 1.6F;
		model.leftArm.xRot = step * 0.15F * walk + 0.1F;
		model.rightArm.yRot = 0.0F;
		model.leftArm.yRot = 0.0F;
		model.rightArm.zRot = 0.08F;
		model.leftArm.zRot = -0.12F;
		AnimationUtils.bobArms(model.rightArm, model.leftArm, state.ageInTicks);
		// The head lolls the other way, with a slow idle roll.
		model.head.zRot = -0.12F - 0.06F * Mth.sin(state.ageInTicks * 0.05F) - 0.08F * walk * Mth.sin(phase);
		// Lean toward the dragging (left, +x) side, lurching further as the weight lands on the bad leg, and hunch forward.
		float lean = 0.05F + walk * (0.09F + 0.07F * Mth.sin(phase));
		float hunch = 0.05F + 0.07F * walk;
		leanFromFeet(model.root(), lean, hunch);
	}

	public static void reach(HumanoidModel<?> model, HumanoidRenderState state, boolean aggressive) {
		if (!isFreeArm(state.rightArmPose) || !isFreeArm(state.leftArmPose)) {
			return; // drawing a bow, a crossbow, a spear: let that pose win
		}
		// Vanilla's zombie arms (AnimationUtils.animateZombieArms, which only takes zombie render states).
		float armDrop = -Mth.PI / (aggressive ? 1.5F : 2.25F);
		float attackY = Mth.sin(state.swingAnimation * Mth.PI);
		float attackX = Mth.sin((1.0F - (1.0F - state.swingAnimation) * (1.0F - state.swingAnimation)) * Mth.PI);
		float xRot = armDrop + attackY * 1.2F - attackX * 0.4F;
		float yRot = 0.1F - attackY * 0.6F;
		model.rightArm.xRot = xRot;
		model.rightArm.yRot = -yRot;
		model.rightArm.zRot = 0.0F;
		model.leftArm.xRot = xRot;
		model.leftArm.yRot = yRot;
		model.leftArm.zRot = 0.0F;
		AnimationUtils.bobArms(model.rightArm, model.leftArm, state.ageInTicks);
	}

	private static boolean isFreeArm(HumanoidModel.ArmPose pose) {
		return pose == HumanoidModel.ArmPose.EMPTY || pose == HumanoidModel.ArmPose.ITEM;
	}

	/**
	 * Tilts the whole model about the soles of its feet (not about the neck, where the root pivots), so the feet stay
	 * planted: {@code side} rolls the head toward +x (the model's left), {@code forward} pitches it toward -z.
	 * ModelPart applies Rz * Ry * Rx after its offset; the offset cancels where that moves the feet.
	 */
	private static void leanFromFeet(ModelPart root, float side, float forward) {
		root.zRot += side;
		root.xRot += forward;
		float b = FEET_Y * Mth.cos(root.xRot);
		float c = FEET_Y * Mth.sin(root.xRot);
		root.x += b * Mth.sin(root.zRot);
		root.y += FEET_Y - b * Mth.cos(root.zRot);
		root.z += -c;
	}
}
