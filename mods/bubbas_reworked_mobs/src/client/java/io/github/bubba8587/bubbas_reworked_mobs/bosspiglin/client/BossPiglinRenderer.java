// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.client;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglin;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;

/** Boss Piglin renderer: the fat model, its armour meshes, held item, and the cape. */
public class BossPiglinRenderer extends HumanoidMobRenderer<BossPiglin, BossPiglinRenderState, BossPiglinModel> {
	public static final ModelLayerLocation BODY = new ModelLayerLocation(BubbasReworkedMobs.id("boss_piglin"), "main");
	public static final ArmorModelSet<ModelLayerLocation> ARMOR = new ArmorModelSet<>(
			new ModelLayerLocation(BubbasReworkedMobs.id("boss_piglin"), "helmet"),
			new ModelLayerLocation(BubbasReworkedMobs.id("boss_piglin"), "chestplate"),
			new ModelLayerLocation(BubbasReworkedMobs.id("boss_piglin"), "leggings"),
			new ModelLayerLocation(BubbasReworkedMobs.id("boss_piglin"), "boots"));
	private static final Identifier TEXTURE = BubbasReworkedMobs.id("textures/entity/boss_piglin.png");

	public BossPiglinRenderer(EntityRendererProvider.Context context) {
		this(context, new BossPiglinModel(context.bakeLayer(BODY)));
	}

	private BossPiglinRenderer(EntityRendererProvider.Context context, BossPiglinModel model) {
		super(context, model, model, 0.7F, PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS);
		this.addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ARMOR, context.getModelSet(), BossPiglinModel::new), context.getEquipmentRenderer()));
		this.addLayer(new BossPiglinCapeLayer<>(this, context.getModelSet()));
	}

	@Override
	public Identifier getTextureLocation(BossPiglinRenderState state) {
		return TEXTURE;
	}

	@Override
	public BossPiglinRenderState createRenderState() {
		return new BossPiglinRenderState();
	}

	@Override
	public void extractRenderState(BossPiglin entity, BossPiglinRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.armPose = entity.getArmPose();
		state.isConverting = entity.isConverting();
		state.seated = entity.seatedBlend(partialTicks);
		state.fleeing = entity.fleeingBlend(partialTicks);
	}

	@Override
	protected boolean isShaking(BossPiglinRenderState state) {
		return super.isShaking(state) || state.isConverting;
	}
}
