// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.skies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** The Totem of Clear Skies' areas. */
public final class ClearSkies {
	/** Clear Skies areas of one dimension, as columns. */
	public record AreasPayload(String dimension, List<Integer> columns) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<AreasPayload> TYPE = new CustomPacketPayload.Type<>(BubbasTotemsArtifacts.id("clear_skies"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AreasPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, AreasPayload::dimension,
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), AreasPayload::columns,
				AreasPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final int[] NONE = new int[0];
	private static final Map<ServerLevel, int[]> SERVER = new WeakHashMap<>();
	private static volatile String clientDimension = "";
	private static volatile int[] client = NONE;
	private static volatile long clientReceived;

	private ClearSkies() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(AreasPayload.TYPE, AreasPayload.CODEC);
		ServerTickEvents.END_LEVEL_TICK.register(ClearSkies::tick);
	}

	private static void tick(ServerLevel level) {
		List<TotemAreas.Area> areas = TotemAreas.all(level, TotemAreas.Kind.CLEAR_SKIES);
		int[] before = SERVER.getOrDefault(level, NONE);
		if (areas.isEmpty() && before.length == 0) {
			return;
		}
		int[] now = new int[areas.size() * 4];
		int i = 0;
		for (TotemAreas.Area area : areas) {
			AABB box = area.box();
			now[i++] = (int) Math.floor(box.minX);
			now[i++] = (int) Math.floor(box.minZ);
			now[i++] = (int) Math.ceil(box.maxX) - 1;
			now[i++] = (int) Math.ceil(box.maxZ) - 1;
		}
		SERVER.put(level, now);
		if (now.length > 0 && level.getGameTime() % 20 == 0 || now.length == 0) {
			List<Integer> columns = new ArrayList<>(now.length);
			for (int c : now) {
				columns.add(c);
			}
			AreasPayload payload = new AreasPayload(level.dimension().identifier().toString(), columns);
			for (ServerPlayer player : PlayerLookup.level(level)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	/** Client: store received areas. */
	public static void received(AreasPayload payload) {
		int[] columns = new int[payload.columns().size()];
		for (int i = 0; i < columns.length; i++) {
			columns[i] = payload.columns().get(i);
		}
		client = columns;
		clientDimension = payload.dimension();
		clientReceived = System.currentTimeMillis();
	}

	/** Client: forget areas. */
	public static void clearClient() {
		client = NONE;
		clientDimension = "";
	}

	/** Whether column (x, z) is kept clear of weather. */
	public static boolean clearAt(Level level, int x, int z) {
		int[] columns;
		if (level instanceof ServerLevel server) {
			columns = SERVER.getOrDefault(server, NONE);
		} else {
			columns = client;
			if (columns.length == 0 || System.currentTimeMillis() - clientReceived > 3000L
					|| !clientDimension.equals(level.dimension().identifier().toString())) {
				return false;
			}
		}
		for (int i = 0; i + 3 < columns.length; i += 4) {
			if (x >= columns[i] && z >= columns[i + 1] && x <= columns[i + 2] && z <= columns[i + 3]) {
				return true;
			}
		}
		return false;
	}
}
