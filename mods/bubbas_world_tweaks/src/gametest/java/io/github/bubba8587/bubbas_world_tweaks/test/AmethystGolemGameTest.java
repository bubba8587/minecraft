// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import io.github.bubba8587.bubbas_world_tweaks.golem.AmethystGolem;
import io.github.bubba8587.bubbas_world_tweaks.golem.AmethystGolems;
import io.github.bubba8587.bubbas_world_tweaks.golem.GolemSong;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

/** Amethyst Golem: recording, playback, beat sync, wind chime. */
public final class AmethystGolemGameTest {
	private AmethystGolemGameTest() {
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 6; x++) {
			for (int z = 0; z <= 6; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static AmethystGolem still(GameTestHelper helper, BlockPos pos) {
		AmethystGolem golem = helper.spawn(AmethystGolems.AMETHYST_GOLEM, pos);
		golem.setNoAi(true);
		return golem;
	}

	/** A golem standing on its jukebox, not in the world, so the test drives its clock. */
	private static AmethystGolem onJukebox(GameTestHelper helper, BlockPos jukebox, GolemSong song) {
		AmethystGolem golem = AmethystGolems.AMETHYST_GOLEM.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
		BlockPos abs = helper.absolutePos(jukebox);
		golem.setPos(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5);
		golem.assignJukebox(abs);
		golem.setSong(song);
		return golem;
	}

	/** Note blocks played while recording come back in order, at their pitches and spacing, tuned by shards, on beats. */
	public static void recordsAndPlaysBack(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		AmethystGolem golem = still(helper, new BlockPos(2, 1, 2));
		int[][] notes = {{5, 1, 5}, {17, 5, 4}, {10, 1, 0}};
		for (int[] n : notes) {
			helper.setBlock(new BlockPos(n[1], 1, n[2]), Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, n[0]));
		}
		golem.toggleRecording(level);
		helper.assertTrue(golem.isRecording(), "not recording after the first click");
		for (int i = 0; i < notes.length; i++) {
			int[] n = notes[i];
			helper.runAfterDelay(2 + 5L * i, () -> helper.setBlock(new BlockPos(n[1] + 1, 1, n[2]), Blocks.REDSTONE_BLOCK));
		}
		helper.runAfterDelay(20, () -> {
			golem.toggleRecording(level);
			GolemSong song = golem.song();
			helper.assertTrue(song.size() == 3, "expected 3 recorded notes, got " + song.size());
			for (int i = 0; i < 3; i++) {
				helper.assertTrue(song.tick(i) == 5 * i && song.note(i) == notes[i][0],
						"note " + i + ": tick " + song.tick(i) + " note " + song.note(i));
			}

			AmethystGolem singer = onJukebox(helper, new BlockPos(5, 1, 5), song);
			int beat = WorldTweaksTuning.GOLEM_BEAT_TICKS;
			helper.assertTrue(singer.tickSong(level, 100, beat + 1).isEmpty(), "started off the beat");
			List<Integer> sung = new ArrayList<>();
			for (long t = 0; t < 11; t++) {
				sung.addAll(singer.tickSong(level, 200 + t, 2L * beat + t));
			}
			helper.assertTrue(sung.equals(List.of(5, 17, 10)), "sang " + sung);
			singer.tuneUp(level);
			sung.clear();
			for (long t = 0; t < 11; t++) {
				sung.addAll(singer.tickSong(level, 5000 + t, 3L * beat + t));
			}
			helper.assertTrue(sung.equals(List.of(6, 18, 11)), "tuned up a semitone, sang " + sung);
			helper.succeed();
		});
	}

	/** Two clicks with nothing heard between them wipe the song. */
	public static void emptyRecordingWipes(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		AmethystGolem golem = still(helper, new BlockPos(2, 1, 2));
		golem.setSong(new GolemSong(new int[] {GolemSong.pack(0, 3), GolemSong.pack(4, 7)}));
		golem.toggleRecording(level);
		helper.runAfterDelay(3, () -> {
			golem.toggleRecording(level);
			helper.assertTrue(!golem.isRecording() && golem.song().isEmpty(), "song not wiped by an empty recording");
			helper.succeed();
		});
	}

	/** Songs start only on their beat; a golem that comes later falls in step with one already singing. */
	public static void songsStartOnTheBeatInStep(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int beat = WorldTweaksTuning.GOLEM_BEAT_TICKS;
		GolemSong song = new GolemSong(new int[] {GolemSong.pack(0, 0), GolemSong.pack(400, 12), GolemSong.pack(beat + 200, 24)});
		int period = song.period(beat);
		helper.assertTrue(period == 2 * beat, "a song over one beat long takes two: " + period);
		AmethystGolem first = onJukebox(helper, new BlockPos(1, 1, 1), song);
		AmethystGolem late = onJukebox(helper, new BlockPos(3, 1, 1), song);
		List<Long> firstTicks = new ArrayList<>();
		List<Long> lateTicks = new ArrayList<>();
		long offset = 7 * beat + 300;
		for (long now = 0; now < 5 * beat; now++) {
			long anchor = now + offset;
			if (!first.tickSong(level, now, anchor).isEmpty()) {
				firstTicks.add(anchor);
			}
			if (now >= beat) {
				IntList sung = late.tickSong(level, now, anchor);
				if (!sung.isEmpty()) {
					lateTicks.add(anchor);
				}
			}
		}
		helper.assertTrue(!firstTicks.isEmpty() && Math.floorMod(firstTicks.get(0), period) == 0, "first start off the beat: " + firstTicks);
		helper.assertTrue(!lateTicks.isEmpty(), "the late golem never sang");
		List<Long> overlap = firstTicks.stream().filter(t -> t >= lateTicks.get(0)).toList();
		helper.assertTrue(overlap.equals(lateTicks), "out of step: " + firstTicks + " vs " + lateTicks);
		helper.succeed();
	}

	/** With no song, vibrations below the floor are ignored and those at or above it chime. */
	public static void windChimeFloor(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		AmethystGolem golem = still(helper, new BlockPos(2, 1, 2));
		Vec3 near = helper.absoluteVec(new Vec3(4.5, 1.0, 4.5));
		helper.assertTrue(VibrationSystem.getGameEventFrequency(GameEvent.STEP) < WorldTweaksTuning.GOLEM_CHIME_FLOOR
				&& VibrationSystem.getGameEventFrequency(GameEvent.HIT_GROUND) >= WorldTweaksTuning.GOLEM_CHIME_FLOOR, "floor sits between step and landing");
		int[] base = new int[1];
		helper.runAfterDelay(5, () -> {
			base[0] = golem.chimes();
			level.gameEvent(GameEvent.STEP, near, GameEvent.Context.of(null, null));
		});
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(golem.chimes() == base[0], "chimed below the floor");
			level.gameEvent(GameEvent.HIT_GROUND, near, GameEvent.Context.of(null, null));
		});
		helper.runAfterDelay(35, () -> {
			helper.assertTrue(golem.chimes() == base[0] + 1, "no chime at the floor: " + (golem.chimes() - base[0]));
			helper.succeed();
		});
	}
}
