// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.seeking;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;

/**
 * What a Totem of Seeking can seek, and the block that chooses each (specs/totems-artifacts.md): use the totem on a
 * structure's signature block and it seeks the nearest structure of that type. The list is the builder's proposal; the
 * owner may prefer another way of choosing. Each type is a vanilla structure tag or a list of vanilla structures.
 */
public final class SeekingTypes {
	/** One seekable type. */
	public record SeekType(Identifier id, @Nullable TagKey<Structure> tag, List<ResourceKey<Structure>> structures, List<Block> blocks) {
		public Component displayName() {
			return Component.translatable("seeking." + BubbasTotemsArtifacts.MOD_ID + "." + this.id.getPath());
		}

		/** The structures this type covers in this world (empty when none exist, e.g. a datapack removed them). */
		public HolderSet<Structure> resolve(RegistryAccess access) {
			HolderGetter<Structure> lookup = access.lookupOrThrow(Registries.STRUCTURE);
			if (this.tag != null) {
				return lookup.get(this.tag).<HolderSet<Structure>>map(set -> set).orElse(HolderSet.empty());
			}
			List<Holder<Structure>> holders = new ArrayList<>();
			for (ResourceKey<Structure> key : this.structures) {
				lookup.get(key).ifPresent(holders::add);
			}
			return HolderSet.direct(holders);
		}
	}

	public static final List<SeekType> ALL = List.of(
			tag("village", "village", Blocks.HAY_BLOCK, Blocks.BELL),
			of("desert_pyramid", List.of("desert_pyramid"), Blocks.CHISELED_SANDSTONE),
			of("jungle_temple", List.of("jungle_pyramid"), Blocks.MOSSY_COBBLESTONE),
			of("swamp_hut", List.of("swamp_hut"), Blocks.CAULDRON),
			of("igloo", List.of("igloo"), Blocks.SNOW_BLOCK),
			of("pillager_outpost", List.of("pillager_outpost"), Blocks.DARK_OAK_LOG),
			of("woodland_mansion", List.of("mansion"), Blocks.DARK_OAK_PLANKS),
			of("ocean_monument", List.of("monument"), Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE, Blocks.SEA_LANTERN),
			tag("ruined_portal", "ruined_portal", Blocks.CRYING_OBSIDIAN),
			tag("mineshaft", "mineshaft", Blocks.RAIL),
			tag("abandoned_camp", "abandoned_camp", Blocks.CAMPFIRE),
			of("trail_ruins", List.of("trail_ruins"), Blocks.DECORATED_POT),
			of("stronghold", List.of("stronghold"), Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS),
			of("trial_chambers", List.of("trial_chambers"), Blocks.TUFF_BRICKS, Blocks.CHISELED_TUFF_BRICKS),
			of("ancient_city", List.of("ancient_city"), Blocks.REINFORCED_DEEPSLATE, Blocks.SCULK_SHRIEKER),
			of("nether_fortress", List.of("fortress"), Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE),
			of("bastion_remnant", List.of("bastion_remnant"), Blocks.GILDED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS),
			of("end_city", List.of("end_city"), Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR));

	private SeekingTypes() {
	}

	private static SeekType tag(String id, String tag, Block... blocks) {
		return new SeekType(BubbasTotemsArtifacts.id(id), TagKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace(tag)), List.of(), List.of(blocks));
	}

	private static SeekType of(String id, List<String> structures, Block... blocks) {
		return new SeekType(BubbasTotemsArtifacts.id(id), null,
				structures.stream().map(s -> ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace(s))).toList(), List.of(blocks));
	}

	/** The type whose signature block {@code state} is, if any. */
	public static Optional<SeekType> forBlock(BlockState state) {
		return ALL.stream().filter(t -> t.blocks().stream().anyMatch(state::is)).findFirst();
	}

	public static Optional<SeekType> byId(@Nullable Identifier id) {
		return id == null ? Optional.empty() : ALL.stream().filter(t -> t.id().equals(id)).findFirst();
	}

	/** Every signature block, for docs and tests. */
	public static Stream<Block> allBlocks() {
		return ALL.stream().flatMap(t -> t.blocks().stream());
	}
}
