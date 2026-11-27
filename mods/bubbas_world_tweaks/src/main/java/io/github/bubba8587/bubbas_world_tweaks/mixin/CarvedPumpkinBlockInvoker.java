// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's golem spawn step (clear blocks, place, advancement, block updates). */
@Mixin(CarvedPumpkinBlock.class)
public interface CarvedPumpkinBlockInvoker {
	@Invoker("spawnGolemInWorld")
	static void bubbas_world_tweaks$spawnGolemInWorld(Level level, BlockPattern.BlockPatternMatch match, Entity golem, BlockPos spawnPos) {
		throw new AssertionError();
	}
}
