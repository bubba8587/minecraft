// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client.golem;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the Amethyst Golem model needs to pose itself. */
public class AmethystGolemRenderState extends LivingEntityRenderState {
	public boolean dancing;
	public boolean recording;
	/** Singing pose blend (0-1): performing a song or a wind chime. */
	public float perform;
	/** Waiting pose blend (0-1): on its jukebox between songs. */
	public float waiting;
	/** Ticks since the latest note, with partial ticks. */
	public float sinceNote = 1000.0F;
	/** Ticks between the latest note and the one before. */
	public float noteGap = 1000.0F;
	/** Latest and previous note pitch, 0 (lowest) to 1 (highest). */
	public float pitch;
	public float prevPitch;
	/** Singing sway: progress of the current half swing (0-1) and its direction (+1 or -1). */
	public float swayProgress = 1.0F;
	public float swaySide = 1.0F;
}
