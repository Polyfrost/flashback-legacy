package org.polyfrost.flashbacklegacy.shim.net.minecraft.client.sounds;

import net.minecraft.client.Minecraft;
import org.lwjgl.openal.AL10;
import org.polyfrost.flashbacklegacy.mixin.SoundEngineAccessor;
import org.polyfrost.flashbacklegacy.mixin.SoundManagerAccessor;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.audio.Channel;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.audio.Library;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ChannelAccess {
    private final List<Channel> channels = new ArrayList<>();

    public CompletableFuture<ChannelHandle> createHandle(Library.Pool pool) {
        channels.removeIf(channel -> channel.source == 0);
        var engine = ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager()).getEngine();
        int source = ((SoundEngineAccessor) engine).isStarted() ? AL10.alGenSources() : 0;
        Channel channel = new Channel(source);
        if (source != 0) channels.add(channel);
        return CompletableFuture.completedFuture(new ChannelHandle(channel));
    }

    public void clear() {
        channels.forEach(Channel::stop);
        channels.clear();
    }

    public class ChannelHandle {
        private final Channel channel;

        ChannelHandle(Channel channel) {
            this.channel = channel;
        }

        public void execute(Consumer<Channel> action) {
            if (channel.source != 0) action.accept(channel);
        }

        public boolean isStopped() {
            if (!channel.stopped()) return false;
            channel.stop();
            return true;
        }
    }
}
