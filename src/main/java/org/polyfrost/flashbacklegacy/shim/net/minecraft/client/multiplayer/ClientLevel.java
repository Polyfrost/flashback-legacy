package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.multiplayer;

import java.util.UUID;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.Minecraft;

import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.player.Player;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.entity.LevelEntityGetter;
import java.util.List;

import net.minecraft.client.world.ClientWorld;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.core.Registry;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.ResourceKey;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.entity.Entity;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.world.level.Level;

public class ClientLevel extends Level {
    private static final ResourceKey<Registry<Level>> DIMENSION = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("dimension"));
    private static ClientLevel last;

    public final ClientWorld handle;

    private ClientLevel(ClientWorld handle) {
        this.handle = handle;
    }

    @Override
    public org.polyfrost.flashbacklegacy.shim.net.minecraft.world.scores.Scoreboard getScoreboard() {
        return new org.polyfrost.flashbacklegacy.shim.net.minecraft.world.scores.Scoreboard(handle.getScoreboard());
    }

    public org.polyfrost.flashbacklegacy.shim.net.minecraft.world.TickRateManager tickRateManager() {
        return new org.polyfrost.flashbacklegacy.shim.net.minecraft.world.TickRateManager();
    }

    public static ClientLevel of(ClientWorld handle) {
        if (handle == null) return null;
        ClientLevel level = last;
        if (level == null || level.handle != handle) last = level = new ClientLevel(handle);
        return level;
    }

    @Override
    public void close() {
    }

    public void disconnect(org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component reason) {
        Minecraft.SCHEDULED.add(handle::disconnect);
    }

    public long getDefaultClockTime() {
        return handle.getTimeOfDay();
    }

    public Entity getEntity(int id) {
        return Entity.of(handle.getEntity(id));
    }

    public ResourceKey<Level> dimension() {
        String name = switch (handle.dimension.getId()) {
            case -1 -> "the_nether";
            case 1 -> "the_end";
            default -> "overworld";
        };
        return ResourceKey.create(DIMENSION, Identifier.withDefaultNamespace(name));
    }

    public LevelEntityGetter<Entity> getEntities() {
        return uuid -> {
            for (net.minecraft.entity.Entity e : handle.entities) {
                if (e.getUuid().equals(uuid)) return Entity.of(e);
            }
            return null;
        };
    }

    public Player getPlayerByUUID(UUID uuid) {
        return (Player) Entity.of(handle.getPlayer(uuid));
    }

    public Iterable<Entity> entitiesForRendering() {
        return handle.entities.stream().map(Entity::of).toList();
    }
}
