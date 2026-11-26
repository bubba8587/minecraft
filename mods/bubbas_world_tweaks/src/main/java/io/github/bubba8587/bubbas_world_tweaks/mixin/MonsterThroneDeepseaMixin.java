// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.mixin;

import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortresses;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Guardian spawner in a deep-sea fortress's spawner room. */
@Mixin(NetherFortressPieces.MonsterThrone.class)
abstract class MonsterThroneDeepseaMixin {
	@ModifyArg(method = "postProcess", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V"),
			index = 0)
	private EntityType<?> bubbas_world_tweaks$guardianSpawner(EntityType<?> type) {
		return DeepseaFortresses.placing() ? EntityTypes.GUARDIAN : type;
	}
}
