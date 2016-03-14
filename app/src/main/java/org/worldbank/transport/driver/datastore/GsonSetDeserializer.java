package org.worldbank.transport.driver.datastore;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Custom deserializer to deal with Gson being able to serialize but not deserialize some collections.
 * https://sites.google.com/site/gson/gson-user-guide#TOC-Collections-Examples
 * Created by kathrynkillebrew on 3/14/16.
 */
public class GsonSetDeserializer implements JsonDeserializer<Set> {

    private static final String LOG_LABEL = "GsonSetCustom";
    @Override
    public Set deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray array = json.getAsJsonArray();
        Log.d(LOG_LABEL, "Type of set json: " + typeOfT.toString());

        Set set = new LinkedHashSet(array.size());

        return set;
    }
}
