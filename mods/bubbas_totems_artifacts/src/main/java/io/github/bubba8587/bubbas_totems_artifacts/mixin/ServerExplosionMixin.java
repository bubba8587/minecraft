// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bubba8587.bubbas_totems_artifacts.fortification.Fortification;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Totem of Fortification: the blocks an explosion is about to break (and set alight) lose the protected ones, right
 * where the explosion works them out. Block damage only; entity damage and knockback are untouched.
 */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;
	@Shadow
	@Final
	private Vec3 center;
	@Shadow
	@Final
	private float radius;
	@Shadow
	@Final
	private Explosion.BlockInteraction blockInteraction;

	@ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"))
	private List<BlockPos> bta$fortify(List<BlockPos> blocks) {
		return Fortification.protect(this.level, this.center, this.radius, this.blockInteraction, blocks);
	}
}
