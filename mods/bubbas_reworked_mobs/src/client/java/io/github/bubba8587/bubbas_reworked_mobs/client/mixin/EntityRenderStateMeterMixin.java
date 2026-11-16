// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.client.meter.MeterState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries a mob's visible meter from extract to submit (show_meters). */
@Mixin(EntityRenderState.class)
abstract class EntityRenderStateMeterMixin implements MeterState {
	@Unique
	private int brm$meter;
	@Unique
	private float brm$barX;
	@Unique
	private float brm$barY;
	@Unique
	private float brm$barZ;

	@Override
	public int brm$meter() {
		return brm$meter;
	}

	@Override
	public float brm$barX() {
		return brm$barX;
	}

	@Override
	public float brm$barY() {
		return brm$barY;
	}

	@Override
	public float brm$barZ() {
		return brm$barZ;
	}

	@Override
	public void brm$setMeter(int packed, float x, float y, float z) {
		brm$meter = packed;
		brm$barX = x;
		brm$barY = y;
		brm$barZ = z;
	}
}
