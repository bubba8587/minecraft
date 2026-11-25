// dte:C4
package io.github.bubba8587.bubbas_infused_gems.loot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Gems as loot. */
public final class GemLoot {
	private static ResourceKey<LootItemFunction> mobGear;
	/** Spawn equipment calls nest: gems are added once, when the outermost call returns. */
	private static final ThreadLocal<int[]> SPAWN_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
	private static final Map<Identifier, ResourceKey<LootTable>> INJECT = new LinkedHashMap<>();
	private static final Map<Identifier, ResourceKey<LootItemFunction>> EQUIPMENT = new LinkedHashMap<>();

	private GemLoot() {
	}

	public static Map<Identifier, ResourceKey<LootTable>> injected() {
		return INJECT;
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, BubbasInfusedGems.id("random_gem"), RandomGemFunction.MAP_CODEC);
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, BubbasInfusedGems.id("socket_random_gems"), SocketRandomGemsFunction.MAP_CODEC);
		JsonObject raw = Balance.get().raw;
		mobGear = ResourceKey.create(Registries.ITEM_MODIFIER, Identifier.parse(raw.get("mob_gear_modifier").getAsString()));
		raw.getAsJsonObject("loot_inject").entrySet().forEach(e -> INJECT.put(Identifier.parse(e.getKey()),
				ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(e.getValue().getAsString()))));
		raw.getAsJsonObject("loot_equipment").entrySet().forEach(e -> EQUIPMENT.put(Identifier.parse(e.getKey()),
				ResourceKey.create(Registries.ITEM_MODIFIER, Identifier.parse(e.getValue().getAsString()))));
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			// Not a table a datapack replaced through Fabric
			if (source == LootTableSource.REPLACED) {
				return;
			}
			ResourceKey<LootTable> inject = INJECT.get(key.identifier());
			if (inject != null) {
				builder.withPool(LootPool.lootPool().add(NestedLootTable.lootTableReference(registries.getOrThrow(inject))));
			}
			ResourceKey<LootItemFunction> modifier = EQUIPMENT.get(key.identifier());
			if (modifier != null) {
				builder.apply(registries.getOrThrow(modifier));
			}
		});
	}

	public static void enterSpawnGear() {
		SPAWN_DEPTH.get()[0]++;
	}

	/** Leaving a spawn equipment method: at the outermost one, the mob's gear may get gems. */
	public static void leaveSpawnGear(Mob mob) {
		int[] depth = SPAWN_DEPTH.get();
		depth[0] = Math.max(0, depth[0] - 1);
		if (depth[0] == 0) {
			gemSpawnGear(mob);
		}
	}

	/** After a mob's spawn armor and weapons are chosen: each piece may get gems. */
	public static void gemSpawnGear(Mob mob) {
		if (!(mob.level() instanceof ServerLevel level)) {
			return;
		}
		Optional<? extends Holder<LootItemFunction>> modifier = level.getServer().reloadableRegistries().lookup()
				.lookup(Registries.ITEM_MODIFIER).flatMap(r -> r.get(mobGear));
		if (modifier.isEmpty()) {
			return;
		}
		LootContext context = null;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR && slot.getType() != EquipmentSlot.Type.HAND) {
				continue;
			}
			ItemStack stack = mob.getItemBySlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (context == null) {
				LootParams params = new LootParams.Builder(level)
						.withParameter(LootContextParams.ORIGIN, mob.position())
						.withParameter(LootContextParams.THIS_ENTITY, mob)
						.create(LootContextParamSets.EQUIPMENT);
				context = new LootContext.Builder(params).withOptionalRandomSource(mob.getRandom()).create(Optional.empty());
			}
			ItemStack out = modifier.get().value().apply(stack.copy(), context);
			if (!ItemStack.matches(out, stack)) {
				mob.setItemSlot(slot, out);
			}
		}
	}
}
