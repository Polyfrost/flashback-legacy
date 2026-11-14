package org.polyfrost.flashbacklegacy.record;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.record.Recorder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.NetworkProtocol;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketFlow;
import net.minecraft.network.packet.Packet;
import org.polyfrost.flashbacklegacy.FlashbackLegacy;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.ConnectionProtocol;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.codec.StreamCodec;

import java.io.IOException;
import java.io.UncheckedIOException;

public record LegacyPacket(int id, Body body) implements org.polyfrost.flashbacklegacy.shim.net.minecraft.network.protocol.Packet<Object> {
    public static final StreamCodec<ByteBuf, Object> CODEC = new StreamCodec<>() {
        @Override
        public Object decode(ByteBuf buffer) {
            throw new UnsupportedOperationException("playback isn't ported yet");
        }

        @Override
        public void encode(ByteBuf buffer, Object value) {
            LegacyPacket packet = (LegacyPacket) value;
            PacketByteBuf out = buffer instanceof PacketByteBuf buf ? buf : new PacketByteBuf(buffer);
            out.writeVarInt(packet.id);
            try {
                packet.body.write(out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    };

    public static LegacyPacket of(Packet<?> packet) {
        return new LegacyPacket(NetworkProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet), packet::write);
    }

    public static LegacyPacket raw(Packet<?> like, Body body) {
        return new LegacyPacket(NetworkProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, like), body);
    }

    public static void record(Packet<?> packet) {
        if (!FlashbackLegacy.flashbackLoaded) return;
        Recorder recorder = Flashback.RECORDER;
        if (recorder != null && recorder.readyToWrite()) recorder.writePacketAsync(of(packet), ConnectionProtocol.PLAY);
    }

    @FunctionalInterface
    public interface Body {
        void write(PacketByteBuf buf) throws IOException;
    }
}
