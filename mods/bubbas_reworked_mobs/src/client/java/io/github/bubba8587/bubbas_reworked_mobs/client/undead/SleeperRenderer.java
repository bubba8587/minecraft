// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.bubba8587.bubbas_reworked_mobs.client.mixin.EntityRenderDispatcherAccessor;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * v0 sleeper renderer: draws the whole sleeper with the vanilla mob model and texture (placeholder art), posed by
 * {@link SleeperPoses}, with armor and held items through vanilla's own layers. The planned hybrid moves the body
 * into a baked block model and keeps only gear here (specs/undead.md "Rendering").
 */
public class SleeperRenderer implements BlockEntityRenderer<SleeperBlockEntity, SleeperRenderState> {
	private final Map<UndeadVariant, PosedHumanoid<?, ?>> variants = new EnumMap<>(UndeadVariant.class);
	private final ItemModelResolver itemModels;

	public SleeperRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
		EquipmentLayerRenderer equipment = new EquipmentLayerRenderer(
				((EntityRenderDispatcherAccessor) context.entityRenderer()).brm$getEquipmentAssets(), Minecraft.getInstance().getPalettedTextureManager());
		// The client variant table: one entry per UndeadVariant.
		variants.put(UndeadVariant.SKELETON, new PosedHumanoid<SkeletonRenderState, SkeletonModel<SkeletonRenderState>>(context.entityModelSet(), equipment,
				SkeletonRenderState::new, Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png"),
				ModelLayers.SKELETON, ModelLayers.SKELETON_ARMOR, SleeperRenderer::skeletonModel));
		variants.put(UndeadVariant.ZOMBIE, new PosedHumanoid<ZombieRenderState, ZombieModel<ZombieRenderState>>(context.entityModelSet(), equipment,
				ZombieRenderState::new, Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png"),
				ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_ARMOR, SleeperRenderer::zombieModel));
		variants.put(UndeadVariant.HUSK, new PosedHumanoid<ZombieRenderState, ZombieModel<ZombieRenderState>>(context.entityModelSet(), equipment,
				ZombieRenderState::new, Identifier.withDefaultNamespace("textures/entity/zombie/husk.png"),
				ModelLayers.HUSK, ModelLayers.HUSK_ARMOR, SleeperRenderer::zombieModel));
		variants.put(UndeadVariant.STRAY, new PosedHumanoid<SkeletonRenderState, SkeletonModel<SkeletonRenderState>>(context.entityModelSet(), equipment,
				SkeletonRenderState::new, Identifier.withDefaultNamespace("textures/entity/skeleton/stray.png"),
				ModelLayers.STRAY, ModelLayers.STRAY_ARMOR, SleeperRenderer::skeletonModel));
	}

	private static SkeletonModel<SkeletonRenderState> skeletonModel(ModelPart root, SleeperPose pose) {
		return new SkeletonModel<>(root) {
			@Override
			public void setupAnim(SkeletonRenderState state) {
				super.setupAnim(state);
				SleeperPoses.apply(this, pose);
			}
		};
	}

	private static ZombieModel<ZombieRenderState> zombieModel(ModelPart root, SleeperPose pose) {
		return new ZombieModel<>(root) {
			@Override
			public void setupAnim(ZombieRenderState state) {
				super.setupAnim(state);
				SleeperPoses.apply(this, pose);
			}
		};
	}

	@Override
	public SleeperRenderState createRenderState() {
		return new SleeperRenderState();
	}

	@Override
	public void extractRenderState(SleeperBlockEntity sleeper, SleeperRenderState state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(sleeper, state, partialTicks, cameraPosition, breakProgress);
		if (!(sleeper.getBlockState().getBlock() instanceof SleeperBlock block)) {
			state.humanoid = null;
			return;
		}
		state.variant = block.variant();
		state.pose = sleeper.getBlockState().getValue(SleeperBlock.POSE);
		state.facing = sleeper.getBlockState().getValue(SleeperBlock.FACING);
		HumanoidRenderState humanoid = variants.get(state.variant).createState();
		humanoid.entityType = ModEntities.woken(state.variant);
		humanoid.lightCoords = state.lightCoords;
		humanoid.headEquipment = renderable(sleeper, EquipmentSlot.HEAD);
		humanoid.chestEquipment = renderable(sleeper, EquipmentSlot.CHEST);
		humanoid.legsEquipment = renderable(sleeper, EquipmentSlot.LEGS);
		humanoid.feetEquipment = renderable(sleeper, EquipmentSlot.FEET);
		int seed = (int) sleeper.getBlockPos().asLong();
		humanoid.rightHandItemStack = sleeper.getGear(EquipmentSlot.MAINHAND).copy();
		humanoid.leftHandItemStack = sleeper.getGear(EquipmentSlot.OFFHAND).copy();
		itemModels.updateForTopItem(humanoid.rightHandItemState, humanoid.rightHandItemStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, sleeper.getLevel(), null, seed);
		itemModels.updateForTopItem(humanoid.leftHandItemState, humanoid.leftHandItemStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, sleeper.getLevel(), null, seed + 1);
		state.humanoid = humanoid;
	}

	private static ItemStack renderable(SleeperBlockEntity sleeper, EquipmentSlot slot) {
		ItemStack stack = sleeper.getGear(slot);
		return HumanoidArmorLayer.shouldRender(stack, slot) ? stack.copy() : ItemStack.EMPTY;
	}

	@Override
	public void submit(SleeperRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.humanoid == null) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.0F, 0.5F);
		// Same turn as LivingEntityRenderer#setupRotations for a mob facing this way; the model's front is then -Z.
		poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());
		SleeperPoses.placeInBlock(poseStack, state.pose);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		variants.get(state.variant).submit(state.pose, state.humanoid, poseStack, collector, state.lightCoords, state.breakProgress);
		poseStack.popPose();
	}
}
