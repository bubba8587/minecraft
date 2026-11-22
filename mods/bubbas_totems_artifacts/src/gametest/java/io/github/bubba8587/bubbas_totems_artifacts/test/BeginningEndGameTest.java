// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.lang.reflect.Method;
import java.util.Map;

import io.github.bubba8587.bubbas_totems_artifacts.end.EndTeleport;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsLoot;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The Totems of the Beginning and the End (specs/totems-artifacts.md). The landing rules are core (through
 * BubbasTotemsArtifactsCoreGameTest); the rest is milestone. The cairn side (offering, the ripple
 * itself) is tested in Reworked Mobs' EndermanTotemsGameTest.
 */
public class BeginningEndGameTest {
	private static final String FIELD = TestSupport.FIELD;

	/**
	 * Core. The owner's landing rules: top face, stand on it; side face with room above, on top; side face without room,
	 * down to the floor in front; bottom face, down to the floor under it; fire on the spot, nowhere; nothing hit, nowhere.
	 */
	public void endLandingRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		try {
			for (int x = 3; x <= 22; x++) {
				for (int z = 3; z <= 7; z++) {
					helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				}
			}
			// Top face.
			helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
			expect(helper, player, new BlockPos(5, 1, 5), Direction.UP, new Vec3(5.5, 2.0, 5.5), "top face");
			// Side face, room above: on top.
			helper.setBlock(new BlockPos(8, 1, 5), Blocks.STONE);
			expect(helper, player, new BlockPos(8, 1, 5), Direction.NORTH, new Vec3(8.5, 2.0, 5.5), "side face with room");
			// Side face of a wall with no room above the block hit: down to the floor in front of it.
			for (int y = 1; y <= 4; y++) {
				helper.setBlock(new BlockPos(11, y, 5), Blocks.STONE);
			}
			expect(helper, player, new BlockPos(11, 3, 5), Direction.NORTH, new Vec3(11.5, 1.0, 4.5), "side face, no room");
			// A slab is footing: you stand on its top half.
			helper.setBlock(new BlockPos(14, 1, 5), Blocks.STONE_SLAB);
			expect(helper, player, new BlockPos(14, 1, 5), Direction.UP, new Vec3(14.5, 1.5, 5.5), "top of a slab");
			// Fire on top: not safe, nowhere to land.
			helper.setBlock(new BlockPos(17, 1, 5), Blocks.STONE);
			helper.setBlock(new BlockPos(17, 2, 5), Blocks.FIRE);
			expect(helper, player, new BlockPos(17, 1, 5), Direction.UP, null, "fire on top");
			// Bottom face of an overhang: down to the floor under it.
			helper.setBlock(new BlockPos(20, 3, 5), Blocks.STONE);
			expect(helper, player, new BlockPos(20, 3, 5), Direction.DOWN, new Vec3(20.5, 1.0, 5.5), "bottom face");
			// Nothing hit.
			helper.assertTrue(EndTeleport.landing(level, player, BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO)) == null,
					"a miss gave a landing spot");
		} finally {
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	private static void expect(GameTestHelper helper, Player player, BlockPos rel, Direction face, Vec3 relExpected, String what) {
		BlockPos abs = helper.absolutePos(rel);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
		Vec3 got = EndTeleport.landing(helper.getLevel(), player, hit);
		if (relExpected == null) {
			helper.assertTrue(got == null, what + ": expected nowhere, got " + got);
			return;
		}
		Vec3 want = helper.absoluteVec(relExpected);
		helper.assertTrue(got != null && got.distanceTo(want) < 1.0E-6, what + ": expected " + want + ", got " + got);
	}

	/** Using it while aiming at a block teleports there and starts the cooldown; aiming where there's nowhere to land does nothing. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void endTeleportsWhereAimedWithCooldown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		try {
			for (int x = 1; x <= 14; x++) {
				helper.setBlock(new BlockPos(x, 0, 10), Blocks.STONE);
			}
			helper.setBlock(new BlockPos(12, 1, 10), Blocks.STONE);
			Vec3 start = helper.absoluteVec(new Vec3(2.5, 1.0, 10.5));
			ItemStack totem = new ItemStack(ModItems.TOTEM_OF_THE_END);
			player.setItemInHand(InteractionHand.MAIN_HAND, totem);

			// At a block with fire on top: nowhere safe to land, nothing happens, no cooldown. (A plain miss, nothing in
			// range, gives no landing either: endLandingRules. The test field has a roof in range, so it can't be aimed at here.)
			helper.setBlock(new BlockPos(7, 1, 10), Blocks.STONE);
			helper.setBlock(new BlockPos(7, 2, 10), Blocks.FIRE);
			player.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
			player.lookAt(EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(7.5, 2.0, 10.5)));
			InteractionResult unsafe = totem.use(level, player, InteractionHand.MAIN_HAND);
			helper.assertTrue(unsafe == InteractionResult.FAIL, "aiming at a spot with nowhere to land returned " + unsafe);
			helper.assertFalse(player.getCooldowns().isOnCooldown(totem), "a failed use started the cooldown");
			helper.assertTrue(player.position().distanceTo(start) < 1.0E-6, "a failed use moved the player");
			helper.setBlock(new BlockPos(7, 2, 10), Blocks.AIR);
			helper.setBlock(new BlockPos(7, 1, 10), Blocks.AIR);

			// At the top of the block 10 away: there, with the cooldown.
			player.lookAt(EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(12.5, 2.0, 10.5)));
			InteractionResult used = totem.use(level, player, InteractionHand.MAIN_HAND);
			helper.assertTrue(used.consumesAction(), "using it on a block returned " + used);
			Vec3 want = helper.absoluteVec(new Vec3(12.5, 2.0, 10.5));
			helper.assertTrue(player.position().distanceTo(want) < 1.0E-6, "expected the player at " + want + ", was at " + player.position());
			helper.assertTrue(player.getCooldowns().isOnCooldown(totem), "no cooldown after a teleport");
			helper.assertTrue(totem.getCount() == 1, "the Totem of the End was used up");
		} finally {
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/** Holding a Totem of the End, looking an enderman in the eye isn't staring; without it, it is (vanilla). */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void endHeldMakesEyeContactHarmless(GameTestHelper helper) throws ReflectiveOperationException {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		Enderman enderman = helper.spawn(EntityTypes.ENDERMAN, new Vec3(12.5, 1.0, 8.5));
		enderman.setNoAi(true);
		try {
			Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 4.5));
			player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
			player.lookAt(EntityAnchorArgument.Anchor.EYES, enderman.getEyePosition());
			Method staredBy = Enderman.class.getDeclaredMethod("isBeingStaredBy", Player.class);
			staredBy.setAccessible(true);
			helper.assertTrue((boolean) staredBy.invoke(enderman, player), "control: looking it in the eye should count as staring");
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TOTEM_OF_THE_END));
			helper.assertFalse((boolean) staredBy.invoke(enderman, player), "holding the Totem of the End, eye contact still counts");
		} finally {
			level.getServer().getPlayerList().remove(player);
			enderman.discard();
		}
		helper.succeed();
	}

	/**
	 * Using a Totem of the Beginning: with Reworked Mobs it starts the ripple and is used up; without it nothing happens
	 * and it isn't used up.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void beginningUsedOnlyWithReworkedMobs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		try {
			Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));
			player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
			ItemStack totem = new ItemStack(ModItems.TOTEM_OF_THE_BEGINNING);
			player.setItemInHand(InteractionHand.MAIN_HAND, totem);
			InteractionResult result = totem.use(level, player, InteractionHand.MAIN_HAND);
			if (TestSupport.withMobs()) {
				helper.assertTrue(result.consumesAction() && totem.isEmpty(), "with Reworked Mobs it should be used up: " + result);
			} else {
				helper.assertTrue(result == InteractionResult.PASS && totem.getCount() == 1, "without Reworked Mobs it should do nothing: " + result);
			}
		} finally {
			level.getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/** The Totem of the End is in the End city treasure table at its chance; both are single-stack. */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void endFoundInEndCitiesAndSingleStack(GameTestHelper helper) {
		Map<?, Float> chances = TotemsLoot.all().get(ModItems.TOTEM_OF_THE_END);
		helper.assertTrue(chances != null && chances.equals(TotemsTuning.END_LOOT), "the Totem of the End isn't in the loot list");
		helper.assertTrue(new ItemStack(ModItems.TOTEM_OF_THE_END).getMaxStackSize() == 1
				&& new ItemStack(ModItems.TOTEM_OF_THE_BEGINNING).getMaxStackSize() == 1, "they should be single-stack");
		helper.succeed();
	}
}
