package org.worldbank.transport.driver.utilities;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.SerializedName;

import org.joda.time.Chronology;
import org.joda.time.LocalDateTime;
import org.joda.time.chrono.IslamicChronology;
import org.jsonschema2pojo.media.SerializableMedia;
import org.worldbank.transport.driver.staticmodels.DriverConstantFields;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

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

        // the annotation lists fields by their SerializedName,
        // except for the constant fields, which are listed by field name
        JsonPropertyOrder ordering = (JsonPropertyOrder) model.getAnnotation(JsonPropertyOrder.class);

        boolean isConstants = false;
        if (DriverConstantFields.class.equals(model)) {
            isConstants = true;
            Log.d(LOG_LABEL, "Getting field order for constants form class, by field name.");
        }

        Field[] fields = model.getDeclaredFields();

        ArrayList<String> fieldOrder = new ArrayList<>(fields.length);

        if (ordering != null) {
            String[] serializedNameOrder = ordering.value();

            // map the fields' pretty names (SerializedName) to the POJO field name
            HashMap<String, String> nameMap = new HashMap<>(fields.length);
            for (Field field: fields) {
                String fieldName = field.getName();
                String fieldLabel = fieldName;
                if (isConstants) {
                    fieldLabel = fieldName;
                } else {
                    SerializedName serializedNameAnnotation = field.getAnnotation(SerializedName.class);
                    if (serializedNameAnnotation != null) {
                        fieldLabel = serializedNameAnnotation.value();
                    }
                }
                nameMap.put(fieldLabel, fieldName);
            }

            for (String nextName: serializedNameOrder) {
                String nextField = nameMap.get(nextName);
                if (nextField != null) {
                    fieldOrder.add(nextField);
                } else {
                    // JsonPropertyOrder annotation should list fields by their serialized names
                    Log.e(LOG_LABEL, "Found no field with serialized name " + nextName);
                }
            }

            // Sanity check that all fields have an order and that all ordered fields have been found.
            // If not, add the remaining fields after those that did have an order defined,
            // in the order of declaration.
            // For non-section fields, there will be a hidden ID field that doesn't get listed in the order.
            int fieldOrderSize = fieldOrder.size();
            if ((fieldOrderSize != serializedNameOrder.length) || (fieldOrderSize < fields.length - 1)) {
                for (Field field: fields) {
                    String fieldName = field.getName();
                    if (!fieldOrder.contains(fieldName)) {
                        Log.w(LOG_LABEL, "Adding field without explicit ordering declared: " + fieldName);
                        fieldOrder.add(fieldName);
                    }
                }
            }

        } else {
            Log.d(LOG_LABEL, "Class " + model.getSimpleName() + " has no JsonPropertyOrder defined; using order of declaration");

            // Return in order found by getDeclaredFields if no ordering defined in schema.

            for (Field field: fields) {
                fieldOrder.add(field.getName());
            }
        }

        return fieldOrder.toArray(new String[fieldOrder.size()]);
    }

    /**
     * Build labels for list items by using the first n fields from the ordered field list.
     *
     * @param items Collection of items of type sectionClass to be labelled
     * @param sectionClass Class of the collection members
     *
     * @return Collection of labels consisting of the first few fields, separated by hyphens.
     *         Labels returned will be in same order as list of items passed in.
     */
    public static ListItemLabels getListItemLabels(ArrayList items, Class sectionClass, String defaultLabel) {

        final int MAX_NUM_LABEL_FIELDS = 3;

        String[] fieldOrders = getFieldOrder(sectionClass);

        ArrayList<String> labels = new ArrayList<>(items.size());
        ArrayList<String> imagePaths = null;

        // If there are less than MAX_NUM_LABEL_FIELDS fields in the section,
        // use as many as are available.
        ArrayList<Field> labelFields = new ArrayList<>(MAX_NUM_LABEL_FIELDS);
        int numLabelFields = MAX_NUM_LABEL_FIELDS;
        if (numLabelFields > fieldOrders.length) {
            numLabelFields = fieldOrders.length;
        }

        Field mediaField = null;

        // get fields to use for the labels
        try {
            for (int i = 0; i < numLabelFields; i++) {
                String labelFieldName = fieldOrders[i];
                Field labelField = sectionClass.getField(labelFieldName);

                // Do not attempt to use media fields for string label.
                // Use first media field found for image.
                if (labelField.getType().equals(SerializableMedia.class)) {
                    if (mediaField == null) {
                        mediaField = labelField;
                    }
                } else {
                    labelFields.add(labelField);
                }
            }
        } catch(NoSuchFieldException e) {
            e.printStackTrace();
        }

        if (mediaField != null) {
            imagePaths = new ArrayList<>(items.size());
        }

        // build the labels
        try {
            String label;
            int itemsSize = items.size();
            for (int i = 0; i < itemsSize; i++) {
                label = "";
                Object item = items.get(i);

                // get path to image to use
                if (mediaField != null) {
                    String imagePath = "";
                    Object obj = mediaField.get(item);
                    if (obj != null) {
                        SerializableMedia media = (SerializableMedia) obj;
                        imagePath = media.path;
                    }
                    imagePaths.add(imagePath);
                }

                for (Field labelField : labelFields) {
                    Object obj = labelField.get(item);
                    if (obj == null) {
                        continue; // no value entered for this field
                    }

                    String objString = obj.toString();

                    if (objString.length() == 0) {
                        continue; // empty string representation for this field
                    }

                    if (label.length() > 0) {
                        label += " - "; // separator
                    }
                    label += objString;
                }

                // add a default label if no values found to use
                // {Field Label} - {Item #}
                if (label.length() == 0) {
                    label = defaultLabel;
                    // append item number in list (starting with one)
                    label += " - " + String.valueOf(i + 1);
                }

                labels.add(label);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return new ListItemLabels(labels, imagePaths);
    }

    /**
     * Check if current system locale is in a right-to-left language. Useful to support RTL
     * checking on API < 17.
     *
     * @return True if system language displays right to left.
     */
    public static boolean localeIsRTL() {
        return localeIsRTL(Locale.getDefault());
    }

    public static boolean localeIsRTL(Locale locale) {
        Log.d(LOG_LABEL, "Locale " + locale.getDisplayName() + " language " + Locale.getDefault().getLanguage());

        int direction = Character.getDirectionality(locale.getDisplayName().charAt(0));
        return  direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
                direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT;
    }

    /**
     * Check if the system language is Arabic. Use to decide whether to use Hijri date display.
     *
     * @return True if device language is Arabic.
     */
    public static boolean languageIsArabic() {
        return Locale.getDefault().getLanguage().startsWith("ar");
    }

    /**
     * Convert a date to its Hijri calendar representation.
     * Uses Joda for the conversion, as it is already included as a dependency.
     *
     * @param date ISO date/time to format
     * @return Hijri date string
     */
    public static String formatDateAsHijri(Date date) {
        final Chronology hijri = IslamicChronology.getInstanceUTC();

        LocalDateTime todayHijri = new LocalDateTime(date, hijri);

        // TODO: use DateTimeFormatter for something prettier? What is a good format?
        // Does Joda know yet what the month names are?
        return todayHijri.toString();
    }
}
