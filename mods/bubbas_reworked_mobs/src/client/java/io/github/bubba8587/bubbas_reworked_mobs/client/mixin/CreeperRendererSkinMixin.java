// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_reworked_mobs.client.creeper.CreeperCarriedBlockLayer;
import io.github.bubba8587.bubbas_reworked_mobs.client.creeper.CreeperSkinState;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.look.CreeperSkins;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Biome creeper skins: the texture named by the creeper's synced skin; and its carried pillar block. */
@Mixin(CreeperRenderer.class)
abstract class CreeperRendererSkinMixin {
	@Shadow
	@Final
	private static Identifier CREEPER_LOCATION;

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Creeper;Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;F)V",
			at = @At("TAIL"))
	private void brm$extractSkin(Creeper entity, CreeperRenderState state, float partialTicks, CallbackInfo ci) {
		Identifier skin = CreeperSkins.skin(entity);
		((CreeperSkinState) state).brm$setSkinTexture(skin.equals(CreeperSkins.VANILLA) ? null
				: skin.withPath(path -> "textures/entity/creeper/biome/" + path + ".png"));
		CreeperCarriedBlockLayer.extract(entity, state);
	}

	@ModifyReturnValue(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;)Lnet/minecraft/resources/Identifier;",
			at = @At("RETURN"))
	private Identifier brm$skinTexture(Identifier original, CreeperRenderState state) {
		Identifier skin = ((CreeperSkinState) state).brm$skinTexture();
		return skin != null && original.equals(CREEPER_LOCATION) ? skin : original;
	}
}
