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
	/** A hair of growth so boxes whose faces share a plane don't flicker. */
	private static final CubeDeformation NO_Z_FIGHT = new CubeDeformation(0.02F);
	private static final PartPose HEAD = PartPose.offset(0.0F, 3.0F, -1.5F);
	private static final PartPose BODY = PartPose.offset(0.0F, HIP_Y, 0.0F);
	private static final PartPose RIGHT_ARM = PartPose.offset(-12.0F, 4.0F, 0.0F);
	private static final PartPose LEFT_ARM = PartPose.offset(12.0F, 4.0F, 0.0F);
	private static final PartPose RIGHT_LEG = PartPose.offset(-5.0F, HIP_Y, 0.0F);
	private static final PartPose LEFT_LEG = PartPose.offset(5.0F, HIP_Y, 0.0F);
	private static final float EAR_ANGLE = 20.0F * Mth.DEG_TO_RAD;
	private static final PartPose LEFT_EAR = PartPose.offsetAndRotation(4.5F, -6.0F, 0.0F, 0.0F, 0.0F, -EAR_ANGLE);
	private static final PartPose RIGHT_EAR = PartPose.offsetAndRotation(-4.5F, -6.0F, 0.0F, 0.0F, 0.0F, EAR_ANGLE);

	// armour clearances over the skin
	private static final float OUTER = 0.75F;
	private static final float INNER = 0.45F;
	private static final float BOOTS = 0.8F;

	// seated: model drop so the thighs rest on the entity position, lean back, thighs forward on the cushions, arms hanging over the armrests
	static final float SEAT_DROP = 3.0F;
	private static final float SEAT_LEAN = -0.08F;
	private static final float SEAT_THIGH_X = -1.5F;
	private static final float SEAT_THIGH_SPREAD = 0.28F;
	private static final float SEAT_THIGH_Z = 0.05F;
	private static final float SEAT_ARM_X = -0.45F;
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
				.texOffs(36, 0).addBox(-3.5F, -4.0F, -6.0F, 7.0F, 4.0F, 2.0F)
				.texOffs(61, 12).addBox(-4.0F, -6.0F, -5.0F, 8.0F, 1.0F, 1.0F)
				.texOffs(58, 0).addBox(-4.5F, -3.0F, -6.0F, 1.0F, 3.0F, 1.0F)
				.texOffs(54, 0).addBox(3.5F, -3.0F, -6.0F, 1.0F, 3.0F, 1.0F);
			case "left_ear" -> CubeListBuilder.create().texOffs(51, 6).addBox(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F);
			case "right_ear" -> CubeListBuilder.create().texOffs(39, 6).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F);
			case "body" -> CubeListBuilder.create()
				.texOffs(0, 16).addBox(-10.0F, -15.0F, -7.0F, 20.0F, 15.0F, 14.0F)
				.texOffs(0, 45).addBox(-11.0F, -11.0F, -8.0F, 22.0F, 9.0F, 16.0F)
				.texOffs(68, 16).addBox(-9.0F, -12.0F, -10.0F, 18.0F, 11.0F, 3.0F)
				.texOffs(68, 30).addBox(-7.0F, -10.5F, -12.0F, 14.0F, 8.0F, 2.0F)
				.texOffs(0, 70).addBox(-8.0F, -6.0F, 8.0F, 16.0F, 6.0F, 2.0F)
				.texOffs(36, 70).addBox(-8.0F, -14.0F, 7.0F, 16.0F, 9.0F, 2.0F, NO_Z_FIGHT)
				.texOffs(64, 0).addBox(-10.0F, -16.0F, -4.5F, 6.0F, 3.0F, 9.0F, NO_Z_FIGHT)
				.texOffs(94, 0).addBox(4.0F, -16.0F, -4.5F, 6.0F, 3.0F, 9.0F, NO_Z_FIGHT);
			case "right_arm" -> CubeListBuilder.create().texOffs(0, 81).addBox(-4.0F, -2.0F, -3.0F, 6.0F, 12.0F, 6.0F);
			case "left_arm" -> CubeListBuilder.create().texOffs(24, 81).addBox(-2.0F, -2.0F, -3.0F, 6.0F, 12.0F, 6.0F);
			case "right_leg" -> CubeListBuilder.create().texOffs(48, 81).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 7.0F, 7.0F);
			case "left_leg" -> CubeListBuilder.create().texOffs(76, 81).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 7.0F, 7.0F);
			default -> CubeListBuilder.create();
		}), TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}

	/**
	 * Gold (vanilla humanoid) armour fitted to the big body: helmet; chest as a plate round the chest (its top under the
	 * chin, so snout and tusks show) plus a belly plate, and the arms; leggings as a hip band plus the legs; boots. 64x32 armour textures.
	 */
	public static ArmorModelSet<LayerDefinition> createArmorLayers() {
		return new ArmorModelSet<>(
			armour(part -> switch (part) {
				case "head" -> CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.02F));
				case "hat" -> CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.52F));
				default -> CubeListBuilder.create();
			}),
			armour(part -> switch (part) {
				case "body" -> CubeListBuilder.create()
					.texOffs(16, 16).addBox(-4.0F, -17.25F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(6.0F + OUTER, -3.0F, 5.0F + OUTER))
					.texOffs(16, 16).addBox(-4.0F, -12.5F, -13.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(3.0F + OUTER, -2.0F + OUTER, -1.0F + OUTER));
				case "right_arm" -> CubeListBuilder.create()
					.texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F + OUTER, OUTER, 1.0F + OUTER));
				case "left_arm" -> CubeListBuilder.create()
					.texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F + OUTER, OUTER, 1.0F + OUTER));
				default -> CubeListBuilder.create();
			}),
			armour(part -> switch (part) {
				case "body" -> CubeListBuilder.create()
					.texOffs(16, 16).addBox(-4.0F, -9.0F, -1.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(7.0F + INNER, -3.0F + INNER, 7.0F + INNER));
				case "right_leg" -> leg(false, INNER);
				case "left_leg" -> leg(true, INNER);
				default -> CubeListBuilder.create();
			}),
			armour(part -> switch (part) {
				case "right_leg" -> leg(false, BOOTS);
				case "left_leg" -> leg(true, BOOTS);
				default -> CubeListBuilder.create();
			})
		);
	}

	/** The armour layers baked straight from their definitions (no model layer registration needed). */
	public static ArmorModelSet<BossPiglinModel> bakeArmorModels() {
		return createArmorLayers().map(layer -> new BossPiglinModel(layer.bakeRoot()));
	}

	private static CubeListBuilder leg(boolean mirror, float clearance) {
		return CubeListBuilder.create().texOffs(0, 16).mirror(mirror)
			.addBox(-2.0F, -2.5F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.5F + clearance, -2.5F + clearance, 1.5F + clearance));
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
			flail(this.rightArm, 1.0F, 0.0F, flee, phase);
			flail(this.leftArm, -1.0F, Mth.PI, flee, phase);
			this.head.zRot += FLEE_HEAD_WOBBLE * Mth.sin(phase) * flee;
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

	/** Arms flail in step with the legs (phase is the walk cycle, as vanilla swings limbs). */
	private static void flail(ModelPart arm, float side, float offset, float flee, float phase) {
		arm.xRot = Mth.lerp(flee, arm.xRot, FLAIL_X + FLAIL_X_AMP * Mth.sin(phase + offset));
		arm.yRot = Mth.lerp(flee, arm.yRot, 0.0F);
		arm.zRot = Mth.lerp(flee, arm.zRot, side * (FLAIL_Z + FLAIL_Z_AMP * Mth.cos(phase + offset)));
	}

	private static void sit(ModelPart part, float x, float y, float z, float seat) {
		part.xRot = Mth.lerp(seat, part.xRot, x);
		part.yRot = Mth.lerp(seat, part.yRot, y);
		part.zRot = Mth.lerp(seat, part.zRot, z);
	}
}
