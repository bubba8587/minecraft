// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;

/** Boss Piglin renderer: the fat model drawn at RENDER_SCALE, its armour meshes, held item, and the cape. */
public class BossPiglinRenderer extends HumanoidMobRenderer<BossPiglin, BossPiglinRenderState, BossPiglinModel> {
	private static final Identifier TEXTURE = BubbasReworkedMobs.id("textures/entity/boss_piglin.png");

	public BossPiglinRenderer(EntityRendererProvider.Context context) {
		this(context, new BossPiglinModel(BossPiglinModel.createBodyLayer().bakeRoot()));
	}

	private BossPiglinRenderer(EntityRendererProvider.Context context, BossPiglinModel model) {
		super(context, model, model, 1.0F, PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS);
		this.addLayer(new HumanoidArmorLayer<>(this, BossPiglinModel.bakeArmorModels(), context.getEquipmentRenderer()));
		this.addLayer(new BossPiglinCapeLayer<>(this, context.getModelSet()));
	}

	@Override
	protected void scale(BossPiglinRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(BossPiglinModel.RENDER_SCALE, BossPiglinModel.RENDER_SCALE, BossPiglinModel.RENDER_SCALE);
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
		// the fleeing look only while it moves (vanilla's smoothed walk speed), so a fleeing boss standing still stands still
		state.fleeing = entity.fleeingBlend(partialTicks) * Math.min(1.0F, state.walkAnimationSpeed * FLEE_LOOK_PER_WALK);
	}

	/** Walk-animation speed (vanilla's 0..1) times this is how much of the fleeing look shows: a quarter of full stride is full flail. */
	private static final float FLEE_LOOK_PER_WALK = 4.0F;

	@Override
	protected boolean isShaking(BossPiglinRenderState state) {
		return super.isShaking(state) || state.isConverting;
	}
}
