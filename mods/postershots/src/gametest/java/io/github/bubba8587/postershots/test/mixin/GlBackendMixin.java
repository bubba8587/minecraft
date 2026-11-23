// dte:A3
package io.github.bubba8587.postershots.test.mixin;

import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.renderpearl.backend.opengl.GlBackend;

/** Headless OpenGL: Xvfb offers no sRGB-capable GLX visual, so the poster camera doesn't ask for one (specs/posters.md). */
@Mixin(GlBackend.class)
public class GlBackendMixin {
	@Redirect(method = "createWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SetAttribute(II)Z"))
	private boolean postershots$noSrgbVisual(int attribute, int value) {
		return SDLVideo.SDL_GL_SetAttribute(attribute, attribute == 22 && Boolean.getBoolean("bubba.posters.gl") ? 0 : value);
	}
}
