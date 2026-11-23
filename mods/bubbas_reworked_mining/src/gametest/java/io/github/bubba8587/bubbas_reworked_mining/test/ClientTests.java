// dte:A4,C7
package io.github.bubba8587.bubbas_reworked_mining.test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Picks which client game tests (or parts of one) run: `./gradlew :bubbas_reworked_mining:runClientGameTest
 * -PclientTests=quartz` runs only those; without the property everything runs (CI). Names: quartz.
 * specs/quality.md, Speed.
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
}
