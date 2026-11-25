// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Phasing (soulfire/phasing/Phasing): the server's move check treats Phasing blocks as scaffolding, so moves aren't rejected. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class PhasingMoveCheckMixin {
	@WrapOperation(method = "isEntityCollidingWithAnythingNew", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/LevelReader;getPreMoveCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/phys/Vec3;)Ljava/lang/Iterable;"))
	private Iterable<VoxelShape> brm$phasingPreMove(LevelReader level, Entity entity, AABB box, Vec3 oldPos, Operation<Iterable<VoxelShape>> original) {
		if (!Phasing.isPhasing(entity)) {
			return original.call(level, entity, box, oldPos);
		}
		// The shapes are read lazily while iterating, so collect them under the check
		return Phasing.preMoveCheck(() -> com.google.common.collect.ImmutableList.copyOf(original.call(level, entity, box, oldPos)));
	}
}
