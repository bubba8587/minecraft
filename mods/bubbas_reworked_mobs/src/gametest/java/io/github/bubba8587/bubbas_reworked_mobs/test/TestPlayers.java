// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Real players for server game tests, shared by every test class. The helper's own in-level mock player is creative,
 * which monsters, Wardens and debuffs ignore; these are connected players on the server's player list (so messages,
 * the player list and targeting work). Take each one off again with {@link #remove}: a failed test leaves its players
 * in the level otherwise.
 */
final class TestPlayers {
	private TestPlayers() {
	}

	/** A survival player in the level, at the level's spawn until moved. */
	static ServerPlayer survival(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-survivor"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
			@Override
			public GameType gameMode() {
				return GameType.SURVIVAL;
			}
		};
		join(level, player, cookie);
		GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
		return player;
	}

	/** A survival player standing on the floor at (x, 1, z) in test coordinates, facing {@code yRot}. */
	static ServerPlayer survivalAt(GameTestHelper helper, double x, double z, float yRot) {
		ServerPlayer player = survival(helper);
		Vec3 abs = helper.absoluteVec(new Vec3(x, 1.0, z));
		player.snapTo(abs.x, abs.y, abs.z, yRot, 0.0F);
		player.setYHeadRot(yRot);
		player.setOnGround(true);
		return player;
	}

	static ServerPlayer survivalAt(GameTestHelper helper, double x, double z) {
		return survivalAt(helper, x, z, 0.0F);
	}

	/** A connected player in {@code mode} (the game mode set the normal way, so it can be changed later). */
	static ServerPlayer connected(GameTestHelper helper, GameType mode) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-player"), false);
		ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		join(level, player, cookie);
		player.setGameMode(mode);
		return player;
	}

	static void remove(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static void join(ServerLevel level, ServerPlayer player, CommonListenerCookie cookie) {
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
	}
}
