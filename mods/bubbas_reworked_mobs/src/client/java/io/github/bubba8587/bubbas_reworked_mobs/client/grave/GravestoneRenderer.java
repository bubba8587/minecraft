// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.client.grave;

import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.blockentity.state.StandingSignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * A gravestone's carved text: vanilla's sign text renderer ({@link AbstractSignRenderer}: colours, glow ink, the
 * outline, filtering) with the text placed on the stone's face instead of a sign's board. The stone itself is an
 * ordinary block model (models/block/gravestone_rot_*), as vanilla's signs are in 26.3.
 *
 * <p>Face geometry (block pixels, rotation 0): the stone's front is the south face at z = 10, its body from y 2 to 12
 * (a shoulder and crown step the top to 14), on a plinth. Text at the sign's own scale (1/96 block per font pixel), centred at y 7.5.
 */
public class GravestoneRenderer extends AbstractSignRenderer<StandingSignRenderState> {
	/** The face is 2 px in front of the block's centre; text sits just proud of it. */
	private static final float FACE_OFFSET = 2.0F / 16.0F + 0.003F;
	/** Text centre height relative to the block's centre (y 7.5 of 16, the panel's middle). */
	private static final float TEXT_HEIGHT = 7.5F / 16.0F - 0.5F;
	private static final float SCALE = 1.0F / 96.0F;

	public GravestoneRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public StandingSignRenderState createRenderState() {
		return new StandingSignRenderState();
	}

	@Override
	public void extractRenderState(SignBlockEntity blockEntity, StandingSignRenderState state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
		float angle = RotationSegment.convertToDegrees(blockEntity.getBlockState().getValue(StandingSignBlock.ROTATION));
		state.transformations = new SignRenderState.SignTransformations(text(angle, true), text(angle, false));
	}

	private static Transformation text(float angle, boolean front) {
		Matrix4f m = new Matrix4f().translate(0.5F, 0.5F, 0.5F).rotate(Axis.YP.rotationDegrees(-angle));
		if (!front) {
			m.rotate(Axis.YP.rotationDegrees(180.0F));
		}
		return new Transformation(m.translate(0.0F, TEXT_HEIGHT, FACE_OFFSET).scale(SCALE, -SCALE, SCALE));
	}
}
