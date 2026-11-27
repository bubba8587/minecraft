// dte:C8,B15
package io.github.bubba8587.bubbas_world_tweaks.golem;

import com.google.common.collect.ImmutableSet;
import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** Amethyst Golem registration and build pattern. */
public final class AmethystGolems {
	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, BubbasWorldTweaks.id("amethyst_golem"));
	public static final EntityType<AmethystGolem> AMETHYST_GOLEM = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
			EntityType.Builder.<AmethystGolem>of(AmethystGolem::new, MobCategory.MISC).sized(0.49F, 0.98F).eyeHeight(0.8125F)
					.clientTrackingRange(10).build(KEY));

	private static final ResourceKey<Item> EGG_KEY = ResourceKey.create(Registries.ITEM, BubbasWorldTweaks.id("amethyst_golem_spawn_egg"));
	public static final Item AMETHYST_GOLEM_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, EGG_KEY,
			new SpawnEggItem(new Item.Properties().spawnEgg(AMETHYST_GOLEM).setId(EGG_KEY)));

	/** Jukeboxes as a point of interest with one ticket: one golem per jukebox. */
	public static final ResourceKey<PoiType> JUKEBOX_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, BubbasWorldTweaks.id("golem_jukebox"));

	static {
		PoiTypes.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, JUKEBOX_POI,
				ImmutableSet.copyOf(Blocks.JUKEBOX.getStateDefinition().getPossibleStates()), 1, 1);
	}

	public static boolean isJukeboxPoi(Holder<PoiType> type) {
		return type.is(JUKEBOX_POI);
	}

	/** Pumpkin spot over one amethyst block, and the full build with a carved pumpkin or jack o'lantern. */
	private static @Nullable BlockPattern base;
	private static @Nullable BlockPattern full;

	private AmethystGolems() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(AMETHYST_GOLEM, AmethystGolem.createAttributes());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(output -> output.insertAfter(Items.COPPER_GOLEM_SPAWN_EGG, AMETHYST_GOLEM_SPAWN_EGG));
	}

	public static BlockPattern basePattern() {
		if (base == null) {
			base = BlockPatternBuilder.start().aisle(" ", "#")
					.where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.AMETHYST_BLOCK))).build();
		}
		return base;
	}

	public static BlockPattern fullPattern() {
		if (full == null) {
			full = BlockPatternBuilder.start().aisle("^", "#")
					.where('^', BlockInWorld.hasState(state -> state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN)))
					.where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.AMETHYST_BLOCK))).build();
		}
		return full;
	}

	/** Whether a pumpkin placed at this spot would complete an amethyst golem. */
	public static boolean canBuild(LevelReader level, BlockPos topPos) {
		return basePattern().find(level, topPos) != null;
	}
}
