// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import io.github.bubba8587.bubbas_infused_gems.effect.GemHits;
import io.github.bubba8587.bubbas_infused_gems.effect.GemMobEffect;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The owner's answers to the strength proposal (2026-11-15): one dial per effect. Potency hits linger 4-8 s; Nausea,
 * Glowing and Levitation durations set per build; Levitation hits rise at a speed set per build; Weaving and Ender hits
 * by chance; Wind Burst has a visible floor; worn Breeze lets wind charges through; a Breeze hit makes shots veer.
 */
public class StrengthAnswersGameTest {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	/** A perfect Levitation hit lifts a pig fast: about 1.8 blocks/s once up to speed. */
	@GameTest(maxTicks = 100)
	public void levitationHitRisesAtItsSpeed(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 2, 1));
		GemHits.apply(helper.getLevel(), pig, null, null, List.of(new GemHits.Entry("levitation", Balance.get().cap, false)));
		check(helper, pig.hasEffect(ModEffects.gem("levitation")), "no Levitation gem effect from the hit");
		double y0 = pig.getY();
		helper.runAfterDelay(40, () -> {
			double rose = pig.getY() - y0;
			check(helper, rose > 2.0 && rose < 4.5, "a perfect Levitation hit rose " + rose + " blocks in 2 s");
			pig.discard();
			helper.succeed();
		});
	}

	/** A perfect Ender weapon blinks its target away about 40 % of the time, with chorus fruit's teleport. */
	@GameTest
	public void enderHitBlinksTheTarget(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 2, 1));
		Vec3 home = pig.position();
		int moved = 0;
		for (int i = 0; i < 300; i++) {
			pig.teleportTo(home.x, home.y, home.z);
			GemHits.apply(helper.getLevel(), pig, null, null, List.of(new GemHits.Entry("ender", Balance.get().cap, false)));
			if (pig.position().distanceTo(home) > 0.5) {
				moved++;
			}
		}
		check(helper, moved > 300 * 0.28 && moved < 300 * 0.52, "a perfect Ender weapon blinked " + moved + " of 300 targets, owner 40 %");
		pig.discard();
		helper.succeed();
	}

	/** Worn Breeze lets wind charges through, as a real breeze does; a Breeze hit makes the target's shots veer. */
	@GameTest
	public void breezeRulesMatchVanilla(GameTestHelper helper) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 2, 1));
		Gem g = GemKit.gem("breeze", 1.0);
		for (int i = 0; i < 4; i++) {
			pig.setItemSlot(GemTotals.ARMOR[i], GemKit.socketed(BalanceGameTest.armor("netherite", PIECES[i]), g, g, g));
		}
		WornEffects.refresh(pig);
		var charge = EntityTypes.WIND_CHARGE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		check(helper, pig.deflection(charge) == ProjectileDeflection.NONE, "a perfect Breeze set deflected a wind charge");
		Mob skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(3, 2, 3));
		check(helper, GemActions.veer(skeleton) == 1.0, "an unaffected skeleton veers");
		skeleton.addEffect(new MobEffectInstance(ModEffects.gem("breeze"), 100, GemMobEffect.amplifier(1.0)));
		check(helper, Math.abs(GemActions.veer(skeleton) - 12.0) < 1e-6, "a perfect Breeze hit veers x" + GemActions.veer(skeleton));
		check(helper, pig.deflection(EntityTypes.ARROW.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND)) != ProjectileDeflection.NONE,
				"a perfect Breeze set let an arrow through");
		pig.discard();
		skeleton.discard();
		helper.succeed();
	}

	/** A perfect Weaving weapon at 50 % drops 2-3 webs around the target on a hit that procs. */
	@GameTest
	public void perfectWeavingHitDropsSeveralWebs(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 1, 2));
		int webs = 0;
		for (int i = 0; i < 40 && webs == 0; i++) {
			GemHits.apply(helper.getLevel(), pig, null, null, List.of(new GemHits.Entry("weaving", Balance.get().cap, false)));
			webs = 0;
			for (BlockPos at : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(4, 3, 4)))) {
				if (helper.getLevel().getBlockState(at).is(Blocks.COBWEB)) {
					webs++;
				}
			}
		}
		check(helper, webs >= 2 && webs <= 3, "a perfect Weaving hit dropped " + webs + " webs, owner 2-3");
		pig.discard();
		helper.succeed();
	}
}
