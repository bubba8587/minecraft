// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement;
import io.github.bubba8587.bubbas_reworked_mobs.display.HandPlacing;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnTrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Trading at cairns and laying items on the ground. */
public class CairnTradeGameTest {
	/** Core. An item on a cairn's trade pad is taken, and an answer is laid there; the answer isn't taken again. */
	public void tradePadTakesAndAnswers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CairnSite site = CairnSites.addSite(level, helper.absolutePos(new BlockPos(16, 1, 8)), 1234L);
		site.buildAll(level);
		BlockPos spot = site.tradeSpot();
		helper.assertTrue(spot != null, "the cairn has no trade pad");
		helper.assertTrue(level.getBlockState(spot.below()).is(Blocks.PURPUR_BLOCK), "pad centre is " + level.getBlockState(spot.below()));
		helper.assertTrue(site.at(spot) == null, "the item spot is a design block");
		LootDisplays.place(level, spot, DisplayPlacement.GROUND, Direction.NORTH, new ItemStack(Items.ROTTEN_FLESH));
		helper.assertTrue(CairnTrades.step(level, site) == CairnTrades.Step.STARING, "no enderman came to stare");
		java.util.List<Enderman> takers = level.getEntitiesOfClass(Enderman.class, new AABB(spot).inflate(4.0),
				e -> e.entityTags().contains(CairnTrades.TAKER_TAG));
		helper.assertTrue(takers.size() == 1, takers.size() + " staring endermen");
		Enderman taker = takers.getFirst();
		net.minecraft.world.phys.Vec3 toItem = new net.minecraft.world.phys.Vec3(spot.getX() + 0.5, spot.getY() + 0.1, spot.getZ() + 0.5)
				.subtract(taker.getEyePosition()).normalize();
		helper.assertTrue(taker.getViewVector(1.0F).dot(toItem) > 0.98, "the enderman isn't looking at the item: view " + taker.getViewVector(1.0F)
				+ ", to the item " + toItem);
		helper.assertTrue(CairnTrades.step(level, site) == CairnTrades.Step.STARING, "taken before the stare was over");
		helper.assertTrue(LootDisplays.hasItemAt(level, spot), "the item went while it stared");
		CairnTrades.hurryStare(level, site);
		helper.assertTrue(CairnTrades.step(level, site) == CairnTrades.Step.TAKEN, "the item wasn't taken");
		helper.assertFalse(LootDisplays.hasItemAt(level, spot), "the item still lies there");
		helper.assertFalse(taker.isAlive(), "the staring enderman stayed");
		helper.assertTrue(CairnTrades.step(level, site) == CairnTrades.Step.NONE, "answered before its time");
		CairnTrades.hurry(level, site);
		helper.assertTrue(CairnTrades.step(level, site) == CairnTrades.Step.ANSWERED, "no answer");
		helper.assertTrue(LootDisplays.hasItemAt(level, spot), "the answer isn't on the pad");
		helper.assertTrue(CairnTrades.step(level, site) == CairnTrades.Step.NONE, "the answer was taken back");
		LootDisplays.takeForMob(level, spot);
		helper.assertTrue(CairnTrades.step(level, site) == CairnTrades.Step.NONE, "an empty pad did something");
		for (Enderman e : level.getEntitiesOfClass(Enderman.class, new AABB(spot).inflate(8.0))) {
			e.discard();
		}
		EndermanGameTest.removeBuilt(level, site);
		helper.succeed();
	}

	/** Different cairns put their trade pads in different places around them, including nearby seeds; every form gets one. */
	@GameTest(maxTicks = 5)
	public void tradePadsSitInDifferentPlaces(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		java.util.Set<BlockPos> offsets = new java.util.HashSet<>();
		java.util.Set<io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnDesign.Form> forms = java.util.EnumSet.noneOf(
				io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnDesign.Form.class);
		java.util.Set<io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnDesign.Form> padded = java.util.EnumSet.noneOf(
				io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnDesign.Form.class);
		BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
		int seeds = 0;
		for (long seed : new long[] {11L, 22L, 33L, 44L, 1234L, 2026L, 7L, 8L, 9L, 10L}) {
			CairnSite site = CairnSites.addSite(level, origin, seed);
			var form = io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnDesign.formOf(seed);
			forms.add(form);
			if (site.tradeSpot() != null) {
				offsets.add(site.tradeSpot().subtract(origin));
				padded.add(form);
				seeds++;
			}
			CairnSites.removeSite(level, site);
		}
		helper.assertTrue(offsets.size() >= seeds - 1, "only " + offsets.size() + " pad places for " + seeds + " cairns: " + offsets);
		for (long i = 100; i < 2000 && padded.size() < 7; i++) {
			long seed = net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(i);
			var form =io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnDesign.formOf(seed);
			if (padded.contains(form)) {
				continue;
			}
			CairnSite site = CairnSites.addSite(level, origin, seed);
			forms.add(form);
			if (site.tradeSpot() != null) {
				padded.add(form);
			}
			CairnSites.removeSite(level, site);
		}
		helper.assertTrue(padded.size() == 7, "forms with a pad: " + padded);
		helper.succeed();
	}

	/** Every answer is one real item. */
	@GameTest(maxTicks = 5)
	public void tradeTableAlwaysAnswers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Enderman by = EntityTypes.ENDERMAN.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
		java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
		for (int i = 0; i < 400; i++) {
			ItemStack answer = CairnTrades.roll(level, by);
			helper.assertFalse(answer.isEmpty(), "an empty answer");
			seen.add(answer.getItem());
		}
		helper.assertTrue(seen.size() > 15, "only " + seen.size() + " kinds of answer in 400 rolls");
		helper.succeed();
	}

	/** A held item is laid on the ground as a display, one at a time, only over a floor. */
	@GameTest(structure = EndermanGameTest.FIELD, maxTicks = 5)
	public void heldItemLaysOnTheGround(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 1, 4));
		ItemStack held = new ItemStack(Items.APPLE, 3);
		BlockPos onFloor = helper.absolutePos(new BlockPos(5, 1, 4));
		helper.assertTrue(HandPlacing.lay(level, zombie, onFloor, held), "nothing laid on the floor");
		helper.assertTrue(held.getCount() == 2, "the stack holds " + held.getCount());
		helper.assertTrue(level.getBlockEntity(onFloor) instanceof LootDisplayBlockEntity d && d.getItem().is(Items.APPLE) && d.getItem().getCount() == 1,
				"the display doesn't hold one apple");
		helper.assertFalse(HandPlacing.lay(level, zombie, onFloor, held), "laid on top of a display");
		helper.assertFalse(HandPlacing.lay(level, zombie, helper.absolutePos(new BlockPos(5, 6, 4)), held), "laid in mid-air");
		LootDisplays.takeForMob(level, onFloor);
		zombie.discard();
		helper.succeed();
	}
}
