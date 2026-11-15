// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets a test fire a dispenser the way a redstone pulse does, many times in one tick. */
@Mixin(DispenserBlock.class)
public interface DispenserInvoker {
	@Invoker("dispenseFrom")
	void bubbas$dispense(ServerLevel level, BlockState state, BlockPos pos);
}
