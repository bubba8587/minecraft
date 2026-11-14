// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * One variant's vanilla mob model (placeholder art, v0), baked once per pose, with vanilla's own armor and held-item
 * layers attached. Pose rotations are applied in the models' setupAnim, so armor follows the body exactly.
 */
final class PosedHumanoid<S extends HumanoidRenderState, M extends HumanoidModel<S>> {
	private final Supplier<S> stateFactory;
	private final Identifier texture;
	private final Map<SleeperPose, M> bodies = new EnumMap<>(SleeperPose.class);
	private final Map<SleeperPose, HumanoidArmorLayer<S, M, M>> armor = new EnumMap<>(SleeperPose.class);
	private final Map<SleeperPose, ItemInHandLayer<S, M>> hands = new EnumMap<>(SleeperPose.class);

	PosedHumanoid(EntityModelSet models, EquipmentLayerRenderer equipment, Supplier<S> stateFactory, Identifier texture,
			ModelLayerLocation bodyLayer, ArmorModelSet<ModelLayerLocation> armorLayers, BiFunction<ModelPart, SleeperPose, M> modelFactory) {
		this.stateFactory = stateFactory;
		this.texture = texture;
		for (SleeperPose pose : SleeperPose.values()) {
			M body = modelFactory.apply(models.bakeLayer(bodyLayer), pose);
			bodies.put(pose, body);
			armor.put(pose, new HumanoidArmorLayer<S, M, M>(() -> body, ArmorModelSet.bake(armorLayers, models, part -> modelFactory.apply(part, pose)), equipment));
			hands.put(pose, new ItemInHandLayer<S, M>(() -> body));
		}
	}

	S createState() {
		return stateFactory.get();
	}

	@SuppressWarnings("unchecked")
	void submit(SleeperPose pose, HumanoidRenderState humanoid, PoseStack poseStack, SubmitNodeCollector collector, int light,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		S state = (S) humanoid;
		M body = bodies.get(pose);
		collector.submitModel(body, state, poseStack, body.renderType(texture), light, OverlayTexture.NO_OVERLAY, 0);
		if (breakProgress != null) {
			collector.order(1).submitCrumblingOverlay(body, state, poseStack, body.renderType(texture), light, OverlayTexture.NO_OVERLAY, -1, breakProgress);
		}
		// Layers read part positions now (translateToHand), so pose the body model first, as LivingEntityRenderer does.
		body.setupAnim(state);
		armor.get(pose).submit(poseStack, collector, light, state, 0.0F, 0.0F);
		hands.get(pose).submit(poseStack, collector, light, state, 0.0F, 0.0F);
	}
}
