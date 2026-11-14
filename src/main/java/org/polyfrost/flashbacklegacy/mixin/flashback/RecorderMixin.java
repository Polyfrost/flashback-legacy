package org.polyfrost.flashbacklegacy.mixin.flashback;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.action.ActionCreateLocalPlayer;
import com.moulberry.flashback.action.ActionMoveEntities;
import com.moulberry.flashback.io.AsyncReplaySaver;
import com.moulberry.flashback.io.ReplayWriter;
import com.moulberry.flashback.record.Recorder;
import io.netty.buffer.Unpooled;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.client.network.PlayerInfo;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.SyncedData;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.attribute.EntityAttributeContainer;
import net.minecraft.entity.living.player.HungerManager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.AttachEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDataS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentS2CPacket;
import net.minecraft.network.packet.s2c.play.GameEventS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryMenuSlotContentS2CPacket;
import net.minecraft.network.packet.s2c.play.LoginS2CPacket;
import net.minecraft.network.packet.s2c.play.MapDataS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerHealthS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerInfoS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerXpS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreS2CPacket;
import net.minecraft.network.packet.s2c.play.SelectSlotS2CPacket;
import net.minecraft.network.packet.s2c.play.SpawnPointS2CPacket;
import net.minecraft.network.packet.s2c.play.TabListS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldChunkS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.scoreboard.team.Team;
import net.minecraft.server.TrackedEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.map.SavedMapData;
import net.minecraft.world.saveddata.SavedData;
import org.polyfrost.flashbacklegacy.mixin.ClientChunkCacheAccessor;
import org.polyfrost.flashbacklegacy.mixin.MinecraftAccessor;
import org.polyfrost.flashbacklegacy.mixin.PlayerTabOverlayAccessor;
import org.polyfrost.flashbacklegacy.mixin.SavedDataStorageAccessor;
import org.polyfrost.flashbacklegacy.mixin.TrackedEntityInvoker;
import org.polyfrost.flashbacklegacy.playback.LegacyReplayServer;
import org.polyfrost.flashbacklegacy.record.EntityPosition;
import org.polyfrost.flashbacklegacy.record.LegacyPacket;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer.ClientLevel;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.codec.StreamCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.FutureTask;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(value = Recorder.class, remap = false)
public abstract class RecorderMixin {
    @Shadow
    @Final
    private AsyncReplaySaver asyncReplaySaver;
    @Shadow
    private StreamCodec gamePacketCodec;
    @Shadow
    private long lastRtcEpochMilli;
    @Shadow
    private boolean forceRtcSync;
    @Shadow
    private volatile boolean skippedPacketDueToWaitingForWrite;
    @Shadow
    private int lastSelectedSlot;
    @Shadow
    private float lastExperienceProgress;
    @Shadow
    private int lastTotalExperience;
    @Shadow
    private int lastExperienceLevel;
    @Shadow
    private int lastFoodLevel;
    @Shadow
    private float lastSaturationLevel;
    @Shadow
    private boolean wasSwinging;
    @Shadow
    private int lastSwingTime;

    @Unique
    private final Map<Entity, EntityPosition> lastPositions = new WeakHashMap<>();
    @Unique
    private final ItemStack[] lastEquipment = new ItemStack[5];
    @Unique
    private final ItemStack[] lastHotbar = new ItemStack[9];
    @Unique
    private WeakReference<LocalClientPlayerEntity> lastLocalPlayer;
    @Unique
    private byte[] lastLocalData;

    @Shadow
    private void writeRtc(boolean force) {
        throw new AssertionError();
    }

