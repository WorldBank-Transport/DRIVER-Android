package org.worldbank.transport.driver.datastore;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.jsonschema2pojo.media.SerializableMedia;
import org.worldbank.transport.driver.staticmodels.DriverApp;

import java.util.Set;


/**
 * Handles reading and writing records to and from JSON strings.
 *
 * Created by kathrynkillebrew on 1/6/16.
 */
public class DriverSchemaSerializer {

    public static final String LOG_LABEL = "SchemaSerializer";

    public static Object readRecord(String jsonData) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SerializableMedia.class, new SerializableMedia.SerializableMediaPathStringAdapter());
        builder.registerTypeAdapter(Set.class, new GsonSetDeserializer());
        Gson gson = builder.create();
        try {
            Class driverClass = DriverApp.getSchemaClass();
            if (driverClass != null) {
                return gson.fromJson(jsonData, driverClass);
            } else {
                Log.e(LOG_LABEL, "Could not read record; driver schema undefined");
            }
        } catch (JsonParseException ex) {
            Log.e(LOG_LABEL, "Failed to parse record from JSON");
            ex.printStackTrace();
        }

        return null;
    }

    public static String serializeRecordForStorage(Object object) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SerializableMedia.class, new SerializableMedia.SerializableMediaPathStringAdapter());
        Gson gson = builder.create();
        try {
            Class driverSchemaClass = DriverApp.getSchemaClass();
            if (driverSchemaClass != null) {
                return gson.toJson(object, driverSchemaClass);
            } else {
                Log.e(LOG_LABEL, "No driver schema class to serialize!");
                return null;
            }
        } catch (JsonParseException ex) {
            Log.e(LOG_LABEL, "Failed to serialize record to JSON string");
            ex.printStackTrace();
            return null;
        }
    }

}
