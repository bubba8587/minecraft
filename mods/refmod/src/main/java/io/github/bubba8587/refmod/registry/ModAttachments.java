// dte:A2,A4
package io.github.bubba8587.refmod.registry;

import java.util.UUID;

import com.mojang.serialization.Codec;
import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.blockdata.BarrelTallies;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Data attachments. {@link #LAMP_TOGGLES}: per-player count of lamp toggles; saved, kept on death, synced to the owning player.
 * {@link #CHARM_DISPLAY} and {@link #BARREL_TALLIES}: the no-new-blocks patterns (spec requirement 17, B15 fewNewBlocks).
 */
public final class ModAttachments {
	public static final AttachmentType<Integer> LAMP_TOGGLES = AttachmentRegistry.create(RefMod.id("lamp_toggles"), builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	/**
	 * On a lantern charm's {@code minecraft:interaction} hitbox: the UUID of the {@code minecraft:item_display} it belongs to.
	 * Saved with the entity; its presence is also what marks an interaction entity as ours. Not synced: only the server needs it.
	 */
	public static final AttachmentType<UUID> CHARM_DISPLAY = AttachmentRegistry.createPersistent(RefMod.id("charm_display"), UUIDUtil.CODEC);

	/**
	 * On a chunk: per-position tally counters for vanilla barrels in that chunk. Saved with the chunk (key {@code fabric:attachments}
	 * in the chunk NBT) and synced to every client tracking the chunk.
	 */
	public static final AttachmentType<BarrelTallies> BARREL_TALLIES = AttachmentRegistry.create(RefMod.id("barrel_tallies"), builder -> builder
			.persistent(BarrelTallies.CODEC)
			.syncWith(BarrelTallies.STREAM_CODEC, AttachmentSyncPredicate.all()));

	private ModAttachments() {
	}

	public static int getLampToggles(Player player) {
		return player.getAttachedOrElse(LAMP_TOGGLES, 0);
	}

	public static int incrementLampToggles(Player player) {
		int next = getLampToggles(player) + 1;
		player.setAttached(LAMP_TOGGLES, next);
		return next;
	}

	public static void initialize() {
	}
}
