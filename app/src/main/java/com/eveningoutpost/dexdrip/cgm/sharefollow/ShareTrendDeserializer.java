package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Dex_Constants;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ShareTrendDeserializer implements JsonDeserializer<Dex_Constants.TREND_ARROW_VALUES> {
    @Override
    public Dex_Constants.TREND_ARROW_VALUES deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return Dex_Constants.TREND_ARROW_VALUES.getEnum(jsonElement.getAsString());
    }
}
