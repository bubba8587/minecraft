// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client.golem;

import net.minecraft.util.Mth;

/** Amethyst Golem poses: head look and walk. */
public final class AmethystGolemAnimations {
	private static final float DEG = Mth.DEG_TO_RAD;

	private AmethystGolemAnimations() {
	}

	/** Poses the model from a freshly reset pose. */
	public static void apply(AmethystGolemModel model, AmethystGolemRenderState state) {
		model.head.xRot = state.xRot * DEG;
		model.head.yRot = state.yRot * DEG;
		walk(model, state.walkAnimationPos, Math.min(state.walkAnimationSpeed, 1.0F));
	}

	/** Leg and arm swing. */
	private static void walk(AmethystGolemModel model, float pos, float speed) {
		float swing = Mth.cos(pos * 0.6662F) * 1.2F * speed;
		model.rightLeg.xRot = swing;
		model.leftLeg.xRot = -swing;
		model.rightArm.xRot = -swing * 0.8F;
		model.leftArm.xRot = swing * 0.8F;
		model.body.zRot = Mth.sin(pos * 0.6662F) * 0.05F * speed;
	}
}
