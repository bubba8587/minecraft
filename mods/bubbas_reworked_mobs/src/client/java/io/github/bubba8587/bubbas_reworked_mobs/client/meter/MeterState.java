// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.meter;

/** Added to every EntityRenderState ({@code EntityRenderStateMeterMixin}): the visible meter to draw over it this frame. */
public interface MeterState {
	/** Packed kind << 8 | value (1-100), or 0 for none. */
	int brm$meter();

	/** Where the bar goes, relative to the entity's render position (blocks). */
	float brm$barX();

	float brm$barY();

	float brm$barZ();

	void brm$setMeter(int packed, float x, float y, float z);
}
