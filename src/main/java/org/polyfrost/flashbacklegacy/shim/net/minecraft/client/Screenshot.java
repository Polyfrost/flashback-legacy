package org.polyfrost.flashbacklegacy.shim.net.minecraft.client;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.pipeline.RenderTarget;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform.NativeImage;

public class Screenshot {
    public static void takeScreenshot(RenderTarget target, Consumer<NativeImage> consumer) {
        net.minecraft.client.render.pipeline.RenderTarget framebuffer = Minecraft.getInstance().getRenderTarget();
        int width = framebuffer.width, height = framebuffer.height;
        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);
        try {
            framebuffer.bindWrite(true);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            framebuffer.unbindWrite();
            NativeImage image = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) image.setPixelABGR(x, y, pixels.getInt((x + (height - 1 - y) * width) * 4) | 0xFF000000);
            }
            consumer.accept(image);
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }
}