    @Overwrite
    public void writeSnapshot(boolean asActualSnapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (skippedPacketDueToWaitingForWrite) {
            skippedPacketDueToWaitingForWrite = false;
            Queue<FutureTask<?>> tasks = ((MinecraftAccessor) minecraft).getTasks();
            synchronized (tasks) {
                for (FutureTask<?> task; (task = tasks.poll()) != null; ) task.run();
            }
        }

        if (asActualSnapshot) asyncReplaySaver.submit(ReplayWriter::startSnapshot);
        if (lastRtcEpochMilli == 0L) {
            writeRtc(true);
        } else {
            forceRtcSync = true;
        }

        ClientWorld world = minecraft.world;
        LocalClientPlayerEntity player = minecraft.player;
        ClientPlayNetworkHandler handler = minecraft.getNetworkHandler();
        List packets = new ArrayList<>();
        packets.add(LegacyPacket.of(new LoginS2CPacket(player.getNetworkId(), minecraft.interactionManager.getGameMode(), world.getData().isHardcore(),
                world.dimension.getId(), world.getDifficulty(), handler.maxPlayerCount, world.getData().getGeneratorType(), player.hasReducedDebugInfo())));
        asyncReplaySaver.writeGamePackets(gamePacketCodec, packets);
        packets.clear();
        writeCreateLocalPlayer();

        List<PlayerInfo> players = new ArrayList<>(handler.getOnlinePlayers());
        packets.add(LegacyPacket.raw(new PlayerInfoS2CPacket(), buf -> {
            buf.writeVarInt(0);
            buf.writeVarInt(players.size());
            for (PlayerInfo info : players) {
                writeProfile(buf, info.getProfile(), info.getProfile().getProperties());
                buf.writeVarInt(info.getGameMode() == null ? 0 : info.getGameMode().getId());
                buf.writeVarInt(info.getPing());
                Text name = info.getDisplayName();
                buf.writeBoolean(name != null);
                if (name != null) buf.writeText(name);
            }
        }));
        packets.add(LegacyPacket.of(new WorldBorderS2CPacket(world.getWorldBorder(), WorldBorderS2CPacket.Type.INITIALIZE)));
        packets.add(LegacyPacket.of(new WorldTimeS2CPacket(world.getTime(), world.getTimeOfDay(), world.getGameRules().getBoolean("doDaylightCycle"))));
        packets.add(LegacyPacket.of(new SpawnPointS2CPacket(world.getSpawnPoint())));
        packets.add(LegacyPacket.of(new GameEventS2CPacket(world.isRaining() ? 1 : 2, 0)));
        packets.add(LegacyPacket.of(new GameEventS2CPacket(7, world.getRain(1))));
        packets.add(LegacyPacket.of(new GameEventS2CPacket(8, world.getThunder(1))));

        for (WorldChunk chunk : new ArrayList<>(((ClientChunkCacheAccessor) world.getChunkSource()).getChunks())) {
            packets.add(LegacyPacket.of(new WorldChunkS2CPacket(chunk, true, 0xFFFF)));
        }
        for (BlockEntity blockEntity : new ArrayList<>(world.blockEntities)) {
            Packet<?> packet = blockEntity.createUpdatePacket();
            if (packet != null) packets.add(LegacyPacket.of(packet));
        }

        for (Entity entity : new ArrayList<>(world.entities)) {
            if (entity == player) continue;
            try {
                packets.add(LegacyPacket.of(((TrackedEntityInvoker) new TrackedEntity(entity, 0, 0, true)).invokeCreateAddEntityPacket()));
            } catch (IllegalArgumentException e) {
                continue;
            }
            int id = entity.getNetworkId();
            packets.add(LegacyPacket.of(new EntityDataS2CPacket(id, entity.getSyncedData(), true)));
            if (entity instanceof LivingEntity living) {
                if (living.getAttributes() instanceof EntityAttributeContainer attributes && !attributes.getTrackable().isEmpty()) {
                    packets.add(LegacyPacket.of(new EntityAttributesS2CPacket(id, attributes.getTrackable())));
                }
                ItemStack[] equipment = living.getEquipment();
                for (int slot = 0; slot < equipment.length; slot++) {
                    if (equipment[slot] != null) packets.add(LegacyPacket.of(new EntityEquipmentS2CPacket(id, slot, equipment[slot])));
                }
            }
            if (entity.vehicle != null) packets.add(LegacyPacket.of(new AttachEntityS2CPacket(0, entity, entity.vehicle)));
        }

        writeScoreboard(world.getScoreboard(), packets);
        PlayerTabOverlayAccessor tabList = (PlayerTabOverlayAccessor) minecraft.gui.getPlayerTabOverlay();
        Text header = tabList.getHeader(), footer = tabList.getFooter();
        packets.add(LegacyPacket.raw(new TabListS2CPacket(), buf -> {
            buf.writeText(header == null ? new LiteralText("") : header);
            buf.writeText(footer == null ? new LiteralText("") : footer);
        }));
        for (Map.Entry<String, SavedData> entry : ((SavedDataStorageAccessor) world.getSavedDataStorage()).getSavedDataById().entrySet()) {
            if (entry.getValue() instanceof SavedMapData map) {
                int id = Integer.parseInt(entry.getKey().substring("map_".length()));
                packets.add(LegacyPacket.of(new MapDataS2CPacket(id, map.scale, map.decorations.values(), map.colors, 0, 0, 128, 128)));
            }
        }

        asyncReplaySaver.writeGamePackets(gamePacketCodec, packets);
        if (asActualSnapshot) asyncReplaySaver.submit(ReplayWriter::endSnapshot);
        lastLocalPlayer = null;
    }

