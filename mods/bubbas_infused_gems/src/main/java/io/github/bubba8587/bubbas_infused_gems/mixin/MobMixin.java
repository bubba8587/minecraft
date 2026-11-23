// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.loot.GemLoot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;

/** Gems on mobs' spawn gear. */
@Mixin({Mob.class, Zombie.class, Drowned.class, ZombifiedPiglin.class, AbstractSkeleton.class, WitherSkeleton.class,
		Piglin.class, PiglinBrute.class, Pillager.class, Vindicator.class, Vex.class})
abstract class MobMixin {
	@Inject(method = "populateDefaultEquipmentSlots", at = @At("HEAD"))
	private void bubbas_infused_gems$enterSpawnGear(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
		GemLoot.enterSpawnGear();
	}

	@Inject(method = "populateDefaultEquipmentSlots", at = @At("RETURN"))
	private void bubbas_infused_gems$gemSpawnGear(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
		GemLoot.leaveSpawnGear((Mob) (Object) this);
	}
}
