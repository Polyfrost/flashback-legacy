package org.polyfrost.flashbacklegacy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public final class FlashbackLegacyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!FlashbackLegacy.flashbackLoaded) return;
        try {
            Object flashback = Class.forName("com.moulberry.flashback.Flashback").getConstructor().newInstance();
            ((ModInitializer) flashback).onInitialize();
            ((ClientModInitializer) flashback).onInitializeClient();
            FlashbackLegacy.LOGGER.info("Flashback initialized");
        } catch (Throwable t) {
            FlashbackLegacy.LOGGER.error("Flashback failed to initialize", t);
        }
    }
}
