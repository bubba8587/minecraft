// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.client.mixin;

import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import io.github.bubba8587.bubbas_reworked_beacons.client.PairedBeamState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BeaconRenderState.class)
public abstract class BeaconRenderStateMixin implements PairedBeamState {
	@Unique
	private boolean bubbas_reworked_beacons$endsAtTop;

	@Override
	public boolean bubbas_reworked_beacons$endsAtTop() {
		return this.bubbas_reworked_beacons$endsAtTop;
	}

	@Override
	public void bubbas_reworked_beacons$setEndsAtTop(boolean value) {
		this.bubbas_reworked_beacons$endsAtTop = value;
	}
}
