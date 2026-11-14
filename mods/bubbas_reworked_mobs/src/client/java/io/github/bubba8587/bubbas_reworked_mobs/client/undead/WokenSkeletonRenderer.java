// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.Items;

/**
 * Vanilla's skeleton renderer (AbstractSkeletonRenderer + SkeletonRenderer, copied because their armor layer is
 * hard-wired to SkeletonModel) with our reaching model for the body and its armor.
 */
public class WokenSkeletonRenderer extends HumanoidMobRenderer<Skeleton, SkeletonRenderState, SkeletonModel<SkeletonRenderState>> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

	public WokenSkeletonRenderer(EntityRendererProvider.Context context) {
		super(context, new Reaching(context.bakeLayer(ModelLayers.SKELETON)), 0.5F);
		this.addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.SKELETON_ARMOR, context.getModelSet(), Reaching::new),
				context.getEquipmentRenderer()));
	}

	@Override
	public Identifier getTextureLocation(SkeletonRenderState state) {
		return TEXTURE;
	}

	@Override
	public SkeletonRenderState createRenderState() {
		return new SkeletonRenderState();
	}

	@Override
	public void extractRenderState(Skeleton entity, SkeletonRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isAggressive = entity.isAggressive();
		state.isShaking = entity.isShaking();
		state.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
	}

	@Override
	protected boolean isShaking(SkeletonRenderState state) {
		return state.isShaking;
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(Skeleton mob, HumanoidArm arm) {
		return mob.getMainArm() == arm && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW)
				? HumanoidModel.ArmPose.BOW_AND_ARROW
				: super.getArmPose(mob, arm);
	}

	static final class Reaching extends SkeletonModel<SkeletonRenderState> {
		Reaching(ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(SkeletonRenderState state) {
			super.setupAnim(state);
			UndeadGaits.reach(this, state, state.isAggressive);
		}
	}
}
