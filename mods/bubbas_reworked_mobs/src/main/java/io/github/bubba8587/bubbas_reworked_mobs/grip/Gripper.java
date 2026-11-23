// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grip;

/**
 * A mob that holds its victim as a passenger (the drowned grab's riding hold, specs/drowned.md), so the shared grip
 * rules apply: the held player can't sneak off, can hit the mob holding them, isn't drawn sitting, and sees a hint.
 * Vanilla drowned are checked by class in the same places; mobs of our own implement this.
 */
public interface Gripper {
	/** Lang key of the action-bar hint a held player sees. */
	String grabbedHintKey();
}
