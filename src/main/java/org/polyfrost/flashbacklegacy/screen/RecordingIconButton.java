package org.polyfrost.flashbacklegacy.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.resource.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RecordingIconButton extends ButtonWidget {
    private static final Map<String, Identifier> ICONS = new HashMap<>();

    public final String label;
    private final String icon;

    public RecordingIconButton(int id, int x, int y, String label, String icon) {
        super(id, x, y, 20, 20, "");
        this.label = label;
        this.icon = icon;
    }

    private static Identifier texture(Minecraft mc, String icon) {
        return ICONS.computeIfAbsent(icon, name -> {
            try (InputStream in = RecordingIconButton.class.getClassLoader().getResourceAsStream("assets/flashback/icon_pixelated_" + name + ".png")) {
                BufferedImage image = ImageIO.read(in);
                return mc.getTextureManager().register("flashback_icon_" + name, new DynamicTexture(image));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void render(Minecraft mc, int mouseX, int mouseY) {
        super.render(mc, mouseX, mouseY);
        mc.getTextureManager().bind(texture(mc, icon));
        GlStateManager.color4f(1, 1, 1, 1);
        drawTexture(x + 2, y + 2, 0, 0, 16, 16, 16, 16);
    }
}
