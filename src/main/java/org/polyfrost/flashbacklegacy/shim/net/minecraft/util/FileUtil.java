package org.polyfrost.flashbacklegacy.shim.net.minecraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
    public static String findAvailableName(Path dir, String name, String extension) throws IOException {
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        String candidate = name + extension;
        for (int i = 1; Files.exists(dir.resolve(candidate)); i++) candidate = name + " (" + i + ")" + extension;
        return candidate;
    }
}
