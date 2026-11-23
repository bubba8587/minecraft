// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.test.mixin.GameTestHelperAccessor;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;

/**
 * Cleanup a test registers for whatever it put outside its own area (a far-off Fabricator site, forced chunks, its
 * players there). It runs once the test is done, pass or fail ({@code TestAreaCleanupMixin}), where a test's own
 * success path alone would leave everything behind on a failure for the next tests to trip over.
 */
public final class TestCleanup {
	private static final Map<GameTestInfo, List<Runnable>> HOOKS = new WeakHashMap<>();

	private TestCleanup() {
	}

	/** Runs {@code action} when {@code helper}'s test is done, whether it passed or failed. */
	static void whenDone(GameTestHelper helper, Runnable action) {
		GameTestInfo info = ((GameTestHelperAccessor) helper).brmTest$info();
		synchronized (HOOKS) {
			HOOKS.computeIfAbsent(info, i -> new ArrayList<>()).add(action);
		}
	}

	/** Called by {@code TestAreaCleanupMixin} once {@code info} is done. */
	public static void run(GameTestInfo info) {
		List<Runnable> actions;
		synchronized (HOOKS) {
			actions = HOOKS.remove(info);
		}
		if (actions == null) {
			return;
		}
		for (Runnable action : actions) {
			try {
				action.run();
			} catch (RuntimeException e) {
				System.out.println("TEST CLEANUP " + info.id() + " failed: " + e);
			}
		}
	}
}
