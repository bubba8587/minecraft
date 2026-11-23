// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.client;

/** Added to vanilla's beacon render state: whether the beam stops at its last section. */
public interface PairedBeamState {
	boolean bubbas_reworked_beacons$endsAtTop();

	void bubbas_reworked_beacons$setEndsAtTop(boolean value);
}
