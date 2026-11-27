// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.client.grave;

import net.minecraft.client.gui.GuiPipeline;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveTextLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

/** The gravestone's edit screen: the stone's front face, with the text laid out as {@link GravestoneRenderer} lays it. */
public class GravestoneEditScreen extends AbstractSignEditScreen {
	/** GUI pixels per block pixel. */
	private static final int PIXEL = 9;
	/** Vanilla's shade for a north/south block face. */
	private static final int FACE_SHADE = 0xFFCCCCCC;
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(BubbasReworkedMobs.MOD_ID, "textures/block/gravestone.png");

	public GravestoneEditScreen(SignBlockEntity sign, SignTextSlot slot, boolean shouldFilter) {
		super(sign, slot, shouldFilter);
	}

	@Override
	protected int getSignTextYOffset() {
		return -1;
	}

	@Override
	protected void extractSignBackground(GuiGraphicsExtractor graphics) {
		graphics.pose().scale(PIXEL, PIXEL);
		graphics.pose().translate(-8.0F, GraveTextLayout.TEXT_CENTRE_Y - 16.0F);
		for (int[] r : GraveTextLayout.FACE_RECTS) {
			int w = r[2] - r[0];
			int h = r[3] - r[1];
			graphics.blit(GuiPipeline.TEXTURED, TEXTURE, r[0], 16 - r[3], r[0], 16 - r[3], w, h, 16, 16, FACE_SHADE);
		}
	}

	@Override
	protected int getSignTextSpacing() {
		return 10;
	}
}
