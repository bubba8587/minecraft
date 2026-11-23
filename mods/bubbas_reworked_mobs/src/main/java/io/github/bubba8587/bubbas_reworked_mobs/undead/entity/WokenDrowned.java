// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.level.Level;

/** A woken drowned: on its first tick it becomes a plain vanilla drowned (gear and name kept). */
public class WokenDrowned extends Drowned {
	public WokenDrowned(EntityType<? extends Drowned> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		if (this.level() instanceof ServerLevel && this.isAlive()) {
			this.convertTo(EntityTypes.DROWNED, ConversionParams.single(this, true, true), drowned -> ((DrownedAccess) drowned).brm$makeVanilla());
			return;
		}
		super.tick();
	}
}
