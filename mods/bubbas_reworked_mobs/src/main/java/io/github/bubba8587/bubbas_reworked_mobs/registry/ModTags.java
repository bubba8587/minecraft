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
	/** Weapons a waking undead takes from a loot display or (zombies) picks up from the ground. Skeletons also take bows. */
	public static final TagKey<Item> SLEEPER_WEAPONS = TagKey.create(Registries.ITEM, BubbasReworkedMobs.id("sleeper_weapons"));

	private ModTags() {
	}
}
