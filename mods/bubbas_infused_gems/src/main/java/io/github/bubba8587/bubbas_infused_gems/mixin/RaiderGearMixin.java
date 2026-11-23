// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.loot.GemLoot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.item.ItemStack;

/** Raid mobs: a raid hands out its weapons in applyRaidBuffs, after the spawn gear hook. */
@Mixin({Pillager.class, Vindicator.class})
abstract class RaiderGearMixin {
	@Unique
	private ItemStack bubbas_infused_gems$handBefore;

	@Inject(method = "applyRaidBuffs", at = @At("HEAD"))
	private void bubbas_infused_gems$noteHand(ServerLevel level, int wave, boolean isCaptain, CallbackInfo ci) {
		this.bubbas_infused_gems$handBefore = ((Mob) (Object) this).getMainHandItem();
	}

	@Inject(method = "applyRaidBuffs", at = @At("RETURN"))
	private void bubbas_infused_gems$gemRaidWeapon(ServerLevel level, int wave, boolean isCaptain, CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (mob.getMainHandItem() != this.bubbas_infused_gems$handBefore) {
			GemLoot.gemSpawnGear(mob);
		}
		this.bubbas_infused_gems$handBefore = null;
	}
}
