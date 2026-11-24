// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.spider.WebSettleGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the territory web settle goal to spiders and cave spiders (specs/spiders.md "Territories"). */
@Mixin(Spider.class)
abstract class SpiderSettleMixin extends Monster {
	protected SpiderSettleMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void brm$settleGoal(CallbackInfo ci) {
		goalSelector.addGoal(5, new WebSettleGoal((Spider) (Object) this));
	}
}
