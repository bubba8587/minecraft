// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.seeking;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModComponents;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/** The Totem of Seeking: points to a random structure or biome; reaching it records the find and picks the next. */
public class TotemOfSeekingItem extends io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem {
	/** The current target: its kind, where it was picked, and where it is once the search has found it. */
	public record Target(SeekKinds.Kind kind, GlobalPos origin, Optional<BlockPos> pos) {
		public static final Codec<Target> CODEC = RecordCodecBuilder.create(i -> i.group(
				SeekKinds.Kind.CODEC.fieldOf("kind").forGetter(Target::kind),
				GlobalPos.CODEC.fieldOf("origin").forGetter(Target::origin),
				BlockPos.CODEC.optionalFieldOf("pos").forGetter(Target::pos)).apply(i, Target::new));
		public static final StreamCodec<ByteBuf, Target> STREAM_CODEC = StreamCodec.composite(
				SeekKinds.Kind.STREAM_CODEC, Target::kind, GlobalPos.STREAM_CODEC, Target::origin,
				ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Target::pos, Target::new);
	}

	/** One recorded find. */
	public record Found(SeekKinds.Kind kind, GlobalPos pos) {
		public static final Codec<Found> CODEC = RecordCodecBuilder.create(i -> i.group(
				SeekKinds.Kind.CODEC.fieldOf("kind").forGetter(Found::kind),
				GlobalPos.CODEC.fieldOf("pos").forGetter(Found::pos)).apply(i, Found::new));
		public static final StreamCodec<ByteBuf, Found> STREAM_CODEC = StreamCodec.composite(
				SeekKinds.Kind.STREAM_CODEC, Found::kind, GlobalPos.STREAM_CODEC, Found::pos, Found::new);
		public static final Codec<List<Found>> LIST_CODEC = CODEC.listOf(0, TotemsTuning.SEEK_MEMORY);
		public static final StreamCodec<ByteBuf, List<Found>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list(TotemsTuning.SEEK_MEMORY));
	}

	public TotemOfSeekingItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if ((level.getGameTime() + owner.getId()) % TotemsTuning.SEEK_UPDATE_TICKS == 0 && owner instanceof ServerPlayer player && !player.isSpectator()) {
			update(stack, level, player);
		}
	}

	/** Picks a target if there is none (or it is in another dimension), aims once the search answers, and records arrival. */
	public static void update(ItemStack stack, ServerLevel level, ServerPlayer player) {
		Target target = stack.get(ModComponents.SEEKING_TARGET);
		if (target == null || !target.origin().dimension().equals(level.dimension())) {
			pick(stack, level, player.blockPosition());
			return;
		}
		if (target.pos().isEmpty()) {
			Optional<BlockPos> found = Seeker.lookup(level, target.kind(), target.origin().pos(), skips(stack, level, target.kind()));
			if (found == null) {
				return;
			}
			if (found.isEmpty()) {
				pick(stack, level, player.blockPosition());
				return;
			}
			stack.set(ModComponents.SEEKING_TARGET, new Target(target.kind(), target.origin(), found));
			pointAt(stack, level, found.get());
			level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 0.6F, 1.0F);
			return;
		}
		if (arrived(level, player, target.kind(), target.pos().get())) {
			remember(stack, new Found(target.kind(), GlobalPos.of(level.dimension(), target.pos().get())));
			celebrate(level, player, target.kind(), stack);
			pick(stack, level, player.blockPosition());
		}
	}

	/** A new random target from {@code from}; the needle spins until its search answers. */
	public static void pick(ItemStack stack, ServerLevel level, BlockPos from) {
		stack.remove(DataComponents.LODESTONE_TRACKER);
		SeekKinds.Kind kind = SeekKinds.roll(level, from, level.getRandom());
		if (kind == null) {
			stack.remove(ModComponents.SEEKING_TARGET);
			return;
		}
		Target target = new Target(kind, GlobalPos.of(level.dimension(), from.immutable()), Optional.empty());
		stack.set(ModComponents.SEEKING_TARGET, target);
		Seeker.lookup(level, kind, from, skips(stack, level, kind));
	}

	/** Recorded finds of this kind in this dimension. */
	static List<BlockPos> skips(ItemStack stack, ServerLevel level, SeekKinds.Kind kind) {
		List<BlockPos> out = new ArrayList<>();
		for (Found found : finds(stack)) {
			if (found.kind().equals(kind) && found.pos().dimension().equals(level.dimension())) {
				out.add(found.pos().pos());
			}
		}
		return out;
	}

	/** Adds a find; the oldest is forgotten past {@link TotemsTuning#SEEK_MEMORY}. */
	public static void remember(ItemStack stack, Found found) {
		List<Found> list = new ArrayList<>(finds(stack));
		list.add(found);
		while (list.size() > TotemsTuning.SEEK_MEMORY) {
			list.removeFirst();
		}
		stack.set(ModComponents.SEEKING_FOUND, List.copyOf(list));
	}

	/** Close enough: a structure's spot (or inside one of its pieces nearby), or standing in the biome near its spot. */
	static boolean arrived(ServerLevel level, ServerPlayer player, SeekKinds.Kind kind, BlockPos at) {
		BlockPos here = player.blockPosition();
		double d = Seeker.flatDistSqr(here, at);
		if (kind.biome()) {
			return d <= (double) TotemsTuning.SEEK_ARRIVE_BIOME * TotemsTuning.SEEK_ARRIVE_BIOME && level.getBiome(here).is(kind.id());
		}
		if (d <= (double) TotemsTuning.SEEK_ARRIVE * TotemsTuning.SEEK_ARRIVE) {
			return true;
		}
		return d <= (double) TotemsTuning.SEEK_ARRIVE_INSIDE * TotemsTuning.SEEK_ARRIVE_INSIDE
				&& level.structureManager().getStructureWithPieceAt(here, s -> s.is(kind.id())).isValid();
	}

	/** The find: totem particles, a chime and its name with the count. */
	private static void celebrate(ServerLevel level, ServerPlayer player, SeekKinds.Kind kind, ItemStack stack) {
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 0.8, 0.5, 0.35);
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.4F);
		player.sendOverlayMessage(Component.translatable("message.bubbas_totems_artifacts.seeking_found", SeekKinds.name(kind), count(stack), TotemsTuning.SEEK_MEMORY));
	}

	/** Recorded finds, oldest first. */
	public static List<Found> finds(ItemStack stack) {
		List<Found> list = stack.get(ModComponents.SEEKING_FOUND);
		return list == null ? List.of() : list;
	}

	private static int count(ItemStack stack) {
		return finds(stack).size();
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
		builder.accept(Component.translatable("item.bubbas_totems_artifacts.totem_of_seeking.found", count(stack), TotemsTuning.SEEK_MEMORY).withStyle(ChatFormatting.GRAY));
	}
}
