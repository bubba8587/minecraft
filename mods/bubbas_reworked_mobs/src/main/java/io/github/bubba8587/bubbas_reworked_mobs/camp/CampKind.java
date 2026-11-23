// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The kinds of monster camp (specs/monster-camps.md): skeletons and zombies, and the biome versions with the
 * same rules: husks in deserts, strays in snow, bogged in swamps. Each has its own mob, its own banner (readable from a
 * distance) and its own materials.
 */
public enum CampKind {
	SKELETON(EntityTypes.SKELETON, DyeColor.BLACK, List.of(new Layer(BannerPatterns.SKULL, DyeColor.WHITE), new Layer(BannerPatterns.STRIPE_BOTTOM,
			DyeColor.WHITE))),
	ZOMBIE(EntityTypes.ZOMBIE, DyeColor.GREEN, List.of(new Layer(BannerPatterns.SKULL, DyeColor.BLACK), new Layer(BannerPatterns.TRIANGLES_BOTTOM,
			DyeColor.BLACK))),
	HUSK(EntityTypes.HUSK, DyeColor.BROWN, List.of(new Layer(BannerPatterns.SKULL, DyeColor.YELLOW), new Layer(BannerPatterns.TRIANGLES_BOTTOM,
			DyeColor.YELLOW))),
	STRAY(EntityTypes.STRAY, DyeColor.LIGHT_BLUE, List.of(new Layer(BannerPatterns.SKULL, DyeColor.WHITE), new Layer(BannerPatterns.TRIANGLES_TOP,
			DyeColor.WHITE))),
	BOGGED(EntityTypes.BOGGED, DyeColor.GRAY, List.of(new Layer(BannerPatterns.GRADIENT_UP, DyeColor.GREEN), new Layer(BannerPatterns.SKULL,
			DyeColor.LIME)));

	/** One layer of a camp banner: a vanilla pattern in a colour. */
	public record Layer(ResourceKey<BannerPattern> pattern, DyeColor color) {
	}

	private final EntityType<? extends Mob> mob;
	private final DyeColor bannerBase;
	private final List<Layer> bannerLayers;

	CampKind(EntityType<? extends Mob> mob, DyeColor bannerBase, List<Layer> bannerLayers) {
		this.mob = mob;
		this.bannerBase = bannerBase;
		this.bannerLayers = bannerLayers;
	}

	public EntityType<? extends Mob> mob() {
		return mob;
	}

	public DyeColor bannerBase() {
		return bannerBase;
	}

	public List<Layer> bannerLayers() {
		return bannerLayers;
	}

