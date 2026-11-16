// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import net.fabricmc.loader.api.FabricLoader;

/** Shared bits for this mod's server game tests. */
final class TestSupport {
	static final String NS = "bubbas_totems_artifacts-test";
	static final String FIELD = NS + ":hearing_field";
	static final String LANE = NS + ":drift_lane";

	private TestSupport() {
	}

	/** Whether Bubba's Reworked Mobs is on the game's classpath (-PwithMobs): the sleeper tests run for real only then. */
	static boolean withMobs() {
		return FabricLoader.getInstance().isModLoaded("bubbas_reworked_mobs");
	}

	/** A real survival-mode player on a fake connection (remove it from the player list when done). */
	static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-survivor"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}
		};
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		return player;
	}

	/**
	 * Reworked Mobs' sleepers, for the tests that need them. Touches that mod's classes, so it is only loaded when
	 * {@link #withMobs()} is true.
	 */
	static final class Mobs {
		private Mobs() {
		}

		/** Places a sitting skeleton sleeper facing east at {@code pos} (relative). */
		static void sleeper(GameTestHelper helper, BlockPos pos) {
			helper.setBlock(pos, io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks.sleeper(
					io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant.SKELETON).defaultBlockState()
					.setValue(io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock.POSE,
							io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose.SITTING)
					.setValue(io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock.FACING, Direction.EAST));
		}

		private static io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity at(GameTestHelper helper, BlockPos pos) {
			return helper.getBlockEntity(pos, io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.class);
		}

		static void disturb(GameTestHelper helper, BlockPos pos, float points) {
			at(helper, pos).disturb(helper.getLevel().getGameTime(), points);
		}

		static float disturbance(GameTestHelper helper, BlockPos pos) {
			return at(helper, pos).getDisturbance(helper.getLevel().getGameTime());
		}

		/** What noise is multiplied by at the sleeper now (0: kept asleep). */
		static float sensitivity(GameTestHelper helper, BlockPos pos) {
			return at(helper, pos).effectiveSensitivity(helper.getLevel());
		}

		/** Never wakes during the test. */
		static void deaf(GameTestHelper helper, BlockPos pos) {
			at(helper, pos).setSensitivity(0.0F);
		}

		static boolean keptAsleep(Level level, BlockPos absolute) {
			return io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers.isKeptAsleep(level, absolute);
		}
	}
}
