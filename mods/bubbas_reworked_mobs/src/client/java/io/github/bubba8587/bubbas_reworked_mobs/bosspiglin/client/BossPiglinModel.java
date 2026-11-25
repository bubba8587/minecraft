// dte:C1,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.client;

import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.piglin.AbstractPiglinModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Boss Piglin: a huge piglin (round deep body, gut, rear, humped back, head sunk into the shoulders, short thick
 * legs, heavy arms) with vanilla humanoid part names, so
 * vanilla armour and held-item layers attach. Seated, waddle and flee poses blended by the render state.
 */
public class BossPiglinModel extends AbstractPiglinModel<BossPiglinRenderState> {
	/** Render scale the renderer applies (the throne's seat position assumes it). */
	public static final float RENDER_SCALE = 1.4F;
	public static final int TEXTURE_WIDTH = 128;
	public static final int TEXTURE_HEIGHT = 128;

	// part pivots
	static final float HIP_Y = 17.0F;
	private static final PartPose HEAD = PartPose.offset(0.0F, 3.0F, -1.5F);
	private static final PartPose BODY = PartPose.offset(0.0F, HIP_Y, 0.0F);
	private static final PartPose RIGHT_ARM = PartPose.offset(-13.0F, 4.0F, 0.0F);
	private static final PartPose LEFT_ARM = PartPose.offset(13.0F, 4.0F, 0.0F);
	private static final PartPose RIGHT_LEG = PartPose.offset(-5.0F, HIP_Y, 0.0F);
	private static final PartPose LEFT_LEG = PartPose.offset(5.0F, HIP_Y, 0.0F);
	private static final float EAR_ANGLE = 20.0F * Mth.DEG_TO_RAD;
	private static final PartPose LEFT_EAR = PartPose.offsetAndRotation(4.5F, -6.0F, 0.0F, 0.0F, 0.0F, -EAR_ANGLE);
	private static final PartPose RIGHT_EAR = PartPose.offsetAndRotation(-4.5F, -6.0F, 0.0F, 0.0F, 0.0F, EAR_ANGLE);

	// armour pieces: the painted band of each vanilla texture and where it sits (part space, clearance included)
	private static final float BREASTPLATE_TOP = -14.0F;
	private static final float BREASTPLATE_BOTTOM = -8.75F;
	private static final float PAULDRON_TOP = -3.6F;
	private static final float PAULDRON_BOTTOM = 0.6F;
	private static final float GIRDLE_TOP = -2.45F;
	private static final float GIRDLE_BOTTOM = 0.9F;
	private static final float THIGH_GUARD_TOP = -0.45F;
	private static final float THIGH_GUARD_BOTTOM = 2.6F;
	private static final float BOOT_TOP = 5.0F;
	private static final float BOOT_BOTTOM = 7.3F;

	// seated: model drop so the thighs rest on the entity position, lean back, thighs forward, hands on the armrests
	static final float SEAT_DROP = 3.5F;
	private static final float SEAT_LEAN = -0.08F;
	private static final float SEAT_THIGH_X = -1.45F;
	private static final float SEAT_THIGH_SPREAD = 0.28F;
	private static final float SEAT_THIGH_Z = 0.05F;
	private static final float SEAT_ARM_X = -0.75F;
	private static final float SEAT_ARM_Z = 0.15F;
	private static final float SEAT_HEAD_X = -0.08F;
	// waddle: short steps, splayed stance, body roll and yaw, a bob
	private static final float STEP_SCALE = 0.5F;
	private static final float STANCE_Z = 0.06F;
	private static final float ARM_SWING_SCALE = 0.6F;
	private static final float ARM_OUT = 0.14F;
	private static final float ARM_OUT_WALK = 0.12F;
	private static final float WADDLE_ROLL = 0.13F;
	private static final float WADDLE_YAW = 0.08F;
	private static final float WADDLE_BOB = 0.6F;
	// fleeing: forward lean, arms flailing, head wobble
	private static final float FLEE_LEAN = 0.1F;
	private static final float FLAIL_X = -1.3F;
	private static final float FLAIL_X_AMP = 0.45F;
	private static final float FLAIL_Z = 0.55F;
	private static final float FLAIL_Z_AMP = 0.2F;
	private static final float FLAIL_SPEED = 0.8F;
	private static final float FLEE_HEAD_WOBBLE = 0.1F;

	private final Quaternionf bodyRotation = new Quaternionf();
	private final Vector3f scratch = new Vector3f();

