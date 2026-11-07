package org.polyfrost.flashbacklegacy.mixin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.resource.language.Locale;
import net.minecraft.client.resource.manager.ResourceManager;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.resources.language.ClientLanguage;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Mixin(Locale.class)
public abstract class LocaleMixin {
    @Shadow
    Map<String, String> translations;

    @Inject(method = "reload", at = @At("TAIL"))
    private void flashback$loadJsonLang(ResourceManager manager, List<String> languages, CallbackInfo ci) {
        for (String language : languages) {
            try (InputStream in = LocaleMixin.class.getClassLoader().getResourceAsStream("assets/flashback/lang/" + language.toLowerCase(java.util.Locale.ROOT) + ".json")) {
                if (in == null) continue;
                Map<String, String> map = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), new TypeToken<Map<String, String>>() {}.getType());
                translations.putAll(map);
            } catch (Exception ignored) {
            }
        }
        for (String colour : List.of("white", "orange", "magenta", "yellow", "lime", "pink", "gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            flashback$aliasColour(colour, colour);
        }
        flashback$aliasColour("light_blue", "lightBlue");
        flashback$aliasColour("light_gray", "silver");
        ((ClientLanguage) Language.getInstance()).storage = translations;
    }

    @Unique
    private void flashback$aliasColour(String modern, String legacy) {
        String name = translations.get("item.fireworksCharge." + legacy);
        if (name != null) translations.put("color.minecraft." + modern, name);
    }
}
