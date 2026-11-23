// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A test's GameTestInfo from its helper, so cleanup it registers can run when the test is done (TestCleanup). */
@Mixin(GameTestHelper.class)
public interface GameTestHelperAccessor {
	@Accessor("testInfo")
	GameTestInfo brmTest$info();
}
