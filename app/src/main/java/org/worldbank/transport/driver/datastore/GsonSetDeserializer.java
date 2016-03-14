package org.worldbank.transport.driver.datastore;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.worldbank.transport.driver.staticmodels.DriverApp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import it.necst.grabnrun.SecureDexClassLoader;

/**
 * Custom deserializer to deal with Gson being able to serialize but not deserialize some collections.
 * https://sites.google.com/site/gson/gson-user-guide#TOC-Collections-Examples
 * Created by kathrynkillebrew on 3/14/16.
 */
public class GsonSetDeserializer implements JsonDeserializer<Set> {

    private static final String LOG_LABEL = "GsonSetCustom";

    @SuppressWarnings("unchecked")
    @Override
    public Set deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray array = json.getAsJsonArray();
        Log.d(LOG_LABEL, "Type of set json: " + typeOfT.toString());

        String typeString = typeOfT.toString();
        String typeParam = typeString.substring(typeString.indexOf("<") + 1, typeString.lastIndexOf(">"));
        Log.d(LOG_LABEL, "extracted type param: " + typeParam);

        SecureDexClassLoader classLoader = DriverApp.getSchemaClassLoader();
        try {
            Class loadedClass = classLoader.loadClass(typeParam);
            Method fromValueMethod = loadedClass.getDeclaredMethod("fromValue", String.class);

            Set set = new LinkedHashSet(array.size());

            int size = array.size();
            for (int i = 0; i < size; i++) {
                Log.d(LOG_LABEL, "Checked value found: " + array.get(i).getAsString());
                Object checkedEnum = fromValueMethod.invoke(loadedClass, array.get(i).getAsString());
                set.add(checkedEnum);
            }
            
            return set;

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(LOG_LABEL, "Failed to find extracted set class type " + typeParam);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(LOG_LABEL, "Failed to find fromValue method on enum type " + typeParam);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Log.e(LOG_LABEL, "Could not invoke static fromValue method on enum class " + typeParam);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(LOG_LABEL, "Do not have access to invoke fromValue method on enum class " + typeParam);
        }

        return null;
    }
}
