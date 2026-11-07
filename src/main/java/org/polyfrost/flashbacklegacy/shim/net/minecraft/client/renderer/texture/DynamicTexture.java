package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.renderer.texture;

import java.nio.IntBuffer;
import java.util.function.Supplier;

import org.lwjgl.BufferUtils;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform.NativeImage;
import org.polyfrost.flashbacklegacy.shim.com.mojang.renderpearl.api.textures.GpuTextureView;

import static org.lwjgl.opengl.GL11.*;

public class DynamicTexture extends AbstractTexture {
    private final GlView view;

    public DynamicTexture(Supplier<String> label, NativeImage image) {
        int[] abgr = image.abgrPixels();
        IntBuffer pixels = BufferUtils.createIntBuffer(abgr.length).put(abgr).flip();
        int old = glGetInteger(GL_TEXTURE_BINDING_2D);
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(GL_TEXTURE_2D, old);
        view = new GlView(id);
    }

    public GpuTextureView getTextureView() {
        return view;
    }

    @Override
    public void close() {
        glDeleteTextures(view.id);
    }

    public record GlView(int id) implements GpuTextureView {
        @Override
        public void close() {
        }
    }
}