	public BossPiglinModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		return LayerDefinition.create(mesh(part -> switch (part) {
			case "head" -> CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F)
				.texOffs(60, 104).addBox(-3.5F, -4.0F, -6.0F, 7.0F, 4.0F, 2.0F)
				.texOffs(56, 84).addBox(-4.0F, -6.0F, -5.0F, 8.0F, 1.0F, 1.0F)
				.texOffs(114, 39).addBox(-4.5F, -3.0F, -6.0F, 1.0F, 3.0F, 1.0F)
				.texOffs(114, 35).addBox(3.5F, -3.0F, -6.0F, 1.0F, 3.0F, 1.0F);
			case "left_ear" -> CubeListBuilder.create()
				.texOffs(118, 22).addBox(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F);
			case "right_ear" -> CubeListBuilder.create()
				.texOffs(118, 31).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F);
			case "body" -> CubeListBuilder.create()
				.texOffs(36, 0).addBox(-10.0F, -16.0F, -7.0F, 20.0F, 10.0F, 14.0F)
				.texOffs(0, 47).addBox(-11.0F, -11.0F, -8.0F, 22.0F, 3.0F, 16.0F)
				.texOffs(0, 24).addBox(-12.0F, -8.0F, -8.5F, 24.0F, 6.0F, 17.0F)
				.texOffs(0, 66).addBox(-11.0F, -2.0F, -8.0F, 22.0F, 2.0F, 16.0F)
				.texOffs(76, 57).addBox(-9.5F, -9.0F, -11.0F, 19.0F, 7.0F, 3.0F)
				.texOffs(60, 97).addBox(-7.5F, -8.0F, -13.0F, 15.0F, 5.0F, 2.0F)
				.texOffs(20, 106).addBox(-5.5F, -7.0F, -14.0F, 11.0F, 3.0F, 1.0F)
				.texOffs(0, 16).addBox(-8.0F, -6.0F, 8.0F, 16.0F, 6.0F, 2.0F)
				.texOffs(82, 24).addBox(-8.0F, -14.0F, 7.0F, 16.0F, 9.0F, 2.0F)
				.texOffs(76, 77).addBox(-10.0F, -17.0F, -4.5F, 7.0F, 1.0F, 9.0F)
				.texOffs(76, 67).addBox(3.0F, -17.0F, -4.5F, 7.0F, 1.0F, 9.0F)
				.texOffs(0, 103).addBox(-9.0F, -18.0F, -3.0F, 4.0F, 1.0F, 6.0F)
				.texOffs(94, 101).addBox(5.0F, -18.0F, -3.0F, 4.0F, 1.0F, 6.0F);
			case "right_arm" -> CubeListBuilder.create()
				.texOffs(80, 87).addBox(-4.5F, -3.0F, -3.0F, 6.0F, 4.0F, 6.0F)
				.texOffs(108, 76).addBox(-4.0F, 1.0F, -2.5F, 5.0F, 4.0F, 5.0F)
				.texOffs(104, 11).addBox(-4.5F, 5.0F, -3.0F, 6.0F, 5.0F, 6.0F)
				.texOffs(104, 93).addBox(-4.0F, 10.0F, -3.0F, 5.0F, 3.0F, 5.0F);
			case "left_arm" -> CubeListBuilder.create()
				.texOffs(56, 87).addBox(-1.5F, -3.0F, -3.0F, 6.0F, 4.0F, 6.0F)
				.texOffs(108, 67).addBox(-1.0F, 1.0F, -2.5F, 5.0F, 4.0F, 5.0F)
				.texOffs(104, 0).addBox(-1.5F, 5.0F, -3.0F, 6.0F, 5.0F, 6.0F)
				.texOffs(108, 85).addBox(-1.0F, 10.0F, -3.0F, 5.0F, 3.0F, 5.0F);
			case "right_leg" -> CubeListBuilder.create()
				.texOffs(82, 46).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 3.0F, 8.0F)
				.texOffs(28, 84).addBox(-3.5F, 3.0F, -3.5F, 7.0F, 3.0F, 7.0F)
				.texOffs(30, 97).addBox(-3.5F, 6.0F, -5.0F, 7.0F, 1.0F, 8.0F);
			case "left_leg" -> CubeListBuilder.create()
				.texOffs(82, 35).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 3.0F, 8.0F)
				.texOffs(0, 84).addBox(-3.5F, 3.0F, -3.5F, 7.0F, 3.0F, 7.0F)
				.texOffs(0, 94).addBox(-3.5F, 6.0F, -5.0F, 7.0F, 1.0F, 8.0F);

			default -> CubeListBuilder.create();
		}), TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}

	/**
	 * Gold (vanilla humanoid) armour fitted to the big body as separate pieces with skin between: helmet; breastplate
	 * over the upper chest and pauldrons on the shoulders; a girdle under the gut and thigh guards; boots. Each piece is
	 * a vanilla-UV box stretched so the painted part of its texture lands where wanted. 64x32 armour textures.
	 */
	public static ArmorModelSet<LayerDefinition> createArmorLayers() {
		return new ArmorModelSet<>(
			armour(part -> switch (part) {
				case "head" -> CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.02F));
				case "hat" -> CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.52F));
				default -> CubeListBuilder.create();
			}),
			armour(part -> switch (part) {
				case "body" -> fit(CubeListBuilder.create(), 16, 16, 8, 12, 4, false,
					-7.5F, BREASTPLATE_TOP, -8.7F, 7.5F, topBand(BREASTPLATE_TOP, BREASTPLATE_BOTTOM, 10.0F / 12.0F), -6.0F);
				case "right_arm" -> fit(CubeListBuilder.create(), 40, 16, 4, 12, 4, false,
					-5.25F, PAULDRON_TOP, -3.75F, 2.25F, topBand(PAULDRON_TOP, PAULDRON_BOTTOM, 5.0F / 12.0F), 3.75F);
				case "left_arm" -> fit(CubeListBuilder.create(), 40, 16, 4, 12, 4, true,
					-2.25F, PAULDRON_TOP, -3.75F, 5.25F, topBand(PAULDRON_TOP, PAULDRON_BOTTOM, 5.0F / 12.0F), 3.75F);
				default -> CubeListBuilder.create();
			}),
			armour(part -> switch (part) {
				case "body" -> fit(CubeListBuilder.create(), 16, 16, 8, 12, 4, false,
					-11.45F, bottomBand(GIRDLE_TOP, GIRDLE_BOTTOM, 5.0F / 12.0F), -8.45F, 11.45F, GIRDLE_BOTTOM, 8.45F);
				case "right_leg" -> thighGuard(false);
				case "left_leg" -> thighGuard(true);
				default -> CubeListBuilder.create();
			}),
			armour(part -> switch (part) {
				case "right_leg" -> boot(false);
				case "left_leg" -> boot(true);
				default -> CubeListBuilder.create();
			})
		);
	}

	/** The armour layers baked straight from their definitions (no model layer registration needed). */
	public static ArmorModelSet<BossPiglinModel> bakeArmorModels() {
		return createArmorLayers().map(layer -> new BossPiglinModel(layer.bakeRoot()));
	}

	private static CubeListBuilder thighGuard(boolean mirror) {
		return fit(CubeListBuilder.create(), 0, 16, 4, 12, 4, mirror,
			-4.45F, THIGH_GUARD_TOP, -4.45F, 4.45F, topBand(THIGH_GUARD_TOP, THIGH_GUARD_BOTTOM, 9.0F / 12.0F), 4.45F);
	}

	private static CubeListBuilder boot(boolean mirror) {
		return fit(CubeListBuilder.create(), 0, 16, 4, 12, 4, mirror,
			-4.3F, bottomBand(BOOT_TOP, BOOT_BOTTOM, 6.0F / 12.0F), -4.3F, 4.3F, BOOT_BOTTOM, 4.3F);
	}

	/** A box with vanilla UVs (texture size w x h x d) stretched to span exactly x0..x1, y0..y1, z0..z1. */
	private static CubeListBuilder fit(CubeListBuilder cubes, int u, int v, float w, float h, float d, boolean mirror,
			float x0, float y0, float z0, float x1, float y1, float z1) {
		return cubes.texOffs(u, v).mirror(mirror).addBox((x0 + x1 - w) / 2.0F, (y0 + y1 - h) / 2.0F, (z0 + z1 - d) / 2.0F, w, h, d,
			new CubeDeformation((x1 - x0 - w) / 2.0F, (y1 - y0 - h) / 2.0F, (z1 - z0 - d) / 2.0F));
	}

	/** Bottom of a box whose top {@code fraction} (the painted rows of its texture) covers top..bottom. */
	private static float topBand(float top, float bottom, float fraction) {
		return top + (bottom - top) / fraction;
	}

	/** Top of a box whose bottom {@code fraction} covers top..bottom. */
	private static float bottomBand(float top, float bottom, float fraction) {
		return bottom - (bottom - top) / fraction;
	}

	private static LayerDefinition armour(Function<String, CubeListBuilder> cubes) {
		return LayerDefinition.create(mesh(cubes), 64, 32);
	}

	/** The part tree with this model's pivots; every part present (HumanoidModel and the piglin ears need them). */
	static MeshDefinition mesh(Function<String, CubeListBuilder> cubes) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", cubes.apply("head"), HEAD);
		head.addOrReplaceChild("hat", cubes.apply("hat"), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", cubes.apply("left_ear"), LEFT_EAR);
		head.addOrReplaceChild("right_ear", cubes.apply("right_ear"), RIGHT_EAR);
		root.addOrReplaceChild("body", cubes.apply("body"), BODY);
		root.addOrReplaceChild("right_arm", cubes.apply("right_arm"), RIGHT_ARM);
		root.addOrReplaceChild("left_arm", cubes.apply("left_arm"), LEFT_ARM);
		root.addOrReplaceChild("right_leg", cubes.apply("right_leg"), RIGHT_LEG);
		root.addOrReplaceChild("left_leg", cubes.apply("left_leg"), LEFT_LEG);
		return mesh;
	}

	@Override
	protected float getDefaultEarAngleInDegrees() {
		return 20.0F;
	}

	@Override
	public void setupAnim(BossPiglinRenderState state) {
		super.setupAnim(state);
		float seat = Mth.clamp(state.seated, 0.0F, 1.0F);
		float flee = Mth.clamp(state.fleeing, 0.0F, 1.0F);
		float walk = state.walkAnimationSpeed * (1.0F - seat);
		float phase = state.walkAnimationPos * 0.6662F;
		float t = state.ageInTicks;

		// waddle
		this.rightLeg.xRot *= STEP_SCALE;
		this.leftLeg.xRot *= STEP_SCALE;
		this.rightLeg.zRot += STANCE_Z;
		this.leftLeg.zRot -= STANCE_Z;
		this.rightArm.xRot *= ARM_SWING_SCALE;
		this.leftArm.xRot *= ARM_SWING_SCALE;
		this.rightArm.zRot += ARM_OUT + ARM_OUT_WALK * walk;
		this.leftArm.zRot -= ARM_OUT + ARM_OUT_WALK * walk;
		float bodyX = FLEE_LEAN * flee;
		float bodyY = Mth.cos(phase) * WADDLE_YAW * walk;
		float bodyZ = Mth.sin(phase) * WADDLE_ROLL * walk;
		float lift = -Mth.abs(Mth.sin(phase)) * WADDLE_BOB * walk;

		// fleeing
		if (flee > 0.0F) {
			flail(this.rightArm, 1.0F, 0.0F, flee, t);
			flail(this.leftArm, -1.0F, Mth.PI, flee, t);
			this.head.zRot += FLEE_HEAD_WOBBLE * Mth.sin(t * FLAIL_SPEED) * flee;
		}

		// seated
		if (seat > 0.0F) {
			bodyX = Mth.lerp(seat, bodyX, SEAT_LEAN);
			sit(this.rightLeg, SEAT_THIGH_X, SEAT_THIGH_SPREAD, SEAT_THIGH_Z, seat);
			sit(this.leftLeg, SEAT_THIGH_X, -SEAT_THIGH_SPREAD, -SEAT_THIGH_Z, seat);
			sit(this.rightArm, SEAT_ARM_X + 0.03F * Mth.sin(t * 0.05F), 0.0F, SEAT_ARM_Z, seat);
			sit(this.leftArm, SEAT_ARM_X + 0.03F * Mth.sin(t * 0.05F), 0.0F, -SEAT_ARM_Z, seat);
			this.head.xRot += SEAT_HEAD_X * seat;
		}

		// the body turns about the hips; head and arms ride on it
		this.body.setRotation(bodyX, bodyY, bodyZ);
		this.body.y += lift;
		this.bodyRotation.rotationZYX(bodyZ, bodyY, bodyX);
		this.ride(this.head, bodyX, bodyY, bodyZ, lift);
		this.ride(this.rightArm, bodyX, bodyY, bodyZ, lift);
		this.ride(this.leftArm, bodyX, bodyY, bodyZ, lift);

		if (seat > 0.0F) {
			float drop = SEAT_DROP * seat;
			this.head.y += drop;
			this.body.y += drop;
			this.rightArm.y += drop;
			this.leftArm.y += drop;
			this.rightLeg.y += drop;
			this.leftLeg.y += drop;
		}
	}

	private void ride(ModelPart part, float bodyX, float bodyY, float bodyZ, float lift) {
		this.scratch.set(part.x, part.y - HIP_Y, part.z).rotate(this.bodyRotation);
		part.x = this.scratch.x;
		part.y = HIP_Y + lift + this.scratch.y;
		part.z = this.scratch.z;
		part.xRot += bodyX;
		part.yRot += bodyY;
		part.zRot += bodyZ;
	}

	private static void flail(ModelPart arm, float side, float offset, float flee, float t) {
		arm.xRot = Mth.lerp(flee, arm.xRot, FLAIL_X + FLAIL_X_AMP * Mth.sin(t * FLAIL_SPEED + offset));
		arm.yRot = Mth.lerp(flee, arm.yRot, 0.0F);
		arm.zRot = Mth.lerp(flee, arm.zRot, side * (FLAIL_Z + FLAIL_Z_AMP * Mth.cos(t * FLAIL_SPEED + offset)));
	}

	private static void sit(ModelPart part, float x, float y, float z, float seat) {
		part.xRot = Mth.lerp(seat, part.xRot, x);
		part.yRot = Mth.lerp(seat, part.yRot, y);
		part.zRot = Mth.lerp(seat, part.zRot, z);
	}
}
