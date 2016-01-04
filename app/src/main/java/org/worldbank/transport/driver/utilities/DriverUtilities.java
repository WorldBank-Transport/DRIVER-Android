package org.worldbank.transport.driver.utilities;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Static helper methods.
 *
 * Created by kathrynkillebrew on 12/28/15.
 */
public class DriverUtilities {

    private static final String LOG_LABEL = "DriverUtilities";

    /**
     * Returns the ordered list of field names for the given schema model.
     * @param model Class built by jsonschema2pojo with the json editor annotations
     * @return Array of the names of the fields on the class, ordered according to the JsonPropertyOrder annotation
     */
    public static String[] getFieldOrder(Class model) {

        // the annotation lists fields by their SerializedName
        JsonPropertyOrder ordering = (JsonPropertyOrder) model.getAnnotation(JsonPropertyOrder.class);

        Field[] fields = model.getDeclaredFields();
        ArrayList<String> fieldOrder = new ArrayList<>(fields.length);

        if (ordering != null) {
            String[] serializedNameOrder = ordering.value();

            // map the fields' pretty names (SerializedName) to the POJO field name
            HashMap<String, String> nameMap = new HashMap<>(fields.length);
            for (Field field: fields) {
                SerializedName serializedNameAnnotation = field.getAnnotation(SerializedName.class);
                String serializedName = serializedNameAnnotation.value();
                nameMap.put(serializedName, field.getName());
            }

            for (String nextName: serializedNameOrder) {
                String nextField = nameMap.get(nextName);
                if (nextField != null) {
                    fieldOrder.add(nextField);
                } else {
                    Log.e(LOG_LABEL, "Found no field with serialized name " + nextName);
                }
            }

            // Sanity check that all fields have an order and that all ordered fields have been found.
            // For non-section fields, there will be a hidden ID field that doesn't get listed in the order.
            int fieldOrderSize = fieldOrder.size();
            if ((fieldOrderSize != serializedNameOrder.length) || (fieldOrderSize < fields.length - 1)) {
                Log.e(LOG_LABEL, "Mismatch in field count for ordering:");
                Log.e(LOG_LABEL, "fieldOrder size: " + fieldOrder.size());
                Log.e(LOG_LABEL, "fields length: " + fields.length);
                Log.e(LOG_LABEL, "serializedNameOrder length: " + serializedNameOrder.length);
            }

        } else {
            Log.e(LOG_LABEL, "Class " + model.getSimpleName() + " has no JsonPropertyOrder");

            // Should always have JsonPropertyOrder declared by jsonschema2pojo,
            // but just in case, return a list of all fields in order declared if not.

            for (Field field: fields) {
                fieldOrder.add(field.getName());
            }
        }

        return fieldOrder.toArray(new String[fieldOrder.size()]);
    }
}
