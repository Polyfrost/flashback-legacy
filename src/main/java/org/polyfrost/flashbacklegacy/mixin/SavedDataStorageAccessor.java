package org.polyfrost.flashbacklegacy.mixin;

import net.minecraft.world.saveddata.SavedData;
import net.minecraft.world.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SavedDataStorage.class)
public interface SavedDataStorageAccessor {
    @Accessor
    Map<String, SavedData> getSavedDataById();
}
