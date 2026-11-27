// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.golem;

import java.util.Arrays;

import net.minecraft.world.level.block.NoteBlock;

/** A recorded tune: notes (0-24, note block steps) at tick offsets from the first note. */
public final class GolemSong {
	public static final GolemSong EMPTY = new GolemSong(new int[0]);
	private static final int NOTE_BITS = 5;
	private static final int NOTE_MASK = (1 << NOTE_BITS) - 1;
	/** Note block range. */
	public static final int NOTES = 25;

	private final int[] packed;

	/** From packed (tick, note) values, sorted by tick. */
	public GolemSong(int[] packed) {
		this.packed = packed.clone();
		Arrays.sort(this.packed);
	}

	public static int pack(int tick, int note) {
		return (tick << NOTE_BITS) | (note & NOTE_MASK);
	}

	public static int packedTick(int packed) {
		return packed >>> NOTE_BITS;
	}

	public static int packedNote(int packed) {
		return packed & NOTE_MASK;
	}

	public int[] packed() {
		return this.packed.clone();
	}

	public boolean isEmpty() {
		return this.packed.length == 0;
	}

	public int size() {
		return this.packed.length;
	}

	public int tick(int i) {
		return this.packed[i] >>> NOTE_BITS;
	}

	public int note(int i) {
		return this.packed[i] & NOTE_MASK;
	}

	/** Ticks from the first note to just after the last. */
	public int length() {
		return this.packed.length == 0 ? 0 : tick(this.packed.length - 1) + 1;
	}

	/** Whole beats the song takes: it may start only when the beat clock is a multiple of this. */
	public int period(int beat) {
		return Math.max(1, (length() + beat - 1) / beat) * beat;
	}

	/** A note stepped up by the tuning, wrapping like a note block. */
	public static int tuned(int note, int tuning) {
		return Math.floorMod(note + tuning, NOTES);
	}

	/** A vibration frequency (1-15) spread over the note range. */
	public static int noteForFrequency(int frequency) {
		return Math.round((Math.clamp(frequency, 1, 15) - 1) * (NOTES - 1) / 14.0F);
	}

	/** Note block pitch math. */
	public static float pitch(int note) {
		return NoteBlock.getPitchFromNote(note);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof GolemSong other && Arrays.equals(this.packed, other.packed);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.packed);
	}
}
