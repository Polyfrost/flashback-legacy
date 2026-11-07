package org.polyfrost.flashbacklegacy.mixin.flashback;

import java.util.function.Consumer;

import com.moulberry.flashback.screen.RecoverRecordingsScreen;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RecoverRecordingsScreen.class, remap = false)
public interface RecoverRecordingsScreenAccessor {
    @Accessor("title")
    Component flashback$getTitle();

    @Accessor("messageText")
    Component flashback$getMessageText();

    @Accessor("handler")
    Consumer<RecoverRecordingsScreen.RecoveryOption> flashback$getHandler();
}