    @Overwrite
    private void writeLocalData() {
        LocalClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean first = false;
        if (lastLocalPlayer == null || lastLocalPlayer.get() != player) {
            lastLocalPlayer = new WeakReference<>(player);
            lastSelectedSlot = -1;
            lastLocalData = null;
            Arrays.fill(lastEquipment, null);
            lastExperienceProgress = -1;
            lastTotalExperience = -1;
            lastExperienceLevel = -1;
            lastFoodLevel = -1;
            lastSaturationLevel = -1;
            first = true;
        }

        int id = player.getNetworkId();
        List packets = new ArrayList<>();
        if (player.inventory.selectedSlot != lastSelectedSlot) {
            lastSelectedSlot = player.inventory.selectedSlot;
            packets.add(LegacyPacket.of(new SelectSlotS2CPacket(lastSelectedSlot)));
        }

        if (Flashback.getConfig().recording.recordHotbar) {
            if (player.xpProgress != lastExperienceProgress || player.xp != lastTotalExperience || player.xpLevel != lastExperienceLevel) {
                lastExperienceProgress = player.xpProgress;
                lastTotalExperience = player.xp;
                lastExperienceLevel = player.xpLevel;
                packets.add(LegacyPacket.of(new PlayerXpS2CPacket(player.xpProgress, player.xp, player.xpLevel)));
            }
            HungerManager hunger = player.getHungerManager();
            if (hunger.getFoodLevel() != lastFoodLevel || hunger.getSaturationLevel() != lastSaturationLevel) {
                lastFoodLevel = hunger.getFoodLevel();
                lastSaturationLevel = hunger.getSaturationLevel();
                packets.add(LegacyPacket.of(new PlayerHealthS2CPacket(player.getHealth(), lastFoodLevel, lastSaturationLevel)));
            }
            for (int slot = 0; slot < lastHotbar.length; slot++) {
                ItemStack stack = player.inventory.items[slot];
                if (first || !ItemStack.matches(lastHotbar[slot], stack)) {
                    lastHotbar[slot] = stack == null ? null : stack.copy();
                    packets.add(LegacyPacket.of(new InventoryMenuSlotContentS2CPacket(LegacyReplayServer.HOTBAR_MENU, slot, lastHotbar[slot])));
                }
            }
        }

        byte[] data = serialize(player.getSyncedData());
        if (!Arrays.equals(data, lastLocalData)) {
            lastLocalData = data;
            packets.add(LegacyPacket.raw(new EntityDataS2CPacket(), buf -> {
                buf.writeVarInt(id);
                buf.writeBytes(data);
            }));
        }

        ItemStack[] equipment = player.getEquipment();
        for (int slot = 0; slot < equipment.length; slot++) {
            if (!ItemStack.matches(lastEquipment[slot], equipment[slot])) {
                lastEquipment[slot] = equipment[slot] == null ? null : equipment[slot].copy();
                packets.add(LegacyPacket.of(new EntityEquipmentS2CPacket(id, slot, lastEquipment[slot])));
            }
        }

        if (player.armSwinging && (!wasSwinging || lastSwingTime > player.armSwingingTicks)) {
            packets.add(LegacyPacket.of(new EntityAnimationS2CPacket(player, 0)));
        }
        lastSwingTime = player.armSwingingTicks;
        wasSwinging = player.armSwinging;

        if (!packets.isEmpty()) asyncReplaySaver.writeGamePackets(gamePacketCodec, packets);
    }

