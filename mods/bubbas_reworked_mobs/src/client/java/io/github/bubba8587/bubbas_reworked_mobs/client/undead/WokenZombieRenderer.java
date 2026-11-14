// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * Vanilla's zombie renderer (same layers, texture and baby model) with our shambling model for the adult body and its
 * armor, so the armor shambles too. Babies keep vanilla's model.
 */
public class WokenZombieRenderer extends AbstractZombieRenderer<Zombie, ZombieRenderState, ZombieModel<ZombieRenderState>> {
	public WokenZombieRenderer(EntityRendererProvider.Context context) {
		super(context,
				new Shambling(context.bakeLayer(ModelLayers.ZOMBIE)),
				new BabyZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
				ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), Shambling::new),
				ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new));
	}

	@Override
	public ZombieRenderState createRenderState() {
		return new ZombieRenderState();
	}

	static final class Shambling extends ZombieModel<ZombieRenderState> {
		Shambling(ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(ZombieRenderState state) {
			super.setupAnim(state);
			UndeadGaits.shamble(this, state);
		}
	}
}
