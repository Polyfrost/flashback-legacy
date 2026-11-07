package org.polyfrost.flashbacklegacy.playback;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketFlow;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerPlayerInteractionManager;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.server.network.handler.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;

public class ReplayFakePlayer extends ServerPlayerEntity {
    public ReplayFakePlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile, new ServerPlayerInteractionManager(world));
        networkHandler = new ServerPlayNetworkHandler(server, new Connection(PacketFlow.CLIENTBOUND), this) {
            @Override
            public void sendPacket(Packet packet) {
            }
        };
    }
}
