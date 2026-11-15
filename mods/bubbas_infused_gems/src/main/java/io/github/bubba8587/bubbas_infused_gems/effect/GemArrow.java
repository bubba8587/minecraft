// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import java.util.List;

/** Added to Arrow by a mixin: the gem hit it carries from the bow or crossbow that fired it. */
public interface GemArrow {
	List<GemHits.Entry> bubbas$gemHit();

	void bubbas$setGemHit(List<GemHits.Entry> entries);
}
