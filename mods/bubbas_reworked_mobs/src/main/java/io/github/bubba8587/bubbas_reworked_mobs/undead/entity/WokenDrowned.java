// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.level.Level;

/** A woken drowned: a shipwreck's crew. */
public class WokenDrowned extends Drowned {
	public WokenDrowned(EntityType<? extends Drowned> type, Level level) {
		super(type, level);
	}
}
