// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client.golem;

import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import io.github.bubba8587.bubbas_world_tweaks.golem.AmethystGolem;
import io.github.bubba8587.bubbas_world_tweaks.golem.AmethystGolems;
import io.github.bubba8587.bubbas_world_tweaks.golem.GolemSong;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** Amethyst Golem renderer. */
public class AmethystGolemRenderer extends MobRenderer<AmethystGolem, AmethystGolemRenderState, AmethystGolemModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(BubbasWorldTweaks.id("amethyst_golem"), "main");
	public static final Identifier TEXTURE = BubbasWorldTweaks.id("textures/entity/amethyst_golem/amethyst_golem.png");

	public AmethystGolemRenderer(EntityRendererProvider.Context context) {
		super(context, new AmethystGolemModel(context.bakeLayer(LAYER)), 0.4F);
	}

	public static void initialize() {
		ModelLayerRegistry.registerModelLayer(LAYER, AmethystGolemModel::createBodyLayer);
		EntityRenderers.register(AmethystGolems.AMETHYST_GOLEM, AmethystGolemRenderer::new);
	}

	@Override
	public Identifier getTextureLocation(AmethystGolemRenderState state) {
		return TEXTURE;
	}

	@Override
	public AmethystGolemRenderState createRenderState() {
		return new AmethystGolemRenderState();
	}

	@Override
	public void extractRenderState(AmethystGolem entity, AmethystGolemRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.dancing = entity.isDancing();
		state.recording = entity.isRecording();
		state.perform = entity.perform(partialTicks);
		state.waiting = entity.waiting(partialTicks);
		state.sinceNote = entity.tickCount - entity.noteTick() + partialTicks;
		state.noteGap = entity.noteGap();
		state.pitch = entity.lastNote() / (float) (GolemSong.NOTES - 1);
		state.prevPitch = entity.prevNote() / (float) (GolemSong.NOTES - 1);
		state.swayProgress = Math.clamp((entity.tickCount - entity.swayTick() + partialTicks) / entity.swayLength(), 0.0F, 1.0F);
		state.swaySide = entity.swayRight() ? 1.0F : -1.0F;
	}
}
