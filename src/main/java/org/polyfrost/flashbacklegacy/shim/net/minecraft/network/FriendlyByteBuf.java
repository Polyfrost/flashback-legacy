package org.polyfrost.flashbacklegacy.shim.net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.resources.Identifier;

import java.util.UUID;

public class FriendlyByteBuf extends PacketByteBuf {
    public FriendlyByteBuf(ByteBuf parent) {
        super(parent);
    }

    public FriendlyByteBuf writeVarInt$(int value) {
        writeVarInt(value);
        return this;
    }

    public FriendlyByteBuf writeByteArray$(byte[] bytes) {
        writeByteArray(bytes);
        return this;
    }

    public FriendlyByteBuf writeUUID(UUID uuid) {
        writeUuid(uuid);
        return this;
    }

    public UUID readUUID() {
        return readUuid();
    }

    public FriendlyByteBuf writeIdentifier(Identifier id) {
        writeString(id.toString());
        return this;
    }

    public Identifier readIdentifier() {
        return Identifier.parse(readString(32767));
    }

    @Override
    public FriendlyByteBuf writeBoolean(boolean value) {
        super.writeBoolean(value);
        return this;
    }

    @Override
    public FriendlyByteBuf writeByte(int value) {
        super.writeByte(value);
        return this;
    }

    @Override
    public FriendlyByteBuf writeShort(int value) {
        super.writeShort(value);
        return this;
    }

    @Override
    public FriendlyByteBuf writeInt(int value) {
        super.writeInt(value);
        return this;
    }

    @Override
    public FriendlyByteBuf writeLong(long value) {
        super.writeLong(value);
        return this;
    }

    @Override
    public FriendlyByteBuf writeFloat(float value) {
        super.writeFloat(value);
        return this;
    }

    @Override
    public FriendlyByteBuf writeDouble(double value) {
        super.writeDouble(value);
        return this;
    }

    @Override
    public FriendlyByteBuf writeBytes(ByteBuf src) {
        super.writeBytes(src);
        return this;
    }

    @Override
    public FriendlyByteBuf writeBytes(ByteBuf src, int length) {
        super.writeBytes(src, length);
        return this;
    }

    @Override
    public FriendlyByteBuf getBytes(int index, byte[] dst) {
        super.getBytes(index, dst);
        return this;
    }

    @Override
    public FriendlyByteBuf readerIndex(int index) {
        super.readerIndex(index);
        return this;
    }

    @Override
    public FriendlyByteBuf writerIndex(int index) {
        super.writerIndex(index);
        return this;
    }

    @Override
    public FriendlyByteBuf resetWriterIndex() {
        super.resetWriterIndex();
        return this;
    }

    @Override
    public FriendlyByteBuf skipBytes(int length) {
        super.skipBytes(length);
        return this;
    }

    @Override
    public FriendlyByteBuf clear() {
        super.clear();
        return this;
    }
}
