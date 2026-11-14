package org.polyfrost.flashbacklegacy.shim.com.mojang.authlib.properties;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class PropertyMap {
    public static class Serializer implements JsonSerializer<PropertyMap>, JsonDeserializer<PropertyMap> {
        @Override
        public JsonElement serialize(PropertyMap map, Type type, JsonSerializationContext context) {
            return JsonNull.INSTANCE;
        }

        @Override
        public PropertyMap deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return new PropertyMap();
        }
    }
}
