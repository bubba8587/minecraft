// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Enderman;
import org.jspecify.annotations.Nullable;

/**
 * A caretaker enderman's current job (specs/endermen.md, "Two jobs"). Not saved: after a reload it picks a job again
 * (what it carries is vanilla's saved carried block, and its destination is found again).
 */
public final class EndermanMind {
	/** What it is doing. */
	public enum Job {
		/** Carry its block to {@link #target()} and put it there (a cairn block, or a block that fills a hole). */
		DELIVER,
		/** Walk to {@link #target()} and take the block that doesn't belong there. */
		TAKE,
		/** Jump on the farmland at {@link #target()} until it is dirt again. */
		TRAMPLE,
		/** Stand in the ring around a complete cairn, staring at it (owner; not work: eye contact still angers it). */
		VIGIL,
		/** A builder whose block can't go in yet: stands in the ring around its cairn, checking again now and then. */
		WAIT
	}

	private @Nullable Job job;
	private @Nullable BlockPos target;
	private long started;
	private boolean busy;

	public @Nullable Job job() {
		return job;
	}

	public @Nullable BlockPos target() {
		return target;
	}

	public long started() {
		return started;
	}

	public void set(Job job, BlockPos target, long now) {
		this.job = job;
		this.target = target;
		this.started = now;
	}

	public void clear() {
		this.job = null;
		this.target = null;
	}

	/** E4: while repairing or building it ignores eye contact. */
	public boolean busy() {
		return busy && job != null && job != Job.VIGIL && job != Job.WAIT;
	}

	public void setBusy(boolean busy) {
		this.busy = busy;
	}

	/** The mind of a reworked (overworld) enderman, or null for a vanilla one. */
	public static @Nullable EndermanMind of(Enderman enderman) {
		return enderman instanceof EndermanAccess access ? access.brm$enderMind() : null;
	}
}
