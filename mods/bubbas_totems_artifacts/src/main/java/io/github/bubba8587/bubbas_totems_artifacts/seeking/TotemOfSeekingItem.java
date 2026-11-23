// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.seeking;

import java.util.Optional;
import java.util.function.Consumer;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModComponents;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** The Totem of Seeking. */
public class TotemOfSeekingItem extends io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem {
	public TotemOfSeekingItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult statue = super.useOn(context);
		if (statue != InteractionResult.PASS) {
			return statue;
		}
		Level level = context.getLevel();
		Optional<SeekingTypes.SeekType> type = SeekingTypes.forBlock(level.getBlockState(context.getClickedPos()));
		if (type.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel) {
			ItemStack stack = context.getItemInHand();
			choose(stack, type.get());
			level.playSound(null, context.getClickedPos(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
			Player player = context.getPlayer();
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.bubbas_totems_artifacts.seeking", type.get().displayName()));
				update(stack, serverLevel, player.blockPosition());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Sets the type and forgets the old target (the needle spins until the first answer). */
	public static void choose(ItemStack stack, SeekingTypes.SeekType type) {
		stack.set(ModComponents.SEEKING_TYPE, type.id());
		stack.remove(DataComponents.LODESTONE_TRACKER);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if ((level.getGameTime() + owner.getId()) % TotemsTuning.SEEK_UPDATE_TICKS == 0 && !owner.isSpectator()) {
			update(stack, level, owner.blockPosition());
		}
	}

	/** Aims stack at the nearest structure of its type from from, when the answer is in. */
	public static void update(ItemStack stack, ServerLevel level, BlockPos from) {
		Optional<SeekingTypes.SeekType> type = SeekingTypes.byId(stack.get(ModComponents.SEEKING_TYPE));
		if (type.isEmpty()) {
			return;
		}
		Optional<BlockPos> found = StructureSeeker.lookup(level, type.get(), from);
		if (found == null) {
			return;
		}
		pointAt(stack, level, found.orElse(null));
	}

	/** Points a compass-like stack at target in level, or (null) lets it spin. */
	public static void pointAt(ItemStack stack, ServerLevel level, @Nullable BlockPos target) {
		LodestoneTracker tracker = new LodestoneTracker(Optional.ofNullable(target).map(pos -> GlobalPos.of(level.dimension(), pos)), false);
		if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
			stack.set(DataComponents.LODESTONE_TRACKER, tracker);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		Optional<SeekingTypes.SeekType> type = SeekingTypes.byId(stack.get(ModComponents.SEEKING_TYPE));
		builder.accept(type.map(t -> Component.translatable("item.bubbas_totems_artifacts.totem_of_seeking.seeks", t.displayName()).withStyle(ChatFormatting.GRAY))
				.orElseGet(() -> Component.translatable("item.bubbas_totems_artifacts.totem_of_seeking.unset").withStyle(ChatFormatting.GRAY)));
	}
}
