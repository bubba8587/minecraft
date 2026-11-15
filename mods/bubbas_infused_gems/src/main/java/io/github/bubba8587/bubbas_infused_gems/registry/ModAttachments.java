// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import java.util.UUID;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import net.minecraft.core.UUIDUtil;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Data attachments. {@link #SLIME_OWNER}: on a slime split off by an Oozing weapon, the player (or mob) it belongs to; saved. */
public final class ModAttachments {
	public static final AttachmentType<UUID> SLIME_OWNER = AttachmentRegistry.createPersistent(BubbasInfusedGems.id("slime_owner"), UUIDUtil.CODEC);

	private ModAttachments() {
	}

	public static void initialize() {
	}
}
