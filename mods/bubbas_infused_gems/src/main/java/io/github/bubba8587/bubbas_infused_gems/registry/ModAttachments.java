// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import java.util.UUID;

import com.mojang.serialization.Codec;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import net.minecraft.core.UUIDUtil;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Data attachments. {@link #SLIME_OWNER}: on a slime split off by an Oozing weapon, the player (or mob) it belongs to; saved. */
public final class ModAttachments {
	public static final AttachmentType<UUID> SLIME_OWNER = AttachmentRegistry.createPersistent(BubbasInfusedGems.id("slime_owner"), UUIDUtil.CODEC);
	/** On a wearer: game time of the last Absorption equip-fill (saved, kept on death, so rejoining or respawning can't refill). */
	public static final AttachmentType<Long> LAST_EQUIP_FILL = AttachmentRegistry.create(BubbasInfusedGems.id("last_equip_fill"),
			builder -> builder.persistent(Codec.LONG).copyOnDeath());
	/**
	 * Gems received from Reworked Beacons (GemBeacons), with their end times; not saved (a pulse renews them), synced to the
	 * player itself so its client-side gem effects (gravity, sight, HUD) see them.
	 */
	public static final AttachmentType<java.util.List<io.github.bubba8587.bubbas_infused_gems.effect.GemBeacons.Received>> BEACON_GEMS =
			AttachmentRegistry.create(BubbasInfusedGems.id("beacon_gems"), builder -> builder.syncWith(
					io.github.bubba8587.bubbas_infused_gems.effect.GemBeacons.Received.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list()),
					net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.targetOnly()));
	/** On a mob an Ender hit blinked away: game time until which it takes no new target (owner); not saved. */
	public static final AttachmentType<Long> ENDER_CONFUSED_UNTIL = AttachmentRegistry.create(BubbasInfusedGems.id("ender_confused_until"));


	private ModAttachments() {
	}

	public static void initialize() {
	}
}
