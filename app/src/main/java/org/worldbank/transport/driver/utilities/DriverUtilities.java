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
                    Log.e("DriverUtilities", "Found no field with serialized name " + nextName);
                }
            }

            // sanity check that all fields have an order and that all ordered fields have been found
            if ((fieldOrder.size() != serializedNameOrder.length) || (fieldOrder.size() != fields.length)) {
                Log.e("DriverUtilities", "Mismatch in field count for ordering");
            }

        } else {
            Log.e("DriverUtilities", "Class " + model.getSimpleName() + " has no JsonPropertyOrder");

            // Should always have JsonPropertyOrder declared by jsonschema2pojo,
            // but just in case, return a list of all fields in order declared if not.

            for (Field field: fields) {
                fieldOrder.add(field.getName());
            }
        }

        return fieldOrder.toArray(new String[fieldOrder.size()]);
    }
}
