// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.client.SoulFlames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The first-person burning overlay in soul flames while the local player is soul-burning. */
@Mixin(ScreenEffectRenderer.class)
abstract class SoulFireOverlayMixin {
	@ModifyArg(method = "submit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/sprite/SpriteGetter;get(Lnet/minecraft/client/resources/model/sprite/SpriteId;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
	private SpriteId brm$soulOverlay(SpriteId sprite) {
		var player = Minecraft.getInstance().player;
		return player != null && SoulBurning.isSoulBurning(player) ? SoulFlames.FIRE_1 : sprite;
	}
}
