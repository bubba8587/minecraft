// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.ReworkedBeacon;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import io.github.bubba8587.bubbas_reworked_beacons.client.PairedBeamState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A paired beam (specs/reworked-beacons.md: the pair's status light) ends inside the upside-down beacon instead of
 * running on to the sky like vanilla's last beam section.
 */
@Mixin(BeaconRenderer.class)
public abstract class BeaconRendererMixin {
	@Inject(method = "extract", at = @At("TAIL"))
	private static void bubbas_reworked_beacons$markPaired(BlockEntity blockEntity, BeaconRenderState state, float partialTicks, Vec3 cameraPosition,
			CallbackInfo ci) {
		((PairedBeamState) state).bubbas_reworked_beacons$setEndsAtTop(blockEntity instanceof ReworkedBeacon rb && rb.bubba$isPaired());
	}

	@WrapOperation(method = "submit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BeaconRenderer;submitBeaconBeam(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;FFIII)V"))
	private void bubbas_reworked_beacons$capPairedBeam(PoseStack poseStack, SubmitNodeCollector collector, float radiusScale, float animationTime,
			int beamStart, int height, int color, Operation<Void> original, @Local(argsOnly = true) BeaconRenderState state) {
		if (((PairedBeamState) state).bubbas_reworked_beacons$endsAtTop() && !state.sections.isEmpty()) {
			height = state.sections.getLast().height();
		}
		original.call(poseStack, collector, radiusScale, animationTime, beamStart, height, color);
	}
}
