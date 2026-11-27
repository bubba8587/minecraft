// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client.golem;

import net.minecraft.util.Mth;

/** Amethyst Golem poses: head look, walk, dance, sing, listen. */
public final class AmethystGolemAnimations {
	private static final float DEG = Mth.DEG_TO_RAD;

	private AmethystGolemAnimations() {
	}

	/** Poses the model from a freshly reset pose. */
	public static void apply(AmethystGolemModel model, AmethystGolemRenderState state) {
		model.head.xRot = state.xRot * DEG;
		model.head.yRot = state.yRot * DEG;
		walk(model, state.walkAnimationPos, Math.min(state.walkAnimationSpeed, 1.0F));
		float t = state.ageInTicks;
		if (state.dancing) {
			dance(model, t);
		} else if (state.recording) {
			listen(model, t);
		}
		if (state.singing) {
			sing(model, t);
		}
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

	/** Dance: rock and sway, arms up and waving, head bobbing to the beat. */
	private static void dance(AmethystGolemModel model, float t) {
		float beat = t * 0.45F;
		float sway = Mth.sin(beat);
		model.body.xRot = Math.abs(Mth.cos(beat)) * 0.12F;
		model.body.zRot = sway * 0.18F;
		model.rightArm.zRot = 2.3F + Mth.sin(beat * 2.0F) * 0.35F;
		model.leftArm.zRot = -2.3F + Mth.sin(beat * 2.0F + Mth.PI) * 0.35F;
		model.rightArm.xRot = 0.0F;
		model.leftArm.xRot = 0.0F;
		model.head.zRot = -sway * 0.2F;
		model.head.xRot += Math.abs(Mth.sin(beat)) * 0.25F;
		model.rightLeg.xRot = Math.max(0.0F, sway) * -0.4F;
		model.leftLeg.xRot = Math.max(0.0F, -sway) * -0.4F;
		model.crystals.yRot = sway * 0.15F;
	}

	/** Sing: head tipped back, arms a little open, crystals shimmering. */
	private static void sing(AmethystGolemModel model, float t) {
		model.head.xRot = Math.min(model.head.xRot, 0.0F) - 0.3F + Mth.sin(t * 0.6F) * 0.05F;
		model.rightArm.zRot += 0.25F;
		model.leftArm.zRot -= 0.25F;
		model.crystals.zRot = Mth.sin(t * 1.3F) * 0.06F;
		mouth(model, 0.5F + 0.5F * Mth.sin(t * 0.8F));
	}

	/** Listen: head cocked, one hand cupped to the side of the head. */
	private static void listen(AmethystGolemModel model, float t) {
		model.head.zRot = 0.32F + Mth.sin(t * 0.08F) * 0.04F;
		model.head.xRot += 0.12F;
		model.rightArm.xRot = -2.5F;
		model.rightArm.zRot = 0.55F;
		model.leftArm.xRot = -0.25F;
	}

	/** Jaw: opens the mouth by {@code open} (0-1). */
	private static void mouth(AmethystGolemModel model, float open) {
		model.jaw.xRot = open * 0.5F;
	}
}
