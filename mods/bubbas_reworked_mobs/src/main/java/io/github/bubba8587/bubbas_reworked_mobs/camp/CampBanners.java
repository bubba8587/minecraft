// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.world.LoadedBlockEntities;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Camp banners (specs/monster-camps.md): vanilla banners (B15), one pattern per camp kind. They are the camp's key:
 * while one of its kind's banners stands in the camp's box, the camp is armed. Checked from the block entities of the
 * chunks the box touches (a few map lookups), never by scanning blocks.
 */
public final class CampBanners {
	private CampBanners() {
	}

	/** The standing banner block of the kind's base colour. */
	public static Block standing(CampKind kind) {
		return net.minecraft.world.level.block.Blocks.BANNER.pick(kind.bannerBase());
	}

	/** The wall banner block of the kind's base colour. */
	public static Block wall(CampKind kind) {
		return net.minecraft.world.level.block.Blocks.WALL_BANNER.pick(kind.bannerBase());
	}

	/** The kind's pattern layers. */
	public static BannerPatternLayers layers(CampKind kind, HolderGetter<BannerPattern> patterns) {
		BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
		for (CampKind.Layer layer : kind.bannerLayers()) {
			builder.add(patterns.getOrThrow(layer.pattern()), layer.color());
		}
		return builder.build();
	}

	/** The kind's banner as an item (what a camp banner drops; tests, the creative player). */
	public static ItemStack item(CampKind kind, HolderLookup.Provider registries) {
		ItemStack stack = new ItemStack(standing(kind).asItem());
		stack.set(DataComponents.BANNER_PATTERNS, layers(kind, registries.lookupOrThrow(Registries.BANNER_PATTERN)));
		return stack;
	}

	/** Gives a just-placed banner block the kind's pattern. */
	public static void paint(@Nullable BlockEntity entity, CampKind kind, HolderLookup.Provider registries) {
		if (entity instanceof BannerBlockEntity banner) {
			banner.applyComponentsFromItemStack(item(kind, registries));
			banner.setChanged();
		}
	}

	/** Whether this banner is the kind's banner (null kind: any banner, for pillager outposts). */
	public static boolean matches(BannerBlockEntity banner, @Nullable CampKind kind) {
		if (kind == null) {
			return true;
		}
		if (banner.getBaseColor() != kind.bannerBase()) {
			return false;
		}
		List<BannerPatternLayers.Layer> layers = banner.getPatterns().layers();
		List<CampKind.Layer> want = kind.bannerLayers();
		if (layers.size() != want.size()) {
			return false;
		}
		for (int i = 0; i < want.size(); i++) {
			if (!layers.get(i).pattern().is(want.get(i).pattern()) || layers.get(i).color() != want.get(i).color()) {
				return false;
			}
		}
		return true;
	}

	/** Whether a matching banner stands in the box, or null when part of the box isn't loaded. */
	public static @Nullable Boolean standsIn(ServerLevel level, BoundingBox box, @Nullable CampKind kind) {
		List<BannerBlockEntity> banners = LoadedBlockEntities.in(level, BannerBlockEntity.class, box);
		return banners == null ? null : banners.stream().anyMatch(banner -> matches(banner, kind));
	}

	/** Whether a lit campfire or soul campfire stands in the box, or null when part of the box isn't loaded. */
	public static @Nullable Boolean litFireIn(ServerLevel level, BoundingBox box) {
		List<CampfireBlockEntity> fires = LoadedBlockEntities.in(level, CampfireBlockEntity.class, box);
		return fires == null ? null : fires.stream().anyMatch(fire -> fire.getBlockState().getValueOrElse(CampfireBlock.LIT, false));
	}
}
