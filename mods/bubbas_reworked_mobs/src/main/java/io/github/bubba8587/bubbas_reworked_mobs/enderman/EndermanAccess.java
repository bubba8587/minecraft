// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import org.jspecify.annotations.Nullable;

/** Added to every enderman by mixin/EndermanMixin. The mind is null for endermen that stay vanilla (outside the overworld). */
public interface EndermanAccess {
	@Nullable EndermanMind brm$enderMind();
}
