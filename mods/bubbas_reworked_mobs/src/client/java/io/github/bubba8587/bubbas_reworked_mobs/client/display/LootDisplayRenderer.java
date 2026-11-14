// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.client.display;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlock;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlockEntity;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the one displayed item with its own item model, posed per {@link DisplayPlacement}. The item is resolved in
 * the FIXED context (what item frames use), so any vanilla or modded item and any resource pack looks right.
 *
 * <p>Pose frame: after {@link #UNDO_FIXED} the item is a sprite in the local XY plane, front toward +Z, centred on the
 * origin; a 16x tool or weapon sprite runs corner to corner, grip at -X-Y, point at +X+Y. Each placement first turns
 * local +Z to the block's {@code facing}. Cost: one item per block, no stack copies, nothing allocated beyond the
 * render state itself (as vanilla's shelf and item frame).
 */
public class LootDisplayRenderer implements BlockEntityRenderer<LootDisplayBlockEntity, LootDisplayRenderState> {
	/** Item size: a sword's diagonal comes to about 0.93 blocks. */
	private static final float SCALE = 0.75F;
	/** Half a sprite's thickness (1/16) at {@link #SCALE}, plus a hair, so flat items sit on a surface without z-fighting. */
	private static final float LIFT = 0.5F / 16.0F * SCALE + 0.002F;
	/** Half the point-to-grip length of a full-diagonal sprite at {@link #SCALE} (7 px * sqrt 2 / 16). */
	private static final float HALF_LENGTH = 0.464F;
	/** How deep a stuck item's point goes into the ground. */
	private static final float STUCK_DEPTH = 0.1F;
	/** How far a buried item's grip end sticks up, and how far it leans toward {@code facing}. */
	private static final float BURIED_TOP = 0.4F;
	private static final float BURIED_LEAN = 25.0F;
	/** The FIXED display transform of item/generated turns the model 180 degrees about Y; undo that. */
	private static final float UNDO_FIXED = 180.0F;

	private final ItemModelResolver itemModels;

	public LootDisplayRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModels = context.itemModelResolver();
	}

	@Override
	public LootDisplayRenderState createRenderState() {
		return new LootDisplayRenderState();
	}

	@Override
	public void extractRenderState(LootDisplayBlockEntity display, LootDisplayRenderState state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(display, state, partialTicks, cameraPosition, breakProgress);
		BlockState block = display.getBlockState();
		if (!block.hasProperty(LootDisplayBlock.PLACEMENT)) {
			state.item.clear();
			return;
		}
		state.placement = block.getValue(LootDisplayBlock.PLACEMENT);
		state.facing = block.getValue(LootDisplayBlock.FACING);
		int seed = HashCommon.long2int(display.getBlockPos().asLong());
		state.jitter = (HashCommon.murmurHash3(seed) & 31) - 15.5F;
		itemModels.updateForTopItem(state.item, display.getItem(), ItemDisplayContext.FIXED, display.getLevel(), null, seed);
	}

	@Override
	public void submit(LootDisplayRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.item.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.0F, 0.5F);
		poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot()); // local +Z = facing
		switch (state.placement) {
			case GROUND -> {
				// Flat on the floor, front up, grip toward facing, point away.
				poseStack.rotateDegrees(Axis.YP, state.jitter);
				poseStack.translate(0.0F, LIFT, 0.0F);
				poseStack.rotateDegrees(Axis.XP, -90.0F);
				poseStack.rotateDegrees(Axis.ZP, 45.0F);
			}
			case BURIED -> {
				// Point down, sunk into the block below; the grip end shows, leaning toward facing.
				float cos = (float) Math.cos(Math.toRadians(BURIED_LEAN));
				poseStack.translate(0.0F, BURIED_TOP - HALF_LENGTH * cos, 0.0F);
				poseStack.rotateDegrees(Axis.XP, BURIED_LEAN);
				poseStack.rotateDegrees(Axis.ZP, 225.0F);
			}
			case STUCK -> {
				// Upright, point a little way into the ground, front toward facing.
				poseStack.translate(0.0F, HALF_LENGTH - STUCK_DEPTH, 0.0F);
				poseStack.rotateDegrees(Axis.ZP, 225.0F);
			}
			case WALL -> {
				// Flat against the wall behind (at local -Z), front out, as it would hang in an item frame.
				poseStack.translate(0.0F, 0.5F, -0.5F + LIFT);
			}
		}
		poseStack.scale(SCALE, SCALE, SCALE);
		poseStack.rotateDegrees(Axis.YP, UNDO_FIXED);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}
}
