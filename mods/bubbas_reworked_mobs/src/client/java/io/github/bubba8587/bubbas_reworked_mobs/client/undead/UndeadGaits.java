// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.ZombieGait;
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
 * <li>Zombies shamble in two phases, a slow drag of the bad leg and a quick lurch forward ({@link #shamble}).</li>
 * <li>Skeletons walk like vanilla zombies: both arms reaching straight out, grabbing (unless drawing a bow).</li>
 * </ul>
 */
public final class UndeadGaits {
	/** Model units from the root pivot (the neck) down to the soles of the feet. */
	private static final float FEET_Y = 24.0F;

	private UndeadGaits() {
	}

	/**
	 * Zombies: a heavy, limp shamble. A long drag, then the body falls forward onto the good leg (ZombieGait has the
	 * cycle and the matching real motion). The jerk comes from the timing (long drag, quicker step), not from violent
	 * moves: everything eases in and out, and the loose parts (arms, head) lag the torso.
	 * <ul>
	 * <li>DRAG (most of the cycle): the body creeps on over the stiff right leg, planted in front at the start and
	 * behind at the end (it sweeps back as far as the body moves); the bad left leg is dragged up from behind, toe
	 * turned out and scraping, faster than the body.</li>
	 * <li>STEP (short): the body pitches forward and falls onto the right leg, which swings through stiff and lands in
	 * front; a slight dip on landing; the planted left leg sweeps back with the body.</li>
	 * <li>Dead weight: the arms hang and swing like pendulums a beat behind the body, forward with the fall, then drift
	 * back to hanging through the drag, the left a little later than the right. The torso rocks onto the bad leg as
	 * the good one swings through and back over the good leg through the drag; the head lolls after it.</li>
	 * </ul>
	 * {@code cycle} is where in the cycle the zombie is (0 to 1), {@code walk} how much it is walking (0 standing, 1
	 * walking).
	 */
	public static void shamble(HumanoidModel<?> model, HumanoidRenderState state, float cycle, float walk) {
		float step = ZombieGait.lurch(cycle);
		// Time since the step began, as a fraction of the cycle (the step is the first 1 - DRAG_FRACTION of it).
		float t = cycle - UndeadTuning.GAIT_DRAG_FRACTION;
		if (t < 0.0F) {
			t += 1.0F;
		}
		float leftLeg;
		float rightLeg;
		if (step < 0.0F) {
			float d = cycle / UndeadTuning.GAIT_DRAG_FRACTION;
			leftLeg = LEFT_BACK + (LEFT_UNDER - LEFT_BACK) * smooth(d);
			// Planted: the body creeps at a steady speed through the drag, so the leg sweeps back evenly.
			rightLeg = RIGHT_FRONT + (RIGHT_BACK - RIGHT_FRONT) * d;
		} else {
			// The planted left foot moves back with the body's own distance through the step (no sliding).
			float pushed = step - Mth.sin(2.0F * Mth.PI * step) / (2.0F * Mth.PI);
			leftLeg = LEFT_UNDER + (LEFT_BACK - LEFT_UNDER) * pushed;
			rightLeg = RIGHT_BACK + (RIGHT_FRONT - RIGHT_BACK) * smooth(step);
		}
		// The fall: the torso pitches forward through the step and slowly rights itself through the drag.
		float fall = swell(t, 0.0F, 0.25F, 0.75F);
		float hunch = 0.10F + 0.10F * fall;
		// Rocking: onto the bad (left, +) side as the good leg swings through, back over the good leg in the drag.
		float sway = Mth.cos(2.0F * Mth.PI * (t - 0.12F));
		float lean = 0.06F + 0.05F * sway;
		// A slight dip as the weight lands on the good leg.
		float dip = 0.6F * swell(t, 0.18F, 0.12F, 0.2F);
		// Arms: pendulums a beat behind the body, forward with the fall (they keep hanging plumb while the torso
		// pitches, and overshoot a little; negative xRot is forward), drifting back through the drag. The left one
		// later and smaller.
		float rightSwing = -ARM_SWING * swell(t, 0.06F, 0.25F, 0.6F);
		float leftSwing = -ARM_SWING * 0.8F * swell(t, 0.11F, 0.25F, 0.6F);
		model.rightLeg.xRot = rightLeg * walk;
		model.rightLeg.zRot = 0.0F;
		model.leftLeg.xRot = leftLeg * walk;
		model.leftLeg.yRot = -0.3F * walk - 0.1F;
		model.leftLeg.zRot = -0.06F - 0.04F * walk;
		// A hit swings the right arm up and down.
		float swingHit = Mth.sin(state.swingAnimation * Mth.PI);
		model.rightArm.xRot = 0.05F + rightSwing * walk - swingHit * 1.6F;
		model.leftArm.xRot = 0.08F + leftSwing * walk;
		model.rightArm.yRot = 0.0F;
		model.leftArm.yRot = 0.0F;
		model.rightArm.zRot = 0.08F - 0.04F * sway * walk;
		model.leftArm.zRot = -0.12F - 0.04F * sway * walk;
		AnimationUtils.bobArms(model.rightArm, model.leftArm, state.ageInTicks);
		// The head hangs and lolls to the good side with a slow idle roll, following the torso's rock and fall late.
		float headSway = Mth.cos(2.0F * Mth.PI * (t - 0.24F));
		model.head.xRot += 0.12F + 0.12F * swell(t, 0.08F, 0.25F, 0.6F) * walk;
		model.head.zRot = -0.12F - 0.06F * Mth.sin(state.ageInTicks * 0.05F) + 0.08F * headSway * walk;
		leanFromFeet(model.root(), 0.05F + (lean - 0.05F) * walk, 0.05F + (hunch - 0.05F) * walk);
		// Straight legs spread apart lift the hips: drop the body until the lower foot touches the ground.
		float spread = Math.min(Math.abs(model.rightLeg.xRot), Math.abs(model.leftLeg.xRot));
		model.root().y += LEG_LENGTH * (1.0F - Mth.cos(spread)) + dip * walk;
	}

	/**
	 * A smooth bump over the cycle: 0 until {@code start}, eases up to 1 over {@code rise}, eases back down to 0 over
	 * {@code fall}. {@code t} is time since the step began (0 to 1); the bump may wrap past the end of the cycle.
	 */
	private static float swell(float t, float start, float rise, float fall) {
		float x = t - start;
		if (x < 0.0F) {
			x += 1.0F;
		}
		if (x < rise) {
			return smooth(x / rise);
		}
		return 1.0F - smooth((x - rise) / fall);
	}

	/** Model units from the hip pivot to the sole. */
	private static final float LEG_LENGTH = 12.0F;

	// Leg angles (radians, + is backward) at the cycle's key points.
	/** The bad leg, trailing furthest behind at the end of the step ... */
	private static final float LEFT_BACK = 0.55F;
	/** ... and dragged up to under the hip by the end of the drag. */
	private static final float LEFT_UNDER = 0.0F;
	/** The good leg planted in front after the step; the body creeps over it through the drag to ... */
	private static final float RIGHT_BACK = 0.4F;
	/** ... here, behind, and it swings through stiff to land in front again. */
	private static final float RIGHT_FRONT = -0.45F;
	/** How far forward the arms swing with the fall (radians). */
	private static final float ARM_SWING = 0.35F;

	private static float smooth(float x) {
		x = Mth.clamp(x, 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
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
