// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client.golem;

import net.minecraft.util.Mth;

/** Amethyst Golem poses: head look, walk, dance, sing, wait for the beat, listen, per-note jaw and crystals. */
public final class AmethystGolemAnimations {
	private static final float DEG = Mth.DEG_TO_RAD;
	private static final float ATTACK_TICKS = 1.5F;
	private static final float DECAY_TICKS = 5.5F;
	private static final float NOTE_RING_TICKS = 14.0F;

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
		} else {
			if (state.waiting > 0.0F) {
				waitForBeat(model, t, smooth(state.waiting));
			}
			if (state.perform > 0.0F) {
				sing(model, state, smooth(state.perform));
			}
		}
		note(model, state);
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

	/** Sing: head tipped back by pitch, arms open, body swaying a half swing per few notes. */
	private static void sing(AmethystGolemModel model, AmethystGolemRenderState state, float amount) {
		float pitch = Mth.lerp(smooth(Math.min(1.0F, state.sinceNote / 3.0F)), state.prevPitch, state.pitch);
		float lift = 0.2F + 0.3F * pitch + 0.08F * envelope(state);
		model.head.xRot = Mth.lerp(amount, model.head.xRot, Math.min(model.head.xRot, 0.0F) - lift);
		float sway = Mth.lerp(smooth(state.swayProgress), -state.swaySide, state.swaySide) * amount;
		model.body.zRot += sway * 0.1F;
		model.head.zRot += sway * 0.12F;
		model.rightArm.zRot += (0.35F + sway * 0.1F) * amount;
		model.leftArm.zRot -= (0.35F - sway * 0.1F) * amount;
		model.rightArm.xRot -= 0.25F * amount;
		model.leftArm.xRot -= 0.25F * amount;
		mouth(model, 0.12F * amount);
	}

	/** Waiting for the beat: head a little bowed, hands folded in front, slow breathing. */
	private static void waitForBeat(AmethystGolemModel model, float t, float amount) {
		float breath = Mth.sin(t * 0.07F);
		model.head.xRot = Mth.lerp(amount, model.head.xRot, 0.2F + breath * 0.03F);
		model.head.yRot *= 1.0F - amount * 0.7F;
		model.body.xRot += (0.04F + breath * 0.015F) * amount;
		model.rightArm.xRot = Mth.lerp(amount, model.rightArm.xRot, -0.75F);
		model.leftArm.xRot = Mth.lerp(amount, model.leftArm.xRot, -0.75F);
		model.rightArm.yRot += -0.55F * amount;
		model.leftArm.yRot += 0.55F * amount;
		model.rightArm.zRot += -0.1F * amount;
		model.leftArm.zRot += 0.1F * amount;
	}

	/** Each note: the jaw pops open and eases shut, the crystals flick and pulse. */
	private static void note(AmethystGolemModel model, AmethystGolemRenderState state) {
		float s = state.sinceNote;
		if (s >= NOTE_RING_TICKS) {
			return;
		}
		float env = envelope(state);
		model.jaw.xRot = Math.max(model.jaw.xRot, env * 0.55F);
		float decay = (float) Math.exp(-s * 0.35F);
		float strength = 0.6F + 0.4F * state.pitch;
		model.crystalCenter.xRot += Mth.sin(s * 1.9F) * decay * 0.16F * strength;
		model.crystalLeft.zRot += Mth.sin(s * 1.9F + 1.2F) * decay * 0.22F * strength;
		model.crystalRight.zRot -= Mth.sin(s * 1.9F + 2.1F) * decay * 0.22F * strength;
		model.crystalBack.xRot -= Mth.sin(s * 1.9F + 0.6F) * decay * 0.2F * strength;
		float pulse = 1.0F + 0.14F * env;
		model.crystals.xScale = pulse;
		model.crystals.yScale = pulse;
		model.crystals.zScale = pulse;
	}

	/** Note envelope (0-1): a 1.5 tick attack from wherever the last note left it, then a decay to 0 over 5.5 ticks. */
	private static float envelope(AmethystGolemRenderState state) {
		return envelope(state.sinceNote, Math.min(1.0F, decayOnly(state.noteGap)));
	}

	private static float envelope(float s, float from) {
		if (s < 0.0F) {
			return 0.0F;
		}
		if (s < ATTACK_TICKS) {
			return Mth.lerp(s / ATTACK_TICKS, from, 1.0F);
		}
		return decayOnly(s);
	}

	private static float decayOnly(float s) {
		if (s < ATTACK_TICKS) {
			return 1.0F;
		}
		float u = Math.min(1.0F, (s - ATTACK_TICKS) / DECAY_TICKS);
		return (1.0F - u) * (1.0F - u);
	}

	private static float smooth(float x) {
		return x * x * (3.0F - 2.0F * x);
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
