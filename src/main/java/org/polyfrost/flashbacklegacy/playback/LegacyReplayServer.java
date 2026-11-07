package org.polyfrost.flashbacklegacy.playback;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Connection;
import net.minecraft.network.NetworkProtocol;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketFlow;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.server.command.handler.CommandRegistry;
import net.minecraft.server.integrated.IntegratedPlayerManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;
import net.minecraft.world.gen.WorldGeneratorType;
import org.apache.commons.io.FileUtils;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.packet.FlashbackAccurateEntityPosition;
import com.moulberry.flashback.visuals.AccurateEntityPositionHandler;
import com.moulberry.flashback.keyframe.handler.ReplayServerKeyframeHandler;
import com.moulberry.flashback.playback.ReplayServer;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.mixin.EntityAccessor;
import org.polyfrost.flashbacklegacy.mixin.ServerPlayerEntityAccessor;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.FriendlyByteBuf;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LegacyReplayServer extends IntegratedServer {
    public static final String SAVE_NAME = "flashback-replay";
    public static final int VIEWER_ID_START = -981723987;
    public static final int HOTBAR_MENU = -2;
    private static final Map<Integer, String> OBJECT_KEYS = Map.ofEntries(
            Map.entry(1, "Boat"), Map.entry(2, "Item"), Map.entry(50, "PrimedTnt"), Map.entry(51, "EnderCrystal"),
            Map.entry(60, "Arrow"), Map.entry(61, "Snowball"), Map.entry(62, "ThrownEgg"), Map.entry(63, "Fireball"),
            Map.entry(64, "SmallFireball"), Map.entry(65, "ThrownEnderpearl"), Map.entry(66, "WitherSkull"),
            Map.entry(72, "EyeOfEnderSignal"), Map.entry(73, "ThrownPotion"), Map.entry(75, "ThrownExpBottle"),
            Map.entry(76, "FireworksRocketEntity"), Map.entry(78, "ArmorStand"));
    private static final String[] MINECART_KEYS = {"MinecartRideable", "MinecartChest", "MinecartFurnace", "MinecartTNT",
            "MinecartSpawner", "MinecartHopper", "MinecartCommandBlock"};

    public static Path pending;

    private final FileSystem fileSystem;
    private final TreeMap<Integer, Path> chunkFiles = new TreeMap<>();
    private final Set<Path> forceSnapshot = new HashSet<>();
    private final Map<Path, Integer> chunkSources = new HashMap<>();
    private static volatile int source;
    public final int totalTicks;
    public final com.moulberry.flashback.playback.ReplayServer bridge;
    private int chunkStart;
    private int timePacketTick;
    private long packetTime, packetTimeOfDay;
    private ReplayFile reader;
    private final Map<UUID, GameProfile> profiles = new HashMap<>();
    private final Set<UUID> listed = new HashSet<>();
    private final List<Packet<?>> joinPackets = new ArrayList<>();
    private final Set<WorldChunk> recordedChunks = new HashSet<>(), clearedChunks = new HashSet<>();
    private final Set<String> teams = new HashSet<>(), objectives = new HashSet<>();
    private final Map<ServerPlayerEntity, UUID> cameras = new HashMap<>();
    private final ItemStack[] hotbar = new ItemStack[9];
    private volatile boolean hotbarRecorded;
    private volatile int selectedSlot, food = 20, xp, xpLevel;
    private volatile float saturation = 5, xpProgress;
    private int localPlayerId = -1;
    private int dimension;
    private int nextViewerId = VIEWER_ID_START;
    private int failures;
    private int followDelay;
    public volatile int tick;
    public volatile int targetTick;
    public volatile long lastTickNanos;

    public static boolean separateViewerTicks;
    public static volatile boolean worldHeld;
    private static boolean tickingViewer;
    private static final Set<String> LOGGED = new HashSet<>();
    public static final int SYNC_ID = 0x46425343;
    public static volatile boolean clientSynced, serverSynced;

    public static void tickViewer(net.minecraft.world.World world, Entity viewer) {
        tickingViewer = true;
        try {
            world.tickEntity(viewer);
        } finally {
            tickingViewer = false;
        }
    }

    public static boolean skipClientTick(Entity entity) {
        return separateViewerTicks && !tickingViewer && entity == Minecraft.getInstance().player;
    }

    public static net.minecraft.client.entity.living.player.ClientPlayerEntity spectatedPlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getServer() instanceof LegacyReplayServer && mc.getCamera() instanceof net.minecraft.client.entity.living.player.ClientPlayerEntity p && p != mc.player ? p : null;
    }

    public static boolean spectate(java.util.UUID uuid) {
        if (!(Minecraft.getInstance().getServer() instanceof LegacyReplayServer server)) return false;
        server.executeTask(() -> {
            for (ServerPlayerEntity viewer : server.getPlayerManager().getAll()) {
                Entity target = viewer.getServerWorld().getEntity(uuid);
                if (target != null) viewer.setCamera(target);
            }
        });
        return true;
    }

    public static void logOnce(String what, Throwable e) {
        if (LOGGED.add(what)) FlashbackLegacy.LOGGER.error("Replay: failed to {}", what, e);
    }

    public static boolean isReplayEntity(Entity entity) {
        return entity.world instanceof ServerWorld world && world.getServer() instanceof LegacyReplayServer
                && (!(entity instanceof ServerPlayerEntity) || entity instanceof ReplayFakePlayer);
    }

    public static void command(String[] args) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (args[0].equals("play")) {
                Flashback.openReplayWorld(Path.of("flashback/replays", args[1]).toAbsolutePath());
                return;
            }
            if (!(minecraft.getServer() instanceof LegacyReplayServer server)) {
                minecraft.gui.getChat().addMessage(new LiteralText("Not in a replay"));
                return;
            }
            ReplayServer bridge = server.bridge;
            switch (args[0]) {
                case "pause" -> bridge.replayPaused = true;
                case "resume" -> bridge.replayPaused = false;
                case "speed" -> bridge.setDesiredTickRate(Math.max(1, 20 * Float.parseFloat(args[1])), true);
                case "seek" -> bridge.goToReplayTick(Math.clamp(Integer.parseInt(args[1]), 0, server.totalTicks));
                default -> throw new IllegalArgumentException();
            }
        } catch (RuntimeException e) {
            minecraft.gui.getChat().addMessage(new LiteralText("Usage: /flashback-legacy play <file> | pause | resume | speed <x> | seek <tick>"));
        }
    }

    public static void play(Path path) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.world != null) {
            IntegratedServer old = minecraft.getServer();
            minecraft.world.disconnect();
            minecraft.setWorld(null);
            while (old != null && !old.hasStopped()) java.util.concurrent.locks.LockSupport.parkNanos(10_000_000);
        }
        FileUtils.deleteQuietly(new File(minecraft.gameDir, "saves/" + SAVE_NAME));
        pending = path;
        WorldSettings settings = new WorldSettings(0L, WorldSettings.GameMode.SPECTATOR, false, false, WorldGeneratorType.FLAT)
                .setGeneratorOptions("3;minecraft:air;1;")
                .enableCommands();
        minecraft.startGame(SAVE_NAME, "Replay", settings);
    }

    public LegacyReplayServer(Minecraft minecraft, String saveName, String worldName, WorldSettings settings) {
        super(minecraft, saveName, worldName, settings);
        CommandRegistry commands = (CommandRegistry) getCommandHandler();
        commands.register(new HideCommand(true));
        commands.register(new HideCommand(false));
        Path path = pending;
        pending = null;
        try {
            fileSystem = FileSystems.newFileSystem(path);
            JsonObject metadata = JsonParser.parseString(Files.readString(fileSystem.getPath("/metadata.json"))).getAsJsonObject();
            int ticks = 0, recording = 0;
            for (Map.Entry<String, com.google.gson.JsonElement> entry : metadata.getAsJsonObject("chunks").entrySet()) {
                JsonObject meta = entry.getValue().getAsJsonObject();
                Path file = fileSystem.getPath("/" + entry.getKey());
                chunkFiles.put(ticks, file);
                if (meta.has("forcePlaySnapshot") && meta.get("forcePlaySnapshot").getAsBoolean()) {
                    forceSnapshot.add(file);
                    recording++;
                }
                chunkSources.put(file, recording);
                ticks += meta.get("duration").getAsInt();
            }
            totalTicks = ticks;
            bridge = new ReplayServer(this, metadata);
        } catch (IOException e) {
            throw new RuntimeException("Unable to open replay " + path, e);
        }
    }

    @Override
    protected boolean init() throws IOException {
        if (!super.init()) return false;
        setPlayerManager(new IntegratedPlayerManager(this) {
            @Override
            public void onLogin(Connection connection, ServerPlayerEntity player) {
                player.setNetworkId(nextViewerId++);
                player.dimension = dimension;
                Entity local = getWorld(dimension).getEntity(localPlayerId);
                if (local != null) moveBehind(player, local);
                joinPackets.stream().filter(p -> p instanceof PlayerInfoS2CPacket).forEach(connection::send);
                super.onLogin(connection, player);
                joinPackets.stream().filter(p -> !(p instanceof PlayerInfoS2CPacket)).forEach(connection::send);
            }
        });
        getPlayerManager().setWorld(worlds);
        GameRules rules = worlds[0].getGameRules();
        for (String rule : new String[]{"doMobSpawning", "doDaylightCycle", "doFireTick", "mobGriefing", "doTileDrops", "doEntityDrops"}) {
            rules.set(rule, "false");
        }
        rules.set("randomTickSpeed", "0");
        for (ServerWorld world : worlds) world.savingDisabled = true;

        open(chunkFiles.firstEntry());
        playSnapshot();
        return true;
    }

    public long tickMillis() {
        return exporting() ? 1 : Math.max(1, Math.round(1000 / bridge.tickRate()));
    }

    private static boolean exporting() {
        return Flashback.EXPORT_JOB != null && Flashback.EXPORT_JOB.isRunning();
    }

    public void resetViewerChunks(double x, double y, double z) {
        executeTask(() -> {
            followDelay = 0;
            for (ServerPlayerEntity viewer : getPlayerManager().getAll()) {
                var chunks = viewer.getServerWorld().getChunkMap();
                chunks.removePlayer(viewer);
                viewer.networkHandler.teleport(x, y, z, viewer.yaw, viewer.pitch);
                chunks.addPlayer(viewer);
            }
        });
    }

    public int localPlayerId() {
        return localPlayerId;
    }

    private void playSnapshot() throws IOException {
        bridge.isProcessingSnapshot = true;
        try {
            reader.playSnapshot(this);
        } finally {
            bridge.isProcessingSnapshot = false;
        }
        forgetClearedChunks();
        bridge.updateRtc();
        cameras.forEach((viewer, uuid) -> {
            Entity target = world().getEntity(uuid);
            if (target != null) viewer.setCamera(target);
        });
        cameras.clear();
        if (forceSnapshot.contains(chunkFiles.get(chunkStart))) followDelay = Math.min(followDelay, 1);
    }

    @Override
    protected void tick() {
        lastTickNanos = System.nanoTime();
        boolean finishing = bridge.sendFinishedServerTick.get();
        boolean exporting = exporting();
        if (exporting && !(finishing && clientSynced)) return;
        int ticked = tick;
        try {
            int jump = bridge.jumpToTick;
            boolean playing = jump < 0 && !bridge.replayPaused;
            if (jump >= 0) {
                bridge.jumpToTick = -1;
                targetTick = jump;
                followDelay = Math.min(followDelay, 1);
            } else if (playing) {
                if (targetTick < totalTicks) targetTick++;
                else bridge.replayPaused = true;
            }
            applyKeyframes();
            if (targetTick != tick && !(bridge.isFrozen() && targetTick > tick)) {
                if (!playing || targetTick != tick + 1) {
                    seek(targetTick);
                } else if (!advanceTick()) {
                    bridge.replayPaused = true;
                    targetTick = tick;
                }
            }
            if (followDelay > 0 && --followDelay == 0) followLocalPlayer();
            if (exporting ? tick != ticked && tick % 20 == 0 : getTicks() % 20 == 0) {
                for (ServerPlayerEntity viewer : getPlayerManager().getAll()) viewer.getServerWorld().getEntityMap().updateVisibility(viewer);
            }
        } catch (IOException e) {
            FlashbackLegacy.LOGGER.error("Replay playback failed", e);
            bridge.replayPaused = true;
        }
        var data = world().getData();
        long time = data.getTime(), timeOfDay = data.getTimeOfDay();
        super.tick();
        if (bridge.isFrozen()) {
            data.setTime(time);
            data.setTimeOfDay(timeOfDay);
        }
        if (finishing && bridge.sendFinishedServerTick.compareAndSet(true, false) && Flashback.EXPORT_JOB != null) {
            for (ServerPlayerEntity viewer : getPlayerManager().getAll()) viewer.networkHandler.sendPacket(new KeepAliveS2CPacket(SYNC_ID));
            Flashback.EXPORT_JOB.onFinishedServerTick();
        }
    }

    private void seek(int target) throws IOException {
        Map.Entry<Integer, Path> entry = chunkFiles.floorEntry(target);
        if (target < tick || entry.getKey() > chunkStart) {
            open(entry);
            playSnapshot();
            tick = chunkStart;
            timePacketTick = chunkStart;
        }
        bridge.fastForwarding = true;
        try {
            while (tick < target - 1 && advanceTick()) ;
        } finally {
            bridge.fastForwarding = false;
        }
        if (tick < target) advanceTick();
    }

    private void open(Map.Entry<Integer, Path> entry) throws IOException {
        chunkStart = entry.getKey();
        source = chunkSources.get(entry.getValue());
        reader = new ReplayFile(Files.readAllBytes(entry.getValue()));
    }

    private boolean advanceTick() throws IOException {
        while (true) {
            switch (reader.next(this)) {
                case NEXT_TICK -> {
                    tick++;
                    bridge.updateRtc();
                    return true;
                }
                case END -> {
                    Map.Entry<Integer, Path> next = chunkFiles.higherEntry(chunkStart);
                    if (next == null) return false;
                    open(next);
                    if (forceSnapshot.contains(next.getValue())) playSnapshot();
                }
                case ACTION -> {
                }
            }
        }
    }

    private void applyKeyframes() {
        bridge.setDesiredTickRate(20, false);
        bridge.setFrozen(false, -1);
        try {
            bridge.getEditorState().applyKeyframes(new ReplayServerKeyframeHandler(bridge), targetTick);
        } catch (RuntimeException | LinkageError e) {
            fail("apply keyframes", e);
        }
        bridge.updateFrozen();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            fileSystem.close();
        } catch (IOException ignored) {
        }
        FileUtils.deleteQuietly(new File(getRunDir(), "saves/" + SAVE_NAME));
    }

    public long recordedTime() {
        return packetTime + tick - timePacketTick;
    }

    public long recordedTimeOfDay() {
        return packetTimeOfDay < 0 ? packetTimeOfDay : packetTimeOfDay + tick - timePacketTick;
    }

    private ServerWorld world() {
        return getWorld(dimension);
    }

    void handleGamePacket(PacketByteBuf buf) {
        int id = buf.readVarInt();
        Packet<?> packet;
        try {
            packet = NetworkProtocol.PLAY.createPacket(PacketFlow.CLIENTBOUND, id);
            packet.read(buf);
        } catch (Exception e) {
            fail("decode packet " + id, e);
            buf.readerIndex(buf.writerIndex());
            return;
        }
        try {
            apply(packet);
        } catch (Exception e) {
            fail("apply " + packet.getClass().getSimpleName(), e);
        }
    }

    private void apply(Packet<?> packet) throws IOException {
        ServerWorld world = world();
        switch (packet) {
            case LoginS2CPacket p -> {
                for (ServerPlayerEntity viewer : getPlayerManager().getAll()) {
                    if (viewer.getCamera() != viewer) cameras.put(viewer, viewer.getCamera().getUuid());
                }
                for (ServerWorld w : worlds) clearEntities(w);
                clearChunks();
                joinPackets.clear();
                unlistPlayers();
                clearScoreboard();
                localPlayerId = p.getEntityId();
                changeDimension(p.getDimension());
            }
            case PlayerRespawnS2CPacket p -> changeDimension(p.getDimension());
            case PlayerMoveS2CPacket p -> followDelay = Math.min(followDelay, 1);
            case WorldChunkS2CPacket p -> fillChunk(p.getChunkX(), p.getChunkZ(), p.getChunkData(), p.getSections(), p.isFull());
            case WorldChunksS2CPacket p -> {
                for (int i = 0; i < p.getChunkCount(); i++) fillChunk(p.getChunkX(i), p.getChunkZ(i), p.getData(i), p.getSections(i), true);
            }
            case BlockUpdateS2CPacket p -> setBlock(p.getPos(), p.getBlockState());
            case BlocksUpdateS2CPacket p -> {
                for (BlocksUpdateS2CPacket.BlockUpdate update : p.getUpdates()) setBlock(update.getBlockPos(), update.getBlockState());
            }
            case PlayerInfoS2CPacket p -> {
                for (PlayerInfoS2CPacket.Entry entry : p.getEntries()) {
                    UUID uuid = entry.getProfile().getId();
                    if (p.getAction() == PlayerInfoS2CPacket.Action.ADD_PLAYER) {
                        profiles.put(uuid, entry.getProfile());
                        listed.add(uuid);
                    } else if (p.getAction() == PlayerInfoS2CPacket.Action.REMOVE_PLAYER) listed.remove(uuid);
                }
                broadcast(p, true);
            }
            case AddPlayerS2CPacket p -> {
                GameProfile profile = profiles.getOrDefault(p.getUuid(), new GameProfile(p.getUuid(), "Player"));
                Entity entity = new ReplayFakePlayer(this, world, profile);
                entity.getSyncedData().update(p.getDataEntries());
                spawn(entity, p.getId(), p.getX() / 32.0, p.getY() / 32.0, p.getZ() / 32.0, p.getYaw(), p.getPitch());
            }
            case AddMobS2CPacket p -> {
                Entity entity = Entities.create(p.getType(), world);
                if (entity == null) return;
                entity.getSyncedData().update(p.getDataEntries());
                spawn(entity, p.getId(), p.getX() / 32.0, p.getY() / 32.0, p.getZ() / 32.0, p.getYaw(), p.getPitch());
                entity.setHeadYaw(p.getHeadYaw() * 360 / 256f);
            }
            case AddEntityS2CPacket p -> spawnObject(p, world);
            case AddExperienceOrbS2CPacket p -> {
                NbtCompound nbt = entityNbt("XPOrb");
                nbt.putInt("Value", p.getXp());
                Entity entity = Entities.create(nbt, world);
                spawn(entity, p.getId(), p.getX() / 32.0, p.getY() / 32.0, p.getZ() / 32.0, (byte) 0, (byte) 0);
            }
            case AddPaintingS2CPacket p -> {
                NbtCompound nbt = hangingNbt("Painting", p.getPos(), p.getFacing().getIdHorizontal());
                nbt.putString("Motive", p.getMotive());
                spawnAt(Entities.create(nbt, world), p.getId());
            }
            case EntityDataS2CPacket p -> {
                Entity entity = world.getEntity(p.getId());
                if (entity != null) entity.getSyncedData().update(p.getDataEntries());
            }
            case EntityEquipmentS2CPacket p -> {
                Entity entity = world.getEntity(p.getId());
                if (entity != null) entity.setEquipment(p.getEquipmentSlot(), p.getItem());
            }
            case RemoveEntitiesS2CPacket p -> {
                for (int id : p.getIds()) {
                    Entity entity = world.getEntity(id);
                    if (entity != null) world.removeEntity(entity);
                }
            }
            case WorldTimeS2CPacket p -> {
                timePacketTick = tick;
                packetTime = p.getTime();
                packetTimeOfDay = p.getTimeOfDay();
                world.getData().setTime(p.getTime());
                world.getData().setTimeOfDay(Math.abs(p.getTimeOfDay()));
            }
            case GameEventS2CPacket p -> {
                int event = p.getEvent();
                if (event == 1 || event == 2 || event == 7 || event == 8) broadcast(p, true);
            }
            case WorldBorderS2CPacket p -> broadcast(p, true);
            case TabListS2CPacket p -> broadcast(p, true);
            case TeamS2CPacket p -> {
                if (p.getAction() == 0) teams.add(p.getName());
                else if (p.getAction() == 1) teams.remove(p.getName());
                broadcast(p, true);
            }
            case ScoreboardObjectiveS2CPacket p -> {
                if (p.getAction() == 0) objectives.add(p.getName());
                else if (p.getAction() == 1) objectives.remove(p.getName());
                broadcast(p, true);
            }
            case ScoreboardScoreS2CPacket p -> broadcast(p, true);
            case ScoreboardDisplayS2CPacket p -> broadcast(p, true);
            case SoundEventS2CPacket p -> broadcast(p, false);
            case WorldEventS2CPacket p -> broadcast(p, false);
            case ParticleS2CPacket p -> broadcast(p, false);
            case ExplosionS2CPacket p -> broadcast(p, false);
            case BlockEventS2CPacket p -> broadcast(p, false);
            case BlockMiningProgressS2CPacket p -> broadcast(p, false);
            case AddGlobalEntityS2CPacket p -> broadcast(p, false);
            case BlockEntityUpdateS2CPacket p -> {
                BlockEntity blockEntity = world.getBlockEntity(p.getPos());
                if (blockEntity == null) return;
                blockEntity.readNbt(p.getNbt());
                world.getChunkMap().onBlockChanged(p.getPos());
            }
            case SignUpdateS2CPacket p -> {
                if (!(world.getBlockEntity(p.getPos()) instanceof SignBlockEntity sign)) return;
                System.arraycopy(p.getLines(), 0, sign.lines, 0, sign.lines.length);
                world.getChunkMap().onBlockChanged(p.getPos());
            }
            case MapDataS2CPacket p -> broadcast(p, true);
            case PlayerHealthS2CPacket p -> {
                food = p.getHunger();
                saturation = p.getSaturation();
            }
            case PlayerXpS2CPacket p -> {
                xpProgress = p.getLevelProgress();
                xp = p.getXp();
                xpLevel = p.getLevel();
            }
            case SelectSlotS2CPacket p -> selectedSlot = p.getSlot();
            case InventoryMenuSlotContentS2CPacket p -> {
                if (p.getMenuId() != HOTBAR_MENU || p.getSlotId() < 0 || p.getSlotId() >= hotbar.length) return;
                hotbar[p.getSlotId()] = p.getItem();
                hotbarRecorded = true;
            }
            case ChatMessageS2CPacket p -> broadcast(p, false);
            case TitlesS2CPacket p -> broadcast(p, false);
            case EntityAnimationS2CPacket p -> sendForEntity(p, p.getId());
            case EntityEventS2CPacket p -> sendForEntity(p, p.getEntity(world));
            case EntityVelocityS2CPacket p -> sendForEntity(p, p.getId());
            case EntityStatusEffectS2CPacket p -> sendForEntity(p, p.getId());
            case EntityRemoveStatusEffectS2CPacket p -> sendForEntity(p, p.getId());
            case EntityAttributesS2CPacket p -> sendForEntity(p, p.getEntityId());
            case EntityPickupS2CPacket p -> sendForEntity(p, p.getId());
            case PlayerSleepS2CPacket p -> sendForEntity(p, p.getPlayer(world));
            case AttachEntityS2CPacket p -> {
                Entity entity = world.getEntity(p.getId());
                if (entity == null) return;
                if (p.getType() == 0) entity.startRiding(world.getEntity(p.getAttachedId()));
                else sendForEntity(p, entity);
            }
            default -> {
            }
        }
    }

    private void followLocalPlayer() {
        Entity local = world().getEntity(localPlayerId);
        if (local == null) {
            followDelay = 1;
            return;
        }
        for (ServerPlayerEntity viewer : getPlayerManager().getAll()) moveBehind(viewer, local);
    }

    private static void moveBehind(ServerPlayerEntity viewer, Entity local) {
        double yaw = Math.toRadians(local.yaw);
        double x = local.x + Math.sin(yaw) * 3, y = local.y + 1, z = local.z - Math.cos(yaw) * 3;
        if (viewer.networkHandler == null) viewer.updatePositionAndAngles(x, y, z, local.yaw, local.pitch);
        else viewer.networkHandler.teleport(x, y, z, local.yaw, local.pitch);
    }

    private void unlistPlayers() throws IOException {
        if (listed.isEmpty()) return;
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeVarInt(PlayerInfoS2CPacket.Action.REMOVE_PLAYER.ordinal());
        buf.writeVarInt(listed.size());
        listed.forEach(buf::writeUuid);
        listed.clear();
        PlayerInfoS2CPacket packet = new PlayerInfoS2CPacket();
        packet.read(buf);
        getPlayerManager().sendPacket(packet, dimension);
    }

    private void clearScoreboard() throws IOException {
        for (String team : teams) {
            PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
            buf.writeString(team);
            buf.writeByte(1);
            TeamS2CPacket packet = new TeamS2CPacket();
            packet.read(buf);
            getPlayerManager().sendPacket(packet, dimension);
        }
        for (String objective : objectives) {
            PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
            buf.writeString(objective);
            buf.writeByte(1);
            ScoreboardObjectiveS2CPacket packet = new ScoreboardObjectiveS2CPacket();
            packet.read(buf);
            getPlayerManager().sendPacket(packet, dimension);
        }
        teams.clear();
        objectives.clear();
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeText(new LiteralText(""));
        buf.writeText(new LiteralText(""));
        TabListS2CPacket packet = new TabListS2CPacket();
        packet.read(buf);
        getPlayerManager().sendPacket(packet, dimension);
    }

    public void applyHud(PlayerEntity camera, PlayerEntity viewer) {
        boolean recorded = camera.getNetworkId() == localPlayerId;
        viewer.xpProgress = recorded ? xpProgress : 0;
        viewer.xp = recorded ? xp : 0;
        viewer.xpLevel = recorded ? xpLevel : 0;
        if (!recorded) return;
        camera.getHungerManager().setFoodLevel(food);
        camera.getHungerManager().setSaturationLevel(saturation);
        if (!hotbarRecorded) return;
        System.arraycopy(hotbar, 0, camera.inventory.items, 0, hotbar.length);
        camera.inventory.selectedSlot = selectedSlot;
    }

    private void listPlayer(GameProfile profile) {
        if (!listed.add(profile.getId())) return;
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeVarInt(PlayerInfoS2CPacket.Action.ADD_PLAYER.ordinal());
        buf.writeVarInt(1);
        buf.writeUuid(profile.getId());
        buf.writeString(profile.getName());
        buf.writeVarInt(profile.getProperties().size());
        for (com.mojang.authlib.properties.Property property : profile.getProperties().values()) {
            buf.writeString(property.getName());
            buf.writeString(property.getValue());
            buf.writeBoolean(property.hasSignature());
            if (property.hasSignature()) buf.writeString(property.getSignature());
        }
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeBoolean(false);
        PlayerInfoS2CPacket packet = new PlayerInfoS2CPacket();
        try {
            packet.read(buf);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getPlayerManager().sendPacket(packet, dimension);
    }

    private void clearEntities(ServerWorld world) {
        for (Entity entity : new ArrayList<>(world.entities)) {
            if (isReplayEntity(entity)) world.removeEntityNow(entity);
        }
    }

    private void clearChunks() {
        for (WorldChunk chunk : recordedChunks) {
            new ArrayList<>(chunk.getBlockEntities().keySet()).forEach(chunk.getWorld()::removeBlockEntity);
            Arrays.fill(chunk.getSections(), null);
        }
        clearedChunks.addAll(recordedChunks);
        recordedChunks.clear();
    }

    private void forgetClearedChunks() {
        clearedChunks.removeAll(recordedChunks);
        for (ServerPlayerEntity viewer : getPlayerManager().getAll()) {
            for (WorldChunk chunk : clearedChunks) {
                if (chunk.getWorld() == viewer.world && viewer.getServerWorld().getChunkMap().isChunkWithinView(viewer, chunk.chunkX, chunk.chunkZ)) {
                    viewer.networkHandler.sendPacket(new WorldChunkS2CPacket(chunk, true, 65535));
                }
            }
        }
        clearedChunks.clear();
    }

    private void changeDimension(int dim) {
        if (dim == dimension) return;
        ServerWorld from = world(), to = getWorld(dim);
        Entity local = from.getEntity(localPlayerId);
        clearEntities(from);
        dimension = dim;
        for (ServerPlayerEntity viewer : new ArrayList<>(getPlayerManager().getAll())) {
            if (viewer.world != from) continue;
            viewer.dimension = dim;
            viewer.networkHandler.sendPacket(new PlayerRespawnS2CPacket(dim, to.getDifficulty(), to.getData().getGeneratorType(), viewer.interactionManager.getGameMode()));
            from.removeEntityNow(viewer);
            viewer.removed = false;
            viewer.setWorld(to);
            to.addEntity(viewer);
            getPlayerManager().onChangedDimension(viewer, from);
            viewer.networkHandler.teleport(viewer.x, viewer.y, viewer.z, viewer.yaw, viewer.pitch);
            viewer.interactionManager.setWorld(to);
            getPlayerManager().sendWorldInfo(viewer, to);
            getPlayerManager().sendPlayerInfo(viewer);
        }
        if (local instanceof ReplayFakePlayer old) {
            ReplayFakePlayer player = new ReplayFakePlayer(this, to, old.getGameProfile());
            spawn(player, localPlayerId, old.x, old.y, old.z, old.yaw, old.pitch);
        }
        followDelay = 100;
    }

    private void spawnObject(AddEntityS2CPacket p, ServerWorld world) {
        int type = p.getType(), data = p.getData();
        Entity entity;
        if (type == 70) {
            NbtCompound nbt = entityNbt("FallingSand");
            nbt.putInt("TileID", data & 4095);
            nbt.putByte("Data", (byte) (data >> 12));
            nbt.putByte("Time", (byte) 1);
            entity = Entities.create(nbt, world);
        } else if (type == 71 || type == 77) {
            BlockPos pos = new BlockPos(p.getX() / 32, p.getY() / 32, p.getZ() / 32);
            spawnAt(Entities.create(hangingNbt(type == 71 ? "ItemFrame" : "LeashKnot", pos, data), world), p.getId());
            return;
        } else if (type == 10) {
            entity = Entities.createSilently(MINECART_KEYS[Math.floorMod(data, MINECART_KEYS.length)], world);
        } else {
            String key = OBJECT_KEYS.get(type);
            if (key == null) return;
            entity = Entities.createSilently(key, world);
        }
        if (entity == null) return;
        spawn(entity, p.getId(), p.getX() / 32.0, p.getY() / 32.0, p.getZ() / 32.0, (byte) p.getYaw(), (byte) p.getPitch());
    }

    private static NbtCompound entityNbt(String key) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", key);
        return nbt;
    }

    private static NbtCompound hangingNbt(String key, BlockPos pos, int facing) {
        NbtCompound nbt = entityNbt(key);
        nbt.putInt("TileX", pos.getX());
        nbt.putInt("TileY", pos.getY());
        nbt.putInt("TileZ", pos.getZ());
        nbt.putByte("Facing", (byte) facing);
        return nbt;
    }

    private void spawn(Entity entity, int id, double x, double y, double z, byte yaw, byte pitch) {
        spawn(entity, id, x, y, z, yaw * 360 / 256f, pitch * 360 / 256f);
    }

    private void spawn(Entity entity, int id, double x, double y, double z, float yaw, float pitch) {
        entity.updatePositionAndAngles(x, y, z, yaw, pitch);
        spawnAt(entity, id);
    }

    public static void stabilizeUuid(Entity entity, int id) {
        if (!(entity instanceof PlayerEntity) && Minecraft.getInstance().getServer() instanceof LegacyReplayServer) {
            ((EntityAccessor) entity).setUuid(new UUID(0x466C617368626163L + source, id));
        }
    }

    void handleAccuratePosition(PacketByteBuf buf) {
        if (bridge.fastForwarding || Flashback.getConfig().advanced.disableIncreasedFirstPersonUpdates) return;
        FlashbackAccurateEntityPosition packet = (FlashbackAccurateEntityPosition) FlashbackAccurateEntityPosition.STREAM_CODEC.decode(new FriendlyByteBuf(buf));
        org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft.SCHEDULED.add(() -> AccurateEntityPositionHandler.update(packet));
    }

    private void spawnAt(Entity entity, int id) {
        if (entity == null) return;
        stabilizeUuid(entity, id);
        if (entity instanceof ReplayFakePlayer player) listPlayer(player.getGameProfile());
        ServerWorld world = world();
        Entity old = world.getEntity(id);
        if (old != null) world.removeEntityNow(old);
        for (ServerPlayerEntity viewer : getPlayerManager().getAll()) ((ServerPlayerEntityAccessor) viewer).getRemovedEntities().remove(Integer.valueOf(id));
        entity.setNetworkId(id);
        world.addEntity(entity);
    }

    private void sendForEntity(Packet<?> packet, int id) {
        sendForEntity(packet, world().getEntity(id));
    }

    private void sendForEntity(Packet<?> packet, Entity entity) {
        if (entity != null && !bridge.fastForwarding) world().getEntityMap().sendPacket(entity, packet);
    }

    private void broadcast(Packet<?> packet, boolean onJoin) {
        if (onJoin) {
            if (getPlayerManager().getCount() == 0) joinPackets.add(packet);
        } else if (bridge.fastForwarding) return;
        getPlayerManager().sendPacket(packet, dimension);
    }

    private void fillChunk(int x, int z, byte[] data, int sections, boolean full) {
        if (full && sections == 0) return;
        ServerWorld world = world();
        WorldChunk chunk = world.getChunkAt(x, z);
        if (full) new ArrayList<>(chunk.getBlockEntities().keySet()).forEach(world::removeBlockEntity);
        chunk.update(data, sections, full);
        recordedChunks.add(chunk);
        resendChunk(x, z);
    }

    private void setBlock(BlockPos pos, net.minecraft.block.state.BlockState state) {
        ServerWorld world = world();
        WorldChunk chunk = world.getChunkAt(pos.getX() >> 4, pos.getZ() >> 4);
        recordedChunks.add(chunk);
        WorldChunkSection[] sections = chunk.getSections();
        int index = pos.getY() >> 4;
        if (index < 0 || index >= sections.length) return;
        if (sections[index] == null) sections[index] = new WorldChunkSection(index << 4, !world.dimension.hasNoSky());
        if (sections[index].getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15).getBlock() != state.getBlock()
                && chunk.getBlockEntities().containsKey(pos)) world.removeBlockEntity(pos);
        sections[index].setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
        world.getChunkMap().onBlockChanged(pos);
        world.checkLight(pos);
    }

    private void resendChunk(int x, int z) {
        if (getPlayerManager() == null || getPlayerManager().getCount() == 0) return;
        for (int y = 0; y < 256; y++) world().getChunkMap().onBlockChanged(new BlockPos(x * 16 + (y & 15), y, z * 16));
    }

    void handleMoveEntities(PacketByteBuf buf) {
        int levels = buf.readVarInt();
        for (int i = 0; i < levels; i++) {
            buf.readString(32767);
            int count = buf.readVarInt();
            for (int j = 0; j < count; j++) {
                int id = buf.readVarInt();
                double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
                float yaw = buf.readFloat(), pitch = buf.readFloat(), headYaw = buf.readFloat();
                boolean onGround = buf.readBoolean();
                Entity entity = world().getEntity(id);
                if (entity == null) continue;
                if (entity.vehicle == null) entity.updatePositionAndAngles(x, y, z, yaw, pitch);
                else {
                    entity.yaw = yaw;
                    entity.pitch = pitch;
                }
                entity.setHeadYaw(headYaw);
                entity.onGround = onGround;
            }
        }
    }

    void handleCreateLocalPlayer(PacketByteBuf buf) throws IOException {
        UUID uuid = buf.readUuid();
        double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
        float pitch = buf.readFloat(), yaw = buf.readFloat(), headYaw = buf.readFloat();
        buf.skipBytes(24);
        GameProfile profile = new GameProfile(buf.readUuid(), buf.readString(16));
        int properties = buf.readVarInt();
        for (int i = 0; i < properties; i++) {
            String name = buf.readString(32767), value = buf.readString(32767);
            String signature = buf.readBoolean() ? buf.readString(32767) : null;
            profile.getProperties().put(name, new com.mojang.authlib.properties.Property(name, value, signature));
        }
        buf.readVarInt();
        profiles.putIfAbsent(uuid, profile);
        ServerWorld world = world();
        Entity existing = world.getEntity(localPlayerId);
        if (existing != null) {
            existing.updatePositionAndAngles(x, y, z, yaw, pitch);
            return;
        }
        Entity player = new ReplayFakePlayer(this, world, profile);
        player.updatePositionAndAngles(x, y, z, yaw, pitch);
        player.setHeadYaw(headYaw);
        spawnAt(player, localPlayerId);
        world.getData().setSpawnPoint(new BlockPos(x, y, z));
    }

    private void fail(String what, Throwable e) {
        if (failures++ < 16) FlashbackLegacy.LOGGER.error("Replay: failed to {}", what, e);
    }
}
