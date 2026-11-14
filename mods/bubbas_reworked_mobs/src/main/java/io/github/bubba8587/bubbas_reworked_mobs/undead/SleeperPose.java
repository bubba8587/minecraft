// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import net.minecraft.util.StringRepresentable;

/** How a dormant undead lies in the world (blockstate {@code pose}). The client pose table is {@code SleeperPoses}. */
public enum SleeperPose implements StringRepresentable {
	SITTING("sitting"),
	LYING("lying");

	private final String name;

	SleeperPose(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
