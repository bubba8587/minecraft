// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.seeking;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jspecify.annotations.Nullable;

/** What a Totem of Seeking can be sent to: every findable structure and every biome of a dimension. */
public final class SeekKinds {
	/** One structure (by id) or biome (by id). */
	public record Kind(Identifier id, boolean biome) {
		public static final Codec<Kind> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("id").forGetter(Kind::id),
				Codec.BOOL.fieldOf("biome").forGetter(Kind::biome)).apply(i, Kind::new));
		public static final StreamCodec<ByteBuf, Kind> STREAM_CODEC = StreamCodec.composite(
				Identifier.STREAM_CODEC, Kind::id, ByteBufCodecs.BOOL, Kind::biome, Kind::new);
	}

	private record Kinds(List<List<Kind>> structureSets, List<Kind> biomes) {
	}

	private static final Map<ServerLevel, Kinds> BY_LEVEL = new WeakHashMap<>();

	private SeekKinds() {
	}

	private static Kinds of(ServerLevel level) {
		return BY_LEVEL.computeIfAbsent(level, l -> {
			List<List<Kind>> sets = new ArrayList<>();
			for (Holder<StructureSet> set : l.getChunkSource().getGeneratorState().possibleStructureSets()) {
				if (!(set.value().placement() instanceof RandomSpreadStructurePlacement || set.value().placement() instanceof ConcentricRingsStructurePlacement)) {
					continue;
				}
				Set<Kind> kinds = new LinkedHashSet<>();
				for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
					entry.structure().unwrapKey().ifPresent(key -> kinds.add(new Kind(key.identifier(), false)));
				}
				if (!kinds.isEmpty()) {
					sets.add(List.copyOf(kinds));
				}
			}
			List<Kind> biomes = new ArrayList<>();
			for (Holder<Biome> biome : l.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
				biome.unwrapKey().ifPresent(key -> biomes.add(new Kind(key.identifier(), true)));
			}
			biomes.sort(java.util.Comparator.comparing(k -> k.id().toString()));
			return new Kinds(List.copyOf(sets), List.copyOf(biomes));
		});
	}

	/** Every structure kind of this dimension. */
	public static List<Kind> structures(ServerLevel level) {
		return of(level).structureSets().stream().flatMap(List::stream).toList();
	}

	/** Every biome kind of this dimension. */
	public static List<Kind> biomes(ServerLevel level) {
		return of(level).biomes();
	}

	/** A random kind: structure or biome half and half, a structure by its set, never the biome at {@code at}; null when there is none. */
	public static @Nullable Kind roll(ServerLevel level, BlockPos at, RandomSource random) {
		Kinds kinds = of(level);
		Holder<Biome> here = level.getBiome(at);
		List<Kind> biomes = kinds.biomes().stream().filter(k -> !here.is(k.id())).toList();
		boolean structure = !kinds.structureSets().isEmpty() && (biomes.isEmpty() || random.nextBoolean());
		if (structure) {
			List<Kind> set = kinds.structureSets().get(random.nextInt(kinds.structureSets().size()));
			return set.get(random.nextInt(set.size()));
		}
		return biomes.isEmpty() ? null : biomes.get(random.nextInt(biomes.size()));
	}

	/** The kind's name: the biome's own, or the structure's (its structure.<ns>.<path> name, else its id in title case). */
	public static Component name(Kind kind) {
		if (kind.biome()) {
			return Component.translatable(Util.makeDescriptionId("biome", kind.id()));
		}
		return Component.translatableWithFallback(Util.makeDescriptionId("structure", kind.id()), titleCase(kind.id().getPath()));
	}

	private static String titleCase(String path) {
		StringBuilder out = new StringBuilder();
		for (String word : path.split("[_/]")) {
			if (!word.isEmpty()) {
				if (!out.isEmpty()) {
					out.append(' ');
				}
				out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
			}
		}
		return out.toString();
	}

	/** Kind lists are rebuilt per level (tests, server restart). */
	public static void clear() {
		BY_LEVEL.clear();
	}

	/** The structure holder of a structure kind, if registered. */
	public static java.util.Optional<Holder.Reference<Structure>> structure(ServerLevel level, Kind kind) {
		return level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
				.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, kind.id()));
	}

	/** The biome holder of a biome kind, if registered. */
	public static java.util.Optional<Holder.Reference<Biome>> biome(ServerLevel level, Kind kind) {
		return level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
				.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, kind.id()));
	}
}
