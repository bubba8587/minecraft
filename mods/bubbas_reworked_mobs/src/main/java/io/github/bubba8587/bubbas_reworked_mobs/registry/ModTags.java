// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.gameevent.GameEvent;

public final class ModTags {
	/** Game events sleepers react to: #minecraft:vibrations plus our awaken event. */
	public static final TagKey<GameEvent> SLEEPERS_HEAR = TagKey.create(Registries.GAME_EVENT, BubbasReworkedMobs.id("sleepers_hear"));
	/** Entities whose noise never wakes sleepers (other undead shuffling about). The awaken event still does. */
	public static final TagKey<EntityType<?>> DOES_NOT_WAKE_SLEEPERS = TagKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id("does_not_wake_sleepers"));
	/** This mod's woken undead. */
	public static final TagKey<EntityType<?>> WOKEN_UNDEAD = TagKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id("woken_undead"));
	/** Tools that mine a sleeper into its item. */
	public static final TagKey<Item> SLEEPER_HARVEST_TOOLS = TagKey.create(Registries.ITEM, BubbasReworkedMobs.id("sleeper_harvest_tools"));
	/** Weapons a mob in {@link #GRABS_LOOT} reaches for first when it wakes (before any other item lying around). */
	public static final TagKey<Item> PREFERRED_ON_WAKE = TagKey.create(Registries.ITEM, BubbasReworkedMobs.id("preferred_on_wake"));
	/** Mobs that grab whatever is lying around (loot displays, loose items): on waking and, when idle, by walking to it. */
	/** Never taken by a Fabricator's teleporter. */
	public static final TagKey<EntityType<?>> FABRICATOR_IGNORES = TagKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id("fabricator_ignores"));
	public static final TagKey<EntityType<?>> GRABS_LOOT = TagKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id("grabs_loot"));

	private ModTags() {
	}
}
