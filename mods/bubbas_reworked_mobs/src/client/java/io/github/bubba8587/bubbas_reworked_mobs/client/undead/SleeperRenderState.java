// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.undead;

import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public class SleeperRenderState extends BlockEntityRenderState {
	public UndeadVariant variant = UndeadVariant.SKELETON;
	public SleeperPose pose = SleeperPose.SITTING;
	public Direction facing = Direction.NORTH;
	/** The vanilla mob render state the body, armor and held items are drawn from (its type depends on the variant). */
	public @Nullable HumanoidRenderState humanoid;
}
