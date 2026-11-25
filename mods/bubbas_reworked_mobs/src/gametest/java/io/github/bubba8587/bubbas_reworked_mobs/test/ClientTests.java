// dte:A4
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Picks which client game tests (or parts of one) run: `./gradlew :bubbas_reworked_mobs:runClientGameTest
 * -PclientTests=sleepers,gait` runs only those; without the property everything runs (CI). Names: spiders, webs, territories, sleepers, displays,
 * gait, creepers, biomecreepers, pillar, endermen, drowned, factories, graves, yards, variants, camps, outposts, traderposts, cairnforms, shipwrecks, wisps, maps, prone, oracle; cairnsim and pillarmock only when named. (talisman and warding moved to Bubba's Totems & Artifacts.) specs/quality.md, Speed.
 */
public final class ClientTests {
	private static final Set<String> ONLY = Arrays.stream(System.getProperty("bubba.clientTests", "").split(","))
			.map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());

	private ClientTests() {
	}

	/** Whether the client test (or part) with this name should run. */
	public static boolean run(String name) {
		return ONLY.isEmpty() || ONLY.contains(name);
	}

	/** Whether this part was named in -PclientTests (parts that never run in the full suite). */
	public static boolean named(String name) {
		return ONLY.contains(name);
	}
}
