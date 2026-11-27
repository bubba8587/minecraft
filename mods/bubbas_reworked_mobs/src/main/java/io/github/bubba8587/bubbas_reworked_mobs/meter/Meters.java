// dte:C1,B19
package io.github.bubba8587.bubbas_reworked_mobs.meter;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Visible meters (owner, 2026-11-16; game rule {@code bubbas_reworked_mobs:show_meters}, off by default): a mob's meter
 * value is sent to the players tracking it, only while the rule is on and only when its whole-number value changes
 * (a 0 is sent once when it empties, so the client drops it). The client draws a drowned's grip blue (above its head,
 * and on the held player's screen) and a creeper's startle meter red (above its head).
 */
public final class Meters {
	/** A drowned's grip on its victim (blue). */
	public static final byte GRIP = 0;
	/** A creeper's startle meter (red). */
	public static final byte STARTLE = 1;

	private Meters() {
	}

	/** S2C: entity {@code id}'s meter of {@code kind} now reads {@code value} (0-100; 0: gone). */
	public record MeterPayload(int id, byte kind, byte value) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<MeterPayload> TYPE = new CustomPacketPayload.Type<>(BubbasReworkedMobs.id("meter"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MeterPayload> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, MeterPayload::id,
				ByteBufCodecs.BYTE, MeterPayload::kind,
				ByteBufCodecs.BYTE, MeterPayload::value,
				MeterPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(MeterPayload.TYPE, MeterPayload.CODEC);
	}

	/**
	 * Sends {@code entity}'s meter if its shown value differs from {@code lastSent} (what the caller sent before; start
	 * at 0) and returns the new last-sent value. With the rule off the shown value is 0, so a meter already showing is
	 * cleared once and then nothing more is sent.
	 */
	public static int sync(Entity entity, byte kind, float value, int lastSent, float full) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return lastSent;
		}
		int shown = level.getGameRules().get(ModGameRules.SHOW_METERS) ? Math.clamp(Math.round(value / full * 100.0F), 0, 100) : 0;
		if (shown == lastSent) {
			return lastSent;
		}
		MeterPayload payload = new MeterPayload(entity.getId(), kind, (byte) shown);
		for (ServerPlayer player : PlayerLookup.tracking(entity)) {
			ServerPlayNetworking.send(player, payload);
		}
		return shown;
	}
}
