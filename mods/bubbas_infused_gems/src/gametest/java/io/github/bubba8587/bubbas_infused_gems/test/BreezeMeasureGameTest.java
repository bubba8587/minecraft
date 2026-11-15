// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.github.bubba8587.bubbas_infused_gems.registry.ModCommands;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.DispenserInvoker;
import io.github.bubba8587.bubbas_infused_gems.test.mixin.ProjectileAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Owner playtest: a perfect Breeze set (as /gems kit breeze gives) deflected about half of the arrows that reached the
 * wearer (measured 47-51 %); the owner then raised it (2026-11-15): the perfect set deflects every one. Real arrows from a dispenser and a skeleton, ticked through vanilla's own arrow movement and hit code.
 */
public class BreezeMeasureGameTest {
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("bubbas_infused_gems-test");

	/** Arrows that reached the wearer, deflected and never hurt it, deflected but hurt it anyway, hit without a deflection. */
	private record Tally(int reached, int deflected, int deflectedThenHit, int hit, int rolls) {
		double rate() {
			return deflected / (double) Math.max(reached, 1);
		}

		@Override
		public String toString() {
			return "%d reached, %d deflected (%.1f%%), %d deflected but hit anyway, %d hit, %.2f deflect checks per arrow".formatted(
					reached, deflected, 100 * rate(), deflectedThenHit, hit, rolls / (double) Math.max(reached, 1));
		}
	}

	private static Tally shoot(GameTestHelper helper, ServerPlayer player, int arrows, Supplier<AbstractArrow> fire) {
		int reached = 0;
		int deflected = 0;
		int deflectedThenHit = 0;
		int hit = 0;
		int rolls = 0;
		ProbeState.watched = player;
		for (int n = 0; n < arrows; n++) {
			player.setHealth(player.getMaxHealth());
			player.damageCooldownTime = 0;
			player.hurtTime = 0;
			ProbeState.CALLS.clear();
			AbstractArrow arrow = fire.get();
			boolean hurt = false;
			for (int t = 0; t < 120 && !arrow.isRemoved(); t++) {
				float before = player.getHealth();
				arrow.tick();
				if (player.getHealth() < before) {
					hurt = true;
				}
				player.damageCooldownTime = 0;
			}
			int calls = ProbeState.CALLS.getOrDefault(arrow.getId(), 0);
			boolean wasDeflected = ((ProjectileAccessor) arrow).bubbas$lastDeflectedBy() == player;
			arrow.discard();
			if (calls == 0 && !hurt) {
				continue;
			}
			reached++;
			rolls += calls;
			if (wasDeflected && !hurt) {
				deflected++;
			} else if (wasDeflected) {
				deflectedThenHit++;
			} else {
				hit++;
			}
		}
		ProbeState.watched = null;
		return new Tally(reached, deflected, deflectedThenHit, hit, rolls);
	}

	private static ServerPlayer wearer(GameTestHelper helper, Vec3 at) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 abs = helper.absoluteVec(at);
		player.teleportTo(abs.x, abs.y, abs.z);
		try {
			ModCommands.kit(player.createCommandSourceStack(), "breeze", "netherite");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return player;
	}

	@GameTest(maxTicks = 200)
	public void perfectBreezeDeflectsDispenserArrows(GameTestHelper helper) {
		ServerPlayer player = wearer(helper, new Vec3(1.5, 1, 4.5));
		BlockPos rel = new BlockPos(1, 2, 1);
		helper.setBlock(rel, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
		BlockPos pos = helper.absolutePos(rel);
		BlockState state = helper.getLevel().getBlockState(pos);
		DispenserBlockEntity box = (DispenserBlockEntity) helper.getLevel().getBlockEntity(pos);
		Set<Integer> seen = new HashSet<>();
		AABB area = new AABB(pos).inflate(3);
		Tally tally = shoot(helper, player, 300, () -> {
			box.setItem(0, new ItemStack(Items.ARROW, 64));
			((DispenserInvoker) Blocks.DISPENSER).bubbas$dispense(helper.getLevel(), state, pos);
			List<AbstractArrow> fresh = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, area, a -> !seen.contains(a.getId()));
			AbstractArrow arrow = fresh.getFirst();
			seen.add(arrow.getId());
			return arrow;
		});
		LOG.info("Breeze vs dispenser: {} (set strength {} of cap {})", tally, io.github.bubba8587.bubbas_infused_gems.effect.GemValues.value(player, "breeze"), io.github.bubba8587.bubbas_infused_gems.balance.Balance.get().cap);
		check(helper, tally.reached() > 200, "dispenser arrows reaching the wearer: " + tally);
		check(helper, tally.deflected() == tally.reached(), "perfect Breeze set (owner: always deflects) vs dispenser arrows: " + tally);
		GemKit.remove(helper, player);
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void perfectBreezeDeflectsSkeletonArrows(GameTestHelper helper) {
		ServerPlayer player = wearer(helper, new Vec3(1.5, 1, 6.5));
		Skeleton skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(1, 1, 1));
		skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		Set<Integer> seen = new HashSet<>();
		Tally tally = shoot(helper, player, 100, () -> {
			skeleton.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
			skeleton.performRangedAttack(player, 1.0F);
			List<AbstractArrow> fresh = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, skeleton.getBoundingBox().inflate(3),
					a -> !seen.contains(a.getId()));
			AbstractArrow arrow = fresh.getFirst();
			seen.add(arrow.getId());
			return arrow;
		});
		LOG.info("Breeze vs skeleton: {}", tally);
		check(helper, tally.reached() > 50, "skeleton arrows reaching the wearer: " + tally);
		check(helper, tally.deflected() == tally.reached(), "perfect Breeze set (owner: always deflects) vs skeleton arrows: " + tally);
		skeleton.discard();
		GemKit.remove(helper, player);
		helper.succeed();
	}
}
