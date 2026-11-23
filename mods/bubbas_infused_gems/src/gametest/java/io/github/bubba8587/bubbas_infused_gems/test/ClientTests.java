// dte:A4
package io.github.bubba8587.bubbas_infused_gems.test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Picks which client game tests run. */
public final class ClientTests {
	private static final Set<String> ONLY = Arrays.stream(System.getProperty("bubba.clientTests", "").split(","))
			.map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());

	private ClientTests() {
	}

	/** Whether the client test with this name should run. */
	public static boolean run(String name) {
		return ONLY.isEmpty() || ONLY.contains(name);
	}
}
