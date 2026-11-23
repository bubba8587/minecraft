// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.test;

import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconAreas;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.BeaconLogic;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.HangingBeacon;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.Payloads;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.ReworkedBeacon;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.StandalonePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Beacon tests. */
public class BeaconGameTest {
	/** Builds a square pyramid under or over beacon. */
	static void pyramid(GameTestHelper helper, BlockPos beacon, int layers, int dir, Block block) {
		for (int step = 1; step <= layers; step++) {
			for (int x = -step; x <= step; x++) {
				for (int z = -step; z <= step; z++) {
					helper.setBlock(beacon.offset(x, dir * step, z), block);
				}
			}
		}
	}

	static ReworkedBeacon beaconAt(GameTestHelper helper, BlockPos rel) {
		return (ReworkedBeacon) helper.getBlockEntity(rel, BeaconBlockEntity.class);
	}

	/** Sky beacon, real ticks: an amethyst shard on a 1-layer pyramid gives Speed I to a pig. */
	public void standalonePaymentBuysItsPower(GameTestHelper helper) {
		BlockPos beacon = new BlockPos(4, 2, 4);
		pyramid(helper, beacon, 1, -1, Blocks.IRON_BLOCK);
		helper.setBlock(beacon, Blocks.BEACON);
		beaconAt(helper, beacon).bubba$setItem(new ItemStack(Items.AMETHYST_SHARD));
		LivingEntity pig = helper.spawn(EntityTypes.PIG, new BlockPos(1, 2, 1));
		helper.succeedWhen(() -> {
			MobEffectInstance speed = pig.getEffect(MobEffects.SPEED);
			helper.assertTrue(speed != null, "pig in a 1-layer sky beacon with an amethyst shard has no Speed yet");
			helper.assertValueEqual(speed.getAmplifier(), 0, "Speed amplifier on 1 layer");
		});
	}

	/** The standalone table and its pyramid rules. */
	public void pyramidSizeCapsThePower(GameTestHelper helper) {
		StandalonePayload standalone = new StandalonePayload();
		helper.assertValueEqual(standalone.requiredLevels(new ItemStack(Items.AMETHYST_SHARD)), 1, "amethyst shard layers");
		helper.assertValueEqual(standalone.requiredLevels(new ItemStack(Items.ENDER_PEARL)), 2, "ender pearl layers");
		helper.assertValueEqual(standalone.requiredLevels(new ItemStack(Items.NETHER_STAR)), 3, "nether star layers");
		helper.assertTrue(!standalone.accepts(new ItemStack(Items.EMERALD)) && Payloads.find(new ItemStack(Items.IRON_INGOT)) == null,
				"emerald and iron are no longer payments");
		helper.assertValueEqual(StandalonePayload.amplifierFor(3), 0, "level I on 3 layers");
		helper.assertValueEqual(StandalonePayload.amplifierFor(4), 1, "level II on 4 layers");
		int[] caps = {0, 1, 3, 5, 6};
		for (int l = 0; l <= 4; l++) {
			helper.assertValueEqual(BeaconContext.powerCapFor(l), caps[l], "power cap for " + l + " layers");
		}
		helper.assertTrue(Payloads.find(new ItemStack(Items.ECHO_SHARD)) instanceof TestPayload, "test mod's payload not loaded from the entrypoint");

		BlockPos beacon = new BlockPos(4, 3, 4);
		pyramid(helper, beacon, 2, -1, Blocks.IRON_BLOCK);
		helper.setBlock(beacon, Blocks.BEACON);
		ReworkedBeacon rb = beaconAt(helper, beacon);
		rb.bubba$setItem(new ItemStack(Items.ECHO_SHARD));
		helper.succeedWhen(() -> {
			BeaconContext ctx = BeaconLogic.activeContext(helper.getLevel(), helper.absolutePos(beacon), rb);
			helper.assertTrue(ctx != null, "2-layer sky beacon not active yet");
			helper.assertValueEqual(ctx.levels(), 2, "layers");
			helper.assertValueEqual(ctx.effectivePower(), 3, "Gem Power V capped on 2 layers");
			BeaconContext applied = TestPayload.lastApplied;
			helper.assertTrue(applied != null && applied.effectivePower() == 3, "payload not pulsed at the capped power");
			helper.assertTrue(BeaconAreas.isInside(helper.getLevel(), helper.absolutePos(beacon.east(25)), s -> s.is(Items.ECHO_SHARD)),
					"area query: 25 blocks out should be inside a 2-layer (30 block) sky area");
			helper.assertTrue(!BeaconAreas.isInside(helper.getLevel(), helper.absolutePos(beacon.east(35)), s -> s.is(Items.ECHO_SHARD)),
					"area query: 35 blocks out should be outside");
		});
	}

