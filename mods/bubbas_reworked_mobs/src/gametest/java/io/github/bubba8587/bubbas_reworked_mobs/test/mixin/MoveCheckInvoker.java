// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The server's "colliding with anything new" player move check, for the Phasing tests. */
@Mixin(ServerGamePacketListenerImpl.class)
public interface MoveCheckInvoker {
	@Invoker("isEntityCollidingWithAnythingNew")
	boolean brmTest$collidingWithAnythingNew(LevelReader level, Entity entity, AABB oldAABB, double newX, double newY, double newZ);
}