	public String id() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}

	/** Whether its mobs shoot (archers on the towers, a bow for the leader). */
	public boolean archers() {
		return this != ZOMBIE && this != HUSK;
	}

	/** The camp kind for a biome: husks in deserts and badlands, strays in snow, bogged in swamps, else skeletons or zombies. */
	public static CampKind forBiome(Holder<Biome> biome, RandomSource random) {
		boolean skeletons = random.nextBoolean(); // rolled always, so the stream doesn't depend on the biome
		if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS)) {
			return HUSK;
		}
		if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) {
			return BOGGED;
		}
		if (biome.value().getBaseTemperature() < 0.15F) {
			return STRAY;
		}
		return skeletons ? SKELETON : ZOMBIE;
	}

	// --- materials --------------------------------------------------------------------------------

	/** Tower posts and seats. */
	public BlockState log() {
		return switch (this) {
			case SKELETON, STRAY -> Blocks.SPRUCE_LOG.defaultBlockState();
			case ZOMBIE -> Blocks.DARK_OAK_LOG.defaultBlockState();
			case HUSK -> Blocks.STRIPPED_ACACIA_LOG.defaultBlockState();
			case BOGGED -> Blocks.MANGROVE_LOG.defaultBlockState();
		};
	}

	public BlockState planks() {
		return switch (this) {
			case SKELETON, STRAY -> Blocks.SPRUCE_PLANKS.defaultBlockState();
			case ZOMBIE -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
			case HUSK -> Blocks.ACACIA_PLANKS.defaultBlockState();
			case BOGGED -> Blocks.MANGROVE_PLANKS.defaultBlockState();
		};
	}

	public BlockState slab() {
		return switch (this) {
			case SKELETON, STRAY -> Blocks.SPRUCE_SLAB.defaultBlockState();
			case ZOMBIE -> Blocks.DARK_OAK_SLAB.defaultBlockState();
			case HUSK -> Blocks.ACACIA_SLAB.defaultBlockState();
			case BOGGED -> Blocks.MANGROVE_SLAB.defaultBlockState();
		};
	}

	public BlockState fence() {
		return switch (this) {
			case SKELETON, STRAY -> Blocks.SPRUCE_FENCE.defaultBlockState();
			case ZOMBIE -> Blocks.DARK_OAK_FENCE.defaultBlockState();
			case HUSK -> Blocks.ACACIA_FENCE.defaultBlockState();
			case BOGGED -> Blocks.MANGROVE_FENCE.defaultBlockState();
		};
	}

	/** Stairs of the underground room's tunnel. */
	public BlockState stairs() {
		return switch (this) {
			case SKELETON, STRAY -> Blocks.COBBLESTONE_STAIRS.defaultBlockState();
			case ZOMBIE -> Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState();
			case HUSK -> Blocks.SANDSTONE_STAIRS.defaultBlockState();
			case BOGGED -> Blocks.MUD_BRICK_STAIRS.defaultBlockState();
		};
	}

	/** The trodden ground of the camp, by roll in [0, 1). */
	public BlockState trodden(float roll) {
		return switch (this) {
			// (No dirt path: it turns back to dirt under anything built on it.)
			case SKELETON -> roll < 0.45F ? Blocks.COARSE_DIRT.defaultBlockState() : roll < 0.75F ? Blocks.GRAVEL.defaultBlockState()
					: Blocks.PACKED_MUD.defaultBlockState();
			case ZOMBIE -> roll < 0.4F ? Blocks.COARSE_DIRT.defaultBlockState() : roll < 0.7F ? Blocks.ROOTED_DIRT.defaultBlockState()
					: Blocks.PODZOL.defaultBlockState();
			case HUSK -> roll < 0.5F ? Blocks.SAND.defaultBlockState() : roll < 0.8F ? Blocks.COARSE_DIRT.defaultBlockState()
					: Blocks.SMOOTH_SANDSTONE.defaultBlockState();
			case STRAY -> roll < 0.4F ? Blocks.SNOW_BLOCK.defaultBlockState() : roll < 0.75F ? Blocks.GRAVEL.defaultBlockState()
					: Blocks.PACKED_ICE.defaultBlockState();
			case BOGGED -> roll < 0.45F ? Blocks.MUD.defaultBlockState() : roll < 0.75F ? Blocks.PACKED_MUD.defaultBlockState()
					: Blocks.COARSE_DIRT.defaultBlockState();
		};
	}

	/** The rock of a bone-and-skull outcrop, by roll in [0, 1). */
	public BlockState rock(float roll) {
		return switch (this) {
			case SKELETON -> roll < 0.4F ? Blocks.STONE.defaultBlockState() : roll < 0.7F ? Blocks.ANDESITE.defaultBlockState()
					: Blocks.COBBLESTONE.defaultBlockState();
			case ZOMBIE -> roll < 0.5F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : roll < 0.8F ? Blocks.COBBLESTONE.defaultBlockState()
					: Blocks.TUFF.defaultBlockState();
			case HUSK -> roll < 0.5F ? Blocks.SANDSTONE.defaultBlockState() : roll < 0.8F ? Blocks.SMOOTH_SANDSTONE.defaultBlockState()
					: Blocks.TERRACOTTA.defaultBlockState();
			case STRAY -> roll < 0.45F ? Blocks.STONE.defaultBlockState() : roll < 0.75F ? Blocks.PACKED_ICE.defaultBlockState()
					: Blocks.DIORITE.defaultBlockState();
			case BOGGED -> roll < 0.45F ? Blocks.MUD_BRICKS.defaultBlockState() : roll < 0.75F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
					: Blocks.PACKED_MUD.defaultBlockState();
		};
	}

	/** Skulls on the outcrops and pikes. */
	public BlockState skull() {
		return this == ZOMBIE || this == HUSK ? Blocks.ZOMBIE_HEAD.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState();
	}

	/** A scrap of plant life round the camp's edge. */
	public BlockState weed(float roll) {
		return switch (this) {
			case SKELETON, ZOMBIE -> roll < 0.5F ? Blocks.SHORT_GRASS.defaultBlockState() : Blocks.FERN.defaultBlockState();
			case HUSK -> Blocks.DEAD_BUSH.defaultBlockState();
			case STRAY -> Blocks.SNOW.defaultBlockState();
			case BOGGED -> roll < 0.5F ? Blocks.BROWN_MUSHROOM.defaultBlockState() : Blocks.MOSS_CARPET.defaultBlockState();
		};
	}
}
