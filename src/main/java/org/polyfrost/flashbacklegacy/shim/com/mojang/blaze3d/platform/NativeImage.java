package org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.platform;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class NativeImage implements AutoCloseable {
    private final int width;
    private final int height;
    private final int[] pixels;

    public NativeImage(Format format, int width, int height, boolean zero) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    public NativeImage(int width, int height, boolean zero) {
        this(Format.RGBA, width, height, zero);
    }

    public static NativeImage read(InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        if (image == null) throw new IOException("Unsupported image");
        NativeImage result = new NativeImage(Format.RGBA, image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < result.height; y++) for (int x = 0; x < result.width; x++) result.setPixel(x, y, image.getRGB(x, y));
        return result;
    }

    public static NativeImage read(byte[] bytes) throws IOException {
        return read(new ByteArrayInputStream(bytes));
    }

    public Format format() {
        return Format.RGBA;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPixelABGR(int x, int y) {
        return pixels[x + y * width];
    }

    public void setPixelABGR(int x, int y, int abgr) {
        pixels[x + y * width] = abgr;
    }

    public int getPixel(int x, int y) {
        return swapRB(getPixelABGR(x, y));
    }

    public void setPixel(int x, int y, int argb) {
        setPixelABGR(x, y, swapRB(argb));
    }

    public int[] abgrPixels() {
        return pixels;
    }

    public void resizeSubRectTo(int x, int y, int w, int h, NativeImage target) {
        for (int ty = 0; ty < target.height; ty++) {
            for (int tx = 0; tx < target.width; tx++) {
                target.setPixelABGR(tx, ty, getPixelABGR(x + tx * w / target.width, y + ty * h / target.height));
            }
        }
    }

    public void writeToFile(Path path) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setRGB(x, y, getPixel(x, y));
        ImageIO.write(image, "png", path.toFile());
    }

    private static int swapRB(int c) {
        return c & 0xFF00FF00 | (c & 0xFF) << 16 | c >> 16 & 0xFF;
    }

    @Override
    public void close() {
    }

    public enum Format {
        RGBA;

        public boolean hasAlpha() {
            return true;
        }

        public int alphaOffset() {
            return 24;
        }
    }
}
