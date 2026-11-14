package org.polyfrost.flashbacklegacy;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.flashbacklegacy.patch.FlashbackPatcher;
import org.polyfrost.flashbacklegacy.patch.ShimStubs;
import org.spongepowered.asm.mixin.Mixins;

public final class FlashbackLegacy implements PreLaunchEntrypoint {
    public static final String MOD_ID = "flashback_legacy";
    public static final Logger LOGGER = LogManager.getLogger("Flashback Legacy");

    public static boolean flashbackLoaded;

    @Override
    public void onPreLaunch() {
        CustomValue.CvObject target = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow()
                .getMetadata().getCustomValue(MOD_ID + ":target").getAsObject();
        String version = target.get("version").getAsString();
        String sha1 = target.get("sha1").getAsString();
        Path dir = FabricLoader.getInstance().getGameDir().resolve("flashback");

        try {
            Files.createDirectories(dir);
            Path jar = findJar(dir, sha1);
            if (jar == null) {
                LOGGER.error("No Flashback jar found. Put Flashback {} for MC {} in {}", version, target.get("mc").getAsString(), dir);
                return;
            }
            if (!sha1.equals(sha1(jar))) {
                LOGGER.warn("{} is not Flashback {}; patches may not apply", jar.getFileName(), version);
            }
            Path out = dir.resolve("patched");
            for (Path path : FlashbackPatcher.patch(jar, out)) {
                FabricLauncherBase.getLauncher().addToClassPath(path);
            }
            FabricLauncherBase.getLauncher().addToClassPath(ShimStubs.generate(out, FlashbackLegacy.class.getClassLoader()));
            Mixins.addConfiguration(MOD_ID + ".flashback.mixins.json");
            flashbackLoaded = true;
        } catch (IOException e) {
            LOGGER.error("Failed to patch Flashback", e);
        }
    }

    private static Path findJar(Path dir, String sha1) throws IOException {
        Path found = null;
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(dir, "Flashback-*.jar")) {
            for (Path jar : jars) {
                if (sha1.equals(sha1(jar))) return jar;
                found = jar;
            }
        }
        return found;
    }

    private static String sha1(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
