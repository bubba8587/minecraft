// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import io.github.bubba8587.bubbas_reworked_mobs.undead.ZombieGait;
import io.github.bubba8587.bubbas_reworked_mobs.undead.entity.WokenZombie;
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
	/** Its texture: the zombie's, or another undead's drawn on the same body (the husk's). Null keeps vanilla's choice. */
	private final net.minecraft.resources.@org.jspecify.annotations.Nullable Identifier texture;

	public WokenZombieRenderer(EntityRendererProvider.Context context) {
		this(context, null);
	}

	public WokenZombieRenderer(EntityRendererProvider.Context context, net.minecraft.resources.@org.jspecify.annotations.Nullable Identifier texture) {
		super(context,
				new Shambling(context.bakeLayer(ModelLayers.ZOMBIE)),
				new BabyZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
				ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), Shambling::new),
				ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new));
		this.texture = texture;
	}

	@Override
	public net.minecraft.resources.Identifier getTextureLocation(ZombieRenderState state) {
		return texture != null && !state.isBaby ? texture : super.getTextureLocation(state);
	}

	/**
	 * Ticks the animation runs behind the server's phase: the body's momentum takes a couple of ticks to follow the
	 * speed pulse, and the client shows positions interpolated behind the server's. Measured by UndeadGaitClientGameTest.
	 */
	public static final int CLIENT_LAG_TICKS = 4;

	@Override
	public ZombieRenderState createRenderState() {
		return new ShambleState();
	}

	@Override
	public void extractRenderState(Zombie entity, ZombieRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		if (state instanceof ShambleState shamble) {
			shamble.gaitCycle = ZombieGait.cycle(entity.getId(), entity.level().getGameTime() - CLIENT_LAG_TICKS, partialTicks);
			shamble.gaitWalk = entity instanceof WokenZombie woken ? woken.gaitWalk(partialTicks) : 0.0F;
		}
	}

	/** Our zombie's render state: where it is in its shamble cycle and how much it is walking. */
	static final class ShambleState extends ZombieRenderState {
		float gaitCycle;
		float gaitWalk;
	}

	static final class Shambling extends ZombieModel<ZombieRenderState> {
		Shambling(ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(ZombieRenderState state) {
			super.setupAnim(state);
			if (state instanceof ShambleState shamble) {
				UndeadGaits.shamble(this, state, shamble.gaitCycle, shamble.gaitWalk);
			}
		}
	}
}
