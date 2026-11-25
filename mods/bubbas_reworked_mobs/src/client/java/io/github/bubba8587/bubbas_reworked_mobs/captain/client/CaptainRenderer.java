// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import io.github.bubba8587.bubbas_reworked_mobs.captain.WitherCaptain;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

/**
 * Wither Captain renderer: the wither skeleton's (AbstractSkeletonRenderer's few lines, with CaptainModel for the body
 * and armour), plus the summoning aura.
 */
public class CaptainRenderer extends HumanoidMobRenderer<WitherCaptain, CaptainRenderState, CaptainModel> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png");

	public CaptainRenderer(EntityRendererProvider.Context context) {
		super(context, new CaptainModel(context.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.5F);
		this.addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.WITHER_SKELETON_ARMOR, context.getModelSet(), CaptainModel::new),
				context.getEquipmentRenderer()));
		this.addLayer(new CaptainAuraLayer<>(this, context.getModelSet(), state -> state.summoning > 0.0F));
	}

	@Override
	public Identifier getTextureLocation(CaptainRenderState state) {
		return TEXTURE;
	}

	@Override
	public CaptainRenderState createRenderState() {
		return new CaptainRenderState();
	}

	@Override
	public void extractRenderState(WitherCaptain entity, CaptainRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isAggressive = entity.isAggressive();
		state.isShaking = entity.isShaking();
		state.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
		state.isSummoning = entity.isSummoning();
		state.summoning = entity.summonBlend(partialTicks);
	}

	@Override
	protected boolean isShaking(CaptainRenderState state) {
		return state.isShaking;
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(WitherCaptain mob, HumanoidArm arm) {
		return mob.getMainArm() == arm && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW)
				? HumanoidModel.ArmPose.BOW_AND_ARROW
				: super.getArmPose(mob, arm);
	}
}
