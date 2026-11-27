// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.seeking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

import com.mojang.math.Transformation;
import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** A map put in a Seeking statue is filled in whole, a patch at a time, then pops back out (specs/totems-artifacts.md). Main server thread only. */
public final class SeekingMap {
	/** Tag of the item display that holds a statue's map. */
	public static final String TAG = "bubbas_totems_artifacts.seeking_map";
	/** Loads a patch of the map's ground while it is drawn. */
	public static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, BubbasTotemsArtifacts.id("seeking_map"),
			new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));

	private record Ticket(ChunkPos pos, int radius) {
	}

	/** One map being filled. */
	private static final class Job {
		final MapId map;
		final List<int[]> spots;
		int next;
		@Nullable Ticket loading;
		@Nullable CompletableFuture<?> loaded;
		@Nullable Ticket previous;

		Job(MapId map, List<int[]> spots) {
			this.map = map;
			this.spots = spots;
		}
	}

	private static final Map<ServerLevel, Map<BlockPos, Job>> JOBS = new WeakHashMap<>();

	private SeekingMap() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				Map<BlockPos, Job> jobs = JOBS.get(level);
				if (jobs != null && !jobs.isEmpty()) {
					tick(level, jobs);
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> JOBS.clear());
	}

	/** Whether this stack can go in a Seeking statue. */
	public static boolean accepts(ItemStack stack) {
		return stack.is(Items.FILLED_MAP) || stack.is(Items.MAP);
	}

	/** A map used on a Seeking statue goes in (an empty one becomes a map of the statue's spot). False while it is busy with another. */
	public static boolean insert(ServerLevel level, BlockPos pos, Player player, InteractionHand hand) {
		if (display(level, pos) != null) {
			return false;
		}
		ItemStack held = player.getItemInHand(hand);
		ItemStack map = held.is(Items.MAP) ? MapItem.create(level, pos.getX(), pos.getZ(), (byte) 0, true, false) : held.copyWithCount(1);
		held.consume(1, player);
		spawnCarrier(level, pos, map);
		stop(level, pos);
		level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
		return true;
	}

	/** The statue is gone: its map drops. */
	public static void removed(ServerLevel level, BlockPos pos) {
		Display.ItemDisplay display = display(level, pos);
		if (display != null) {
			Block.popResource(level, pos, display.getItemStack().copy());
			display.discard();
		}
		stop(level, pos);
	}

	/** The statue's pulse: starts filling a map that isn't filled yet. */
	public static void pulse(ServerLevel level, BlockPos pos) {
		Display.ItemDisplay display = display(level, pos);
		if (display == null) {
			return;
		}
		MapId id = display.getItemStack().get(DataComponents.MAP_ID);
		MapItemSavedData data = MapItem.getSavedData(id, level);
		if (id == null || data == null || data.locked || data.dimension != level.dimension()) {
			eject(level, pos, display);
			return;
		}
		Map<BlockPos, Job> jobs = JOBS.computeIfAbsent(level, l -> new HashMap<>());
		Job job = jobs.get(pos);
		if (job == null || !job.map.equals(id)) {
			stop(level, pos);
			jobs.put(pos.immutable(), new Job(id, spots(level, data, pos)));
		}
		level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 6, 0.4, 0.3, 0.4, 0.6);
	}

	/** Where the drawing viewer stands, nearest the statue first, so every pixel is within its drawing radius of one spot. */
	private static List<int[]> spots(ServerLevel level, MapItemSavedData data, BlockPos statue) {
		int scale = 1 << data.scale;
		int width = 128 * scale;
		int reach = Math.max(1, (int) ((radiusPixels(level, data) - 2) * scale * Math.sqrt(2.0)));
		int n = Math.max(1, Math.ceilDiv(width, reach));
		double cell = (double) width / n;
		int minX = data.centerX - width / 2;
		int minZ = data.centerZ - width / 2;
		List<int[]> spots = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				spots.add(new int[] {(int) (minX + (i + 0.5) * cell), (int) (minZ + (j + 0.5) * cell)});
			}
		}
		spots.sort(Comparator.comparingDouble(s -> Math.pow(s[0] - statue.getX(), 2) + Math.pow(s[1] - statue.getZ(), 2)));
		return spots;
	}

	/** Vanilla's drawing radius around a map holder, in map pixels. */
	private static int radiusPixels(ServerLevel level, MapItemSavedData data) {
		int radius = 128 / (1 << data.scale);
		return level.dimensionType().hasCeiling() ? radius / 2 : radius;
	}

	/** Advances every job of this level: load the next patch, draw it once loaded. */
	private static void tick(ServerLevel level, Map<BlockPos, Job> jobs) {
		for (Iterator<Map.Entry<BlockPos, Job>> it = jobs.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<BlockPos, Job> entry = it.next();
			BlockPos pos = entry.getKey();
			Job job = entry.getValue();
			if (!level.isLoaded(pos)) {
				release(level, job);
				it.remove();
				continue;
			}
			Display.ItemDisplay display = display(level, pos);
			BlockState state = level.getBlockState(pos);
			if (display == null || !(state.getBlock() instanceof TotemStatueBlock) || TotemStatueBlock.type(state) != TotemType.SEEKING) {
				if (display != null) {
					Block.popResource(level, pos, display.getItemStack().copy());
					display.discard();
				}
				release(level, job);
				it.remove();
				continue;
			}
			MapItemSavedData data = MapItem.getSavedData(job.map, level);
			if (data == null || !job.map.equals(display.getItemStack().get(DataComponents.MAP_ID))) {
				release(level, job);
				it.remove();
				continue;
			}
			if (job.loaded == null) {
				int[] spot = job.spots.get(job.next);
				int blocks = Math.min(radiusPixels(level, data) * (1 << data.scale), 64 * (1 << data.scale));
				job.loading = new Ticket(new ChunkPos(spot[0] >> 4, spot[1] >> 4), Math.min(TotemsTuning.SEEK_MAP_MAX_TICKET_RADIUS, (blocks >> 4) + 1));
				job.loaded = level.getChunkSource().addTicketAndLoadWithRadius(TICKET, job.loading.pos(), job.loading.radius());
				continue;
			}
			if (!job.loaded.isDone()) {
				continue;
			}
			draw(level, data, job.spots.get(job.next));
			if (job.previous != null) {
				level.getChunkSource().removeTicketWithRadius(TICKET, job.previous.pos(), job.previous.radius());
			}
			job.previous = job.loading;
			job.loading = null;
			job.loaded = null;
			if (++job.next >= job.spots.size()) {
				release(level, job);
				it.remove();
				eject(level, pos, display);
			}
		}
	}

	/** Vanilla's own map drawing, from a stand-in holder at spot: one pass per column stripe. */
	private static void draw(ServerLevel level, MapItemSavedData data, int[] spot) {
		FakePlayer viewer = FakePlayer.get(level);
		viewer.setPos(spot[0] + 0.5, level.getSeaLevel(), spot[1] + 0.5);
		MapItem item = (MapItem) Items.FILLED_MAP;
		for (int pass = 0; pass < 16; pass++) {
			item.update(level, viewer, data);
		}
	}

	/** Stops filling at pos (map changed or taken). */
	private static void stop(ServerLevel level, BlockPos pos) {
		Map<BlockPos, Job> jobs = JOBS.get(level);
		Job job = jobs == null ? null : jobs.remove(pos);
		if (job != null) {
			release(level, job);
		}
	}

	private static void release(ServerLevel level, Job job) {
		for (Ticket ticket : new Ticket[] {job.loading, job.previous}) {
			if (ticket != null) {
				level.getChunkSource().removeTicketWithRadius(TICKET, ticket.pos(), ticket.radius());
			}
		}
		job.loading = null;
		job.previous = null;
		job.loaded = null;
	}

	/** The item display holding pos's map, if any. */
	public static Display.@Nullable ItemDisplay display(ServerLevel level, BlockPos pos) {
		List<Display.ItemDisplay> found = level.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(pos), d -> d.entityTags().contains(TAG));
		return found.isEmpty() ? null : found.getFirst();
	}

	/** The statue's hidden slot: an item display too small to see, holding the map inside the statue. */
	private static void spawnCarrier(ServerLevel level, BlockPos pos, ItemStack map) {
		Display.ItemDisplay carrier = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		carrier.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		carrier.setItemStack(map);
		carrier.setTransformation(new Transformation(null, null, new Vector3f(0.0F, 0.0F, 0.0F), null));
		carrier.addTag(TAG);
		level.addFreshEntity(carrier);
	}

	/** The filled map pops out of the statue's front. */
	private static void eject(ServerLevel level, BlockPos pos, Display.ItemDisplay carrier) {
		ItemStack map = carrier.getItemStack().copy();
		carrier.discard();
		stop(level, pos);
		BlockState state = level.getBlockState(pos);
		Direction facing = state.getBlock() instanceof TotemStatueBlock ? state.getValue(TotemStatueBlock.FACING) : Direction.NORTH;
		ItemEntity item = new ItemEntity(level, pos.getX() + 0.5 + facing.getStepX() * 0.6, pos.getY() + 0.6, pos.getZ() + 0.5 + facing.getStepZ() * 0.6, map,
				facing.getStepX() * 0.12, 0.2, facing.getStepZ() * 0.12);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.0);
		level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
}
