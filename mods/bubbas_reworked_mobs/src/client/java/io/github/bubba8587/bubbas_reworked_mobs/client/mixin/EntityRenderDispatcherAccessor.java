// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 26.3 has no public getter for the equipment asset manager, which armor rendering outside an entity renderer needs. */
@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
	@Accessor("equipmentAssets")
	EquipmentAssetManager brm$getEquipmentAssets();
}