	/** A floor beacon pairs with an upside-down one above it through glass. */
	public void pairedBeaconsMakeAWideBox(GameTestHelper helper) {
		BlockPos floor = new BlockPos(4, 2, 4);
		BlockPos ceiling = floor.above(4);
		pyramid(helper, floor, 1, -1, Blocks.IRON_BLOCK);
		helper.setBlock(floor, Blocks.BEACON);
		helper.setBlock(floor.above(1), Blocks.GLASS);
		helper.setBlock(ceiling, Blocks.BEACON.defaultBlockState().setValue(HangingBeacon.UPSIDE_DOWN, true));
		pyramid(helper, ceiling, 2, 1, Blocks.GOLD_BLOCK);
		ReworkedBeacon rb = beaconAt(helper, floor);
		rb.bubba$setItem(new ItemStack(Items.AMETHYST_SHARD));
		BlockPos abs = helper.absolutePos(floor);

		BeaconLogic.updatePair(helper.getLevel(), abs, rb);
		helper.assertTrue(rb.bubba$isPaired(), "floor beacon did not pair through glass");
		helper.assertValueEqual(rb.bubba$pairGap(), 4, "gap");
		helper.assertValueEqual(rb.bubba$ceilingLevels(), 2, "ceiling pyramid");
		helper.assertValueEqual(rb.bubba$pairLevels(), 1, "the smaller pyramid sets the layers");
		helper.assertValueEqual(BeaconLogic.mode(rb), BeaconMode.PAIRED, "mode");
		AABB area = BeaconLogic.area(helper.getLevel(), abs, rb, BeaconMode.PAIRED, 1);
		AABB expected = new AABB(abs.getX() - 60, abs.getY() - 1 - 10, abs.getZ() - 60,
				abs.getX() + 61, abs.getY() + 4 + 2 + 10 + 1, abs.getZ() + 61);
		helper.assertValueEqual(area, expected, "paired area");

		LivingEntity pig = helper.spawn(EntityTypes.PIG, new BlockPos(1, 2, 1));
		BeaconLogic.pulse(helper.getLevel(), abs, rb);
		helper.assertTrue(pig.hasEffect(MobEffects.SPEED), "paired pulse gave no Speed to a pig in the box");

		// Blocked beam: the pair is off
		helper.setBlock(floor.above(2), Blocks.STONE);
		BeaconLogic.updatePair(helper.getLevel(), abs, rb);
		helper.assertTrue(!rb.bubba$isPaired(), "pair still on with stone in the beam");
		// The upside-down beacon alone is inert
		helper.assertTrue(helper.getBlockEntity(ceiling, BeaconBlockEntity.class).getBeamSections().isEmpty(),
				"upside-down beacon has a beam of its own");
		helper.succeed();
	}

	/** Placing a beacon against the underside of a block hangs it upside down. */
	public void beaconHangsUnderABlock(GameTestHelper helper) {
		helper.assertTrue(!HangingBeacon.isUpsideDown(Blocks.BEACON.defaultBlockState()), "beacons should default to upright");
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos roof = new BlockPos(2, 4, 2);
		helper.setBlock(roof, Blocks.STONE);
		place(helper, player, roof, Direction.DOWN);
		helper.assertTrue(HangingBeacon.isHangingBeacon(helper.getBlockState(roof.below())), "beacon placed under stone is not upside down");

		BlockPos ground = new BlockPos(5, 1, 5);
		helper.setBlock(ground, Blocks.STONE);
		place(helper, player, ground, Direction.UP);
		helper.assertTrue(helper.getBlockState(ground.above()).is(Blocks.BEACON) && !HangingBeacon.isUpsideDown(helper.getBlockState(ground.above())),
				"beacon placed on stone is not upright");
		helper.succeed();
	}

	private static void place(GameTestHelper helper, Player player, BlockPos clicked, Direction face) {
		BlockPos abs = helper.absolutePos(clicked);
		Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		ItemStack stack = new ItemStack(Items.BEACON);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
	}
}
