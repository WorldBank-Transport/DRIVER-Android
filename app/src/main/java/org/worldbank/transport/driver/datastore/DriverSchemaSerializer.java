package org.worldbank.transport.driver.datastore;

import android.util.JsonWriter;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.jsonschema2pojo.media.SerializableMedia;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.staticmodels.DriverSchemaUpload;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;

/**
 * Handles reading and writing records to and from JSON strings.
 *
 * Created by kathrynkillebrew on 1/6/16.
 */
public class DriverSchemaSerializer {

    public static final String LOG_LABEL = "SchemaSerializer";

    public static DriverSchema readRecord(String jsonData) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SerializableMedia.class, new SerializableMedia.SerializableMediaPathStringAdapter());
        Gson gson = builder.create();
        try {
            return gson.fromJson(jsonData, DriverSchema.class);
        } catch (JsonParseException ex) {
            Log.e(LOG_LABEL, "Failed to parse record from JSON");
            ex.printStackTrace();
            return null;
        }
    }

    public static String serializeRecordForStorage(DriverSchema object) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SerializableMedia.class, new SerializableMedia.SerializableMediaPathStringAdapter());
        Gson gson = builder.create();
        try {
            return gson.toJson(object, DriverSchema.class);
        } catch (JsonParseException ex) {
            Log.e(LOG_LABEL, "Failed to serialize record to JSON string");
            ex.printStackTrace();
            return null;
        }
    }

}
