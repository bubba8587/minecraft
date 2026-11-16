// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_reworked_mobs.client.drowned.DrownedPoses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.monster.zombie.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Grabbed by a drowned, the hint isn't vanilla's "Press Shift to dismount" (sneaking doesn't help): it says to fight free. */
@Mixin(ClientPacketListener.class)
abstract class GrabbedHintMixin {
	@WrapOperation(method = "handleSetEntityPassengersPacket",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"))
	private MutableComponent brm$grabbedHint(String key, Object[] args, Operation<MutableComponent> original) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getVehicle() instanceof Drowned) {
			return Component.translatable(DrownedPoses.GRABBED_HINT);
		}
		return original.call(key, args);
	}
}
