package org.polyfrost.flashbacklegacy.editor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.moulberry.flashback.FilePlayerSkin;
import com.moulberry.flashback.state.EditorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.client.render.model.PlayerModelPart;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.client.render.texture.SkinImageProcessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Identifier;
import net.minecraft.scoreboard.team.Team;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.mixin.flashback.FilePlayerSkinAccessor;
import org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.GameProfile;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.EquipmentSlot;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EntityOverrides {
    public record Skin(Identifier skin, Identifier cape, String model) {
    }

    private static final Skin LOADING = new Skin(null, null, null);
    private static final Map<UUID, Skin> PROFILE_SKINS = new HashMap<>();
    private static final Map<String, Skin> FILE_SKINS = new HashMap<>();
    private static final ExecutorService FETCHER = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("Flashback skin fetch").setDaemon(true).build());

    private EntityOverrides() {
    }

    private static EditorState state(Entity entity) {
        return entity.world != null && entity.world.isClient ? EditorUi.editorState() : null;
    }

    public static Boolean modelPart(PlayerEntity player, PlayerModelPart part) {
        EditorState state = state(player);
        if (state == null) return null;
        UUID uuid = player.getUuid();
        if (part == PlayerModelPart.CAPE && state.hideCape.contains(uuid)) return false;
        EnumSet<org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.PlayerModelPart> hidden = state.hiddenModelParts.get(uuid);
        return hidden == null ? null : !hidden.contains(org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.PlayerModelPart.values()[part.ordinal()]);
    }

    public static ItemStack[] hideEquipment(Entity entity) {
        EditorState state = state(entity);
        if (state == null || !(entity instanceof LivingEntity living)) return null;
        EnumSet<EquipmentSlot> hidden = state.hiddenEquipment.get(entity.getUuid());
        if (hidden == null) return null;
        ItemStack[] removed = new ItemStack[5];
        for (EquipmentSlot slot : hidden) {
            if (slot.index < 0) continue;
            removed[slot.index] = living.getEquipment(slot.index);
            living.setEquipment(slot.index, null);
        }
        return removed;
    }

    public static void restoreEquipment(Entity entity, ItemStack[] removed) {
        for (int i = 0; i < removed.length; i++) {
            if (removed[i] != null) ((LivingEntity) entity).setEquipment(i, removed[i]);
        }
    }

    public static String nameTag(LivingEntity entity, String vanilla) {
        EditorState state = state(entity);
        if (state == null || !(entity instanceof PlayerEntity player)) return vanilla;
        UUID uuid = player.getUuid();
        String name = state.nameOverride.get(uuid);
        boolean hidePrefix = state.hideTeamPrefix.contains(uuid);
        boolean hideSuffix = state.hideTeamSuffix.contains(uuid);
        if (name == null && !hidePrefix && !hideSuffix) return vanilla;
        Team team = player.getScoreboardTeam() instanceof Team t ? t : null;
        String prefix = team == null || hidePrefix ? "" : team.getPrefix();
        String suffix = team == null || hideSuffix ? "" : team.getSuffix();
        return prefix + (name == null ? player.getName() : name) + suffix;
    }

    public static boolean hidesBelowName(Entity entity) {
        EditorState state = state(entity);
        return state != null && state.hideBelowName.contains(entity.getUuid());
    }

    public static Skin skin(ClientPlayerEntity player) {
        EditorState state = state(player);
        if (state == null) return null;
        UUID uuid = player.getUuid();
        FilePlayerSkin file = state.skinOverrideFromFile.get(uuid);
        if (file != null) return FILE_SKINS.computeIfAbsent(((FilePlayerSkinAccessor) file).getPathToSkin(), EntityOverrides::loadFile);
        GameProfile profile = state.skinOverride.get(uuid);
        if (profile == null) return null;
        Skin skin = PROFILE_SKINS.get(profile.id());
        if (skin == null) {
            PROFILE_SKINS.put(profile.id(), LOADING);
            fetch(profile.id());
            return LOADING;
        }
        return skin;
    }

    private static void fetch(UUID id) {
        FETCHER.execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            try {
                com.mojang.authlib.GameProfile profile = mc.getSessionService().fillProfileProperties(new com.mojang.authlib.GameProfile(id, null), false);
                Skin[] skin = {new Skin(null, null, "default")};
                mc.getSkinManager().register(profile, (type, location, texture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN) {
                        String model = texture.getMetadata("model");
                        skin[0] = new Skin(location, skin[0].cape(), model == null ? "default" : model);
                    } else if (type == MinecraftProfileTexture.Type.CAPE) {
                        skin[0] = new Skin(skin[0].skin(), location, skin[0].model());
                    }
                    PROFILE_SKINS.put(id, skin[0]);
                }, false);
            } catch (RuntimeException e) {
                FlashbackLegacy.LOGGER.warn("Couldn't fetch skin of {}", id, e);
            }
        });
    }

    private static Skin loadFile(String path) {
        try {
            BufferedImage image = new SkinImageProcessor().process(ImageIO.read(new File(path)));
            Identifier id = Minecraft.getInstance().getTextureManager().register("flashback_skin", new DynamicTexture(image));
            return new Skin(id, null, (image.getRGB(54, 20) >>> 24) == 0 ? "slim" : "default");
        } catch (Exception e) {
            FlashbackLegacy.LOGGER.warn("Couldn't load skin {}", path, e);
            return new Skin(null, null, null);
        }
    }
}