    @Overwrite
    private void writeEntityPositions() {
        ClientWorld world = Minecraft.getInstance().world;
        if (world == null) {
            lastPositions.clear();
            return;
        }

        List<EntityPosition> changed = new ArrayList<>();
        for (Entity entity : world.entities) {
            EntityPosition position = new EntityPosition(entity.getNetworkId(), entity.x, entity.y, entity.z, MathHelper.wrapDegrees(entity.yaw),
                    MathHelper.wrapDegrees(entity.pitch), MathHelper.wrapDegrees(entity.getHeadYaw()), entity.onGround);
            if (!position.equals(lastPositions.put(entity, position))) changed.add(position);
        }
        if (changed.isEmpty()) return;

        String dimension = ClientLevel.of(world).dimension().identifier().toString();
        asyncReplaySaver.submit(writer -> {
            writer.startAction(ActionMoveEntities.INSTANCE);
            PacketByteBuf buf = writer.friendlyByteBuf();
            buf.writeVarInt(1);
            buf.writeString(dimension);
            buf.writeVarInt(changed.size());
            for (EntityPosition position : changed) {
                buf.writeVarInt(position.id());
                buf.writeDouble(position.x());
                buf.writeDouble(position.y());
                buf.writeDouble(position.z());
                buf.writeFloat(position.yaw());
                buf.writeFloat(position.pitch());
                buf.writeFloat(position.headYaw());
                buf.writeBoolean(position.onGround());
            }
            writer.finishAction(ActionMoveEntities.INSTANCE);
        });
    }

    @Overwrite
    private void writeCreateLocalPlayer() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalClientPlayerEntity player = minecraft.player;
        if (player == null) return;
        UUID uuid = player.getUuid();
        double x = player.x, y = player.y, z = player.z;
        float pitch = player.pitch, yaw = player.yaw, headYaw = player.getHeadYaw();
        double velocityX = player.velocityX, velocityY = player.velocityY, velocityZ = player.velocityZ;
        GameProfile profile = player.getGameProfile();
        PropertyMap properties = new PropertyMap();
        properties.putAll(minecraft.getProfileProperties());
        properties.putAll(profile.getProperties());
        int gameMode = minecraft.interactionManager.getGameMode().getId();
        asyncReplaySaver.submit(writer -> {
            writer.startAction(ActionCreateLocalPlayer.INSTANCE);
            PacketByteBuf buf = writer.friendlyByteBuf();
            buf.writeUuid(uuid);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(pitch);
            buf.writeFloat(yaw);
            buf.writeFloat(headYaw);
            buf.writeDouble(velocityX);
            buf.writeDouble(velocityY);
            buf.writeDouble(velocityZ);
            writeProfile(buf, profile, properties);
            buf.writeVarInt(gameMode);
            writer.finishAction(ActionCreateLocalPlayer.INSTANCE);
        });
    }

    @Unique
    private static void writeScoreboard(Scoreboard scoreboard, List packets) {
        for (ScoreboardObjective objective : scoreboard.getObjectives()) packets.add(LegacyPacket.of(new ScoreboardObjectiveS2CPacket(objective, 0)));
        for (ScoreboardScore score : scoreboard.getScores()) packets.add(LegacyPacket.of(new ScoreboardScoreS2CPacket(score)));
        for (int slot = 0; slot < 19; slot++) {
            ScoreboardObjective objective = scoreboard.getDisplayObjective(slot);
            if (objective != null) packets.add(LegacyPacket.of(new ScoreboardDisplayS2CPacket(slot, objective)));
        }
        for (Team team : scoreboard.getTeams()) packets.add(LegacyPacket.of(new TeamS2CPacket(team, 0)));
    }

    @Unique
    private static void writeProfile(PacketByteBuf buf, GameProfile profile, PropertyMap properties) {
        buf.writeUuid(profile.getId());
        buf.writeString(profile.getName());
        buf.writeVarInt(properties.size());
        for (Property property : properties.values()) {
            buf.writeString(property.getName());
            buf.writeString(property.getValue());
            buf.writeBoolean(property.hasSignature());
            if (property.hasSignature()) buf.writeString(property.getSignature());
        }
    }

    @Unique
    private static byte[] serialize(SyncedData data) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            data.write(buf);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Arrays.copyOf(buf.array(), buf.writerIndex());
    }
}
