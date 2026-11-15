// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An Infused Gem in the enchanting table (station/GemEnchanting): the three rows are its three tiers, with vanilla's
 * bookshelf costs, lapis 1-3 and XP; rows above the gem's form stay off. Choosing a row turns it into the finished gem
 * rolled from that tier with the player's enchantment seed. The hint is drawn by the client (EnchantmentScreenMixin),
 * so the vanilla clue stays empty.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {
	@Shadow
	@Final
	private Container enchantSlots;
	@Shadow
	@Final
	private ContainerLevelAccess access;
	@Shadow
	@Final
	private DataSlot enchantmentSeed;
	@Shadow
	@Final
	public int[] costs;
	@Shadow
	@Final
	public int[] enchantClue;
	@Shadow
	@Final
	public int[] levelClue;

	protected EnchantmentMenuMixin(MenuType<?> type, int containerId) {
		super(type, containerId);
	}

	@Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
	private void bubbas$gemCosts(Container container, CallbackInfo ci) {
		if (container != this.enchantSlots) {
			return;
		}
		ItemStack stack = container.getItem(0);
		if (!GemEnchanting.isInfusedGem(stack)) {
			return;
		}
		ci.cancel();
		InfusedGem gem = stack.get(ModComponents.INFUSED_GEM);
		this.access.execute((level, pos) -> {
			int shelves = 0;
			for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
				if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
					shelves++;
				}
			}
			int[] c = GemEnchanting.costs(this.enchantmentSeed.get(), shelves, gem);
			for (int i = 0; i < 3; i++) {
				this.costs[i] = c[i];
				this.enchantClue[i] = -1;
				this.levelClue[i] = -1;
			}
			this.broadcastChanges();
		});
	}

	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void bubbas$enchantGem(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
		ItemStack stack = this.enchantSlots.getItem(0);
		if (buttonId < 0 || buttonId > 2 || !GemEnchanting.isInfusedGem(stack)) {
			return;
		}
		ItemStack lapis = this.enchantSlots.getItem(1);
		int tier = buttonId + 1;
		boolean free = player.hasInfiniteMaterials();
		if (this.costs[buttonId] <= 0 || (!free && (lapis.getCount() < tier || player.experienceLevel < this.costs[buttonId]))) {
			cir.setReturnValue(false);
			return;
		}
		InfusedGem infused = stack.get(ModComponents.INFUSED_GEM);
		this.access.execute((level, pos) -> {
			Gem gem = GemEnchanting.roll(this.enchantmentSeed.get(), tier, infused);
			player.onEnchantmentPerformed(stack, tier);
			ItemStack result = GemEnchanting.finished(gem);
			this.enchantSlots.setItem(0, result);
			lapis.consume(tier, player);
			if (lapis.isEmpty()) {
				this.enchantSlots.setItem(1, ItemStack.EMPTY);
			}
			player.awardStat(Stats.ENCHANT_ITEM);
			if (player instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, result, tier);
			}
			this.enchantSlots.setChanged();
			this.enchantmentSeed.set(player.getEnchantmentSeed());
			level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
		});
		cir.setReturnValue(true);
	}
}
