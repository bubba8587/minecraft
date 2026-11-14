// dte:B5,B6
package io.github.bubba8587.refmod.registry;

import com.mojang.serialization.Codec;
import io.github.bubba8587.refmod.RefMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Data attachment: per-player count of lamp toggles; saved, kept on death, synced to the owning player. */
public final class ModAttachments {
	public static final AttachmentType<Integer> LAMP_TOGGLES = AttachmentRegistry.create(RefMod.id("lamp_toggles"), builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

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
