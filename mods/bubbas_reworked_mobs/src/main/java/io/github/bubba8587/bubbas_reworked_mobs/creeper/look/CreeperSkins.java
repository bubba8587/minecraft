// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper.look;

import java.util.Comparator;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.biome.Biome;

/** Biome creeper skins: picked once from the biome a creeper first appears in, saved and synced to clients. */
public final class CreeperSkins {
	/** Biome tag folder: data/<ns>/tags/worldgen/biome/creeper_skin/<skin>.json. */
	public static final String TAG_PREFIX = "creeper_skin/";
	/** Saved for creepers whose biome has no skin. */
	public static final Identifier VANILLA = BubbasReworkedMobs.id("vanilla");

	public static final AttachmentType<Identifier> SKIN = AttachmentRegistry.create(BubbasReworkedMobs.id("creeper_skin"),
			builder -> builder.persistent(Identifier.CODEC).syncWith(Identifier.STREAM_CODEC, AttachmentSyncPredicate.all()));

	private CreeperSkins() {
	}

	public static void initialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Creeper creeper && !creeper.hasAttached(SKIN)) {
				pick(creeper);
			}
		});
	}

	/** The skin for a biome: its creeper_skin/ tag (first by id when several match), else {@link #VANILLA}. */
	public static Identifier skinFor(Holder<Biome> biome) {
		return biome.tags()
				.map(TagKey::location)
				.filter(id -> id.getPath().startsWith(TAG_PREFIX) && id.getPath().length() > TAG_PREFIX.length())
				.min(Comparator.comparing(Identifier::toString))
				.map(id -> id.withPath(id.getPath().substring(TAG_PREFIX.length())))
				.orElse(VANILLA);
	}

	public static void pick(Creeper creeper) {
		creeper.setAttached(SKIN, skinFor(creeper.level().getBiome(creeper.blockPosition())));
	}

	public static Identifier skin(Creeper creeper) {
		return creeper.getAttachedOrElse(SKIN, VANILLA);
	}
}
