// dte:C1,B20
package io.github.bubba8587.bubbas_reworked_mobs.mixin.compat;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.TerrainOracle;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lithostitched's clay-band hook in terrain building, given the server's registries when the terrain oracle builds without a region. */
@Mixin(value = NoiseBasedChunkGenerator.class, priority = 1500)
abstract class LithostitchedBandsMixin {
	@TargetHandler(mixin = "dev.worldgen.lithostitched.mixin.common.NoiseBasedChunkGeneratorMixin", name = "fillBands")
	@WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/WorldGenRegion;registryAccess()Lnet/minecraft/core/RegistryAccess;"))
	private RegistryAccess brm$regionlessRegistries(WorldGenRegion region, Operation<RegistryAccess> original) {
		if (region == null) {
			RegistryAccess registries = TerrainOracle.registries();
			if (registries != null) {
				return registries;
			}
		}
		return original.call(region);
	}
}
