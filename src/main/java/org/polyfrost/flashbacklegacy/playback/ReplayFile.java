package org.polyfrost.flashbacklegacy.playback;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;

import java.io.IOException;

final class ReplayFile {
    enum Result { ACTION, NEXT_TICK, END }

    private final PacketByteBuf buf;
    private final String[] actions;
    private final int snapshotStart, snapshotEnd;

    ReplayFile(byte[] bytes) {
        buf = new PacketByteBuf(Unpooled.wrappedBuffer(bytes));
        if (buf.readInt() != 0xD780E884) throw new IllegalArgumentException("Invalid magic");
        actions = new String[buf.readVarInt()];
        for (int i = 0; i < actions.length; i++) actions[i] = buf.readString(32767);
        int snapshotSize = buf.readInt();
        snapshotStart = buf.readerIndex();
        snapshotEnd = snapshotStart + snapshotSize;
        buf.readerIndex(snapshotEnd);
    }

    void playSnapshot(LegacyReplayServer server) throws IOException {
        buf.readerIndex(snapshotStart);
        while (buf.readerIndex() < snapshotEnd) next(server);
    }

    Result next(LegacyReplayServer server) throws IOException {
        if (!buf.isReadable()) return Result.END;
        String action = actions[buf.readVarInt()];
        PacketByteBuf body = new PacketByteBuf(buf.readSlice(buf.readInt()));
        switch (action) {
            case "flashback:action/game_packet" -> server.handleGamePacket(body);
            case "flashback:action/move_entities" -> server.handleMoveEntities(body);
            case "flashback:action/create_local_player" -> server.handleCreateLocalPlayer(body);
            case "flashback:action/real_time_clock_optional" -> server.bridge.handleRtc(body);
            case "flashback:action/accurate_player_position_optional" -> server.handleAccuratePosition(body);
            case "flashback:action/next_tick" -> {
                return Result.NEXT_TICK;
            }
            default -> {
            }
        }
        return Result.ACTION;
    }
}
