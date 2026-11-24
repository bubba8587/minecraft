// dte:C6,B19
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.northwind.NorthWind;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.statue.StatueEffects;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

/** Totem of the North Wind (northwind/). Milestone; the core suite runs {@link #usedWhileGlidingBoosts}. */
public class NorthWindGameTest {
	/** A survival player in mid-air at rel, wearing an elytra, looking east; gliding if asked. */
	private static ServerPlayer glider(GameTestHelper helper, Vec3 rel, boolean gliding) {
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		Vec3 at = helper.absoluteVec(rel);
		player.snapTo(at.x, at.y, at.z, -90.0F, 0.0F);
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
		player.setOnGround(false);
		if (gliding) {
			player.startFallFlying();
		}
		return player;
	}

	private static List<FireworkRocketEntity> rocketsOf(GameTestHelper helper, ServerPlayer player) {
		return helper.getLevel().getEntitiesOfClass(FireworkRocketEntity.class, player.getBoundingBox().inflate(4.0), r -> r.getOwner() == player);
	}

	/** Core. Not gliding: nothing, no cooldown. Gliding: an attached rocket pushes the player along, cooldown on, not used up. */
	public void usedWhileGlidingBoosts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer walker = glider(helper, new Vec3(4.5, 1.2, 4.5), false);
		ItemStack idle = new ItemStack(ModItems.TOTEM_OF_NORTH_WIND);
		walker.setItemInHand(InteractionHand.MAIN_HAND, idle);
		InteractionResult none = idle.use(level, walker, InteractionHand.MAIN_HAND);
		helper.assertTrue(none == InteractionResult.PASS, "used while not gliding returned " + none);
		helper.assertFalse(walker.getCooldowns().isOnCooldown(idle), "a use while not gliding started the cooldown");
		helper.assertTrue(rocketsOf(helper, walker).isEmpty(), "a use while not gliding started a boost");
		level.getServer().getPlayerList().remove(walker);

		ServerPlayer player = glider(helper, new Vec3(4.5, 1.2, 10.5), true);
		helper.assertTrue(player.isFallFlying(), "the test player isn't gliding");
		ItemStack totem = new ItemStack(ModItems.TOTEM_OF_NORTH_WIND);
		player.setItemInHand(InteractionHand.MAIN_HAND, totem);
		InteractionResult used = totem.use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(used.consumesAction(), "used while gliding returned " + used);
		helper.assertTrue(player.getCooldowns().isOnCooldown(totem), "no cooldown after a gust");
		helper.assertTrue(totem.getCount() == 1 && player.getMainHandItem() == totem, "the Totem of the North Wind was used up");
		helper.assertTrue(rocketsOf(helper, player).size() == 1, "no rocket attached after a gust");
		helper.succeedWhen(() -> {
			Vec3 speed = player.getDeltaMovement();
			helper.assertTrue(speed.x > 0.5, "the gust didn't push the player east: " + speed);
			level.getServer().getPlayerList().remove(player);
		});
	}

	/** A North Wind statue: a glider inside its area gets a gust and the cooldown; outside, or not gliding, nothing. */
	@GameTest(structure = TestSupport.FIELD, maxTicks = 60)
	public void statueAreaGustsGliders(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos statue = new BlockPos(5, 1, 8);
		helper.setBlock(statue, TotemStatueBlock.stateFor(TotemType.NORTH_WIND));
		helper.assertTrue(StatueEffects.pulses(TotemType.NORTH_WIND), "the North Wind statue doesn't pulse");
		ServerPlayer inside = glider(helper, new Vec3(8.5, 1.2, 8.5), true);
		ServerPlayer outside = glider(helper, new Vec3(5.5 + TotemsTuning.STATUE_RADIUS + 3, 1.2, 8.5), true);
		ServerPlayer walking = glider(helper, new Vec3(3.5, 1.2, 8.5), false);
		helper.succeedWhen(() -> {
			helper.assertTrue(NorthWind.onCooldown(inside), "a glider in the statue's area got no gust");
			helper.assertTrue(rocketsOf(helper, inside).size() == 1, "a glider in the area got " + rocketsOf(helper, inside).size() + " rockets");
			helper.assertFalse(NorthWind.onCooldown(outside), "a glider outside the area got a gust");
			helper.assertFalse(NorthWind.onCooldown(walking), "a player not gliding got a gust");
			for (ServerPlayer p : new ServerPlayer[] {inside, outside, walking}) {
				level.getServer().getPlayerList().remove(p);
			}
			TotemAreas.remove(level, TotemAreas.Kind.NORTH_WIND, helper.absolutePos(statue));
		});
	}

	/** In a Reworked Beacons beacon (run with -PwithBeacons): the whole beacon area, sky high, gusts gliders. */
	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void beaconAreaGustsGliders(GameTestHelper helper) {
		if (!FabricLoader.getInstance().isModLoaded("bubbas_reworked_beacons")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos beacon = helper.absolutePos(new BlockPos(1, 1, 1));
		WithBeacons.pulse(level, beacon);
		BlockPos high = beacon.offset(15, 80, 0);
		helper.assertTrue(TotemAreas.all(level, TotemAreas.Kind.NORTH_WIND).stream().anyMatch(a -> a.box().contains(Vec3.atCenterOf(high))),
				"a 1-layer beacon's area doesn't reach 15 blocks out and 80 up");
		ServerPlayer player = glider(helper, new Vec3(12.5, 1.2, 8.5), true);
		helper.succeedWhen(() -> {
			helper.assertTrue(NorthWind.onCooldown(player), "a glider in the beacon's area got no gust");
			level.getServer().getPlayerList().remove(player);
			TotemAreas.remove(level, TotemAreas.Kind.NORTH_WIND, beacon);
		});
	}

	private static final class WithBeacons {
		static void pulse(ServerLevel level, BlockPos beacon) {
			var payload = new io.github.bubba8587.bubbas_totems_artifacts.compat.NorthWindBeaconPayload();
			ItemStack stack = new ItemStack(ModItems.TOTEM_OF_NORTH_WIND);
			if (!payload.accepts(stack) || payload.accepts(new ItemStack(ModItems.TOTEM_OF_CLEAR_SKIES))) {
				throw new AssertionError("the North Wind payload accepts the wrong items");
			}
			var context = new io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext(level, beacon,
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode.SKY, 1, payload.power(stack),
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext.skyArea(level, beacon, 1));
			payload.apply(level, stack, context);
		}
	}
}
