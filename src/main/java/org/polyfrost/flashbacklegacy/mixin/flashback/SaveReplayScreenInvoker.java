package org.polyfrost.flashbacklegacy.mixin.flashback;

import com.moulberry.flashback.screen.SaveReplayScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = SaveReplayScreen.class, remap = false)
public interface SaveReplayScreenInvoker {
    @Accessor("replayName")
    String flashback$getReplayName();

    @Invoker("setReplayName")
    void flashback$setReplayName(String name);

    @Invoker("saveReplay")
    void flashback$saveReplay();

    @Invoker("deleteReplay")
    void flashback$deleteReplay();
}
