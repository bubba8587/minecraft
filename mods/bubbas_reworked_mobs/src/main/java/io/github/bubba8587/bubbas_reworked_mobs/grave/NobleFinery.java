// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import java.util.Optional;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Noble dead in gemmed armor: item modifier {@code noble_finery} on a noble sleeper's gear (loaded only with Infused Gems). */
public final class NobleFinery {
	public static final ResourceKey<LootItemFunction> MODIFIER = ResourceKey.create(Registries.ITEM_MODIFIER, BubbasReworkedMobs.id("noble_finery"));

	private NobleFinery() {
	}

	/** Gems the sleeper's armor and weapon, when the modifier exists. */
	public static void apply(ServerLevel level, SleeperBlockEntity sleeper, BlockPos pos, long seed) {
		var modifier = level.getServer().reloadableRegistries().lookup().lookup(Registries.ITEM_MODIFIER).flatMap(r -> r.get(MODIFIER));
		if (modifier.isEmpty()) {
			return;
		}
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).create(LootContextParamSets.CHEST);
		LootContext context = new LootContext.Builder(params).withOptionalRandomSeed(seed == 0L ? 1L : seed).create(Optional.empty());
		for (EquipmentSlot slot : SleeperBlockEntity.SLOTS) {
			ItemStack stack = sleeper.getGear(slot);
			if (!stack.isEmpty()) {
				sleeper.setGear(slot, modifier.get().value().apply(stack.copy(), context));
			}
		}
		sleeper.setChanged();
	}
}
