package org.worldbank.transport.driver.staticmodels;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.utilities.DriverUtilities;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.necst.grabnrun.SecureDexClassLoader;
import it.necst.grabnrun.SecureLoaderFactory;


/**
 * Singleton to hold data used across the application.
 *
 * Created by kathrynkillebrew on 12/9/15.
 */
public class DriverApp extends Application {

    private static final String LOG_LABEL = "DriverApp";

    // parent of the models package
    public static final String MODELS_BASE_PACKAGE = "org.worldbank.transport.driver";

    // track current schema version
    private static String currentSchemaVersion;

    private static String SCHEMA_CERT_URL;

    public static final String BACKUP_JAR_NAME = "models.jar";
    public static final String BACKUP_JAR_SCHEMA_VERSION = "f7776177-da77-4f2e-bbb4-e47d1e313187";
    public static final String UPDATED_JAR_NAME = "updatedModels.jar";

    /**
     * Current user.
     */
    private DriverUserInfo userInfo;

    /**
     * Object currently being edited (if any).
     */
    private Record record;

    private static Context mContext;
    private static ConnectivityManager connMgr;
    private static RecordDatabaseManager databaseManager;

    private boolean amTesting = false;
    private boolean useHijri = false;
    private SecureDexClassLoader schemaClassLoader = null;
    private static Map<String, URL> packageNameCertMap = null;

    /**
     * Constructor for use in testing. Can use default constructor instead if not testing.
     *
     * @param amTesting True if in test environment.
     */
    public DriverApp(boolean amTesting) {
        super();
        this.amTesting = amTesting;
        Log.i(LOG_LABEL, "Setting up app with in-memory database for testing");
    }

    public DriverApp() {
        super();
        this.amTesting = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SCHEMA_CERT_URL = getString(R.string.signing_cert_pem_url);

        packageNameCertMap = new HashMap<>(1);
        try {
            packageNameCertMap.put(MODELS_BASE_PACKAGE, new URL(SCHEMA_CERT_URL));
            // check if there is a schema update jar available, and use that first;
            // fall back to backup jar if not
            if (haveUpdatedSchemaJar()) {
                // get current schema version from shared preferences
                String preferencesSchemaVersion = getSchemaVersionFromSharedPreferences();
                if (preferencesSchemaVersion.isEmpty()) {
                    Log.e(LOG_LABEL, "Have an updated schema jar file, but its version was not found in shared preferences!");
                    loadBackupSchema();
                } else if (!loadSchemaClasses(UPDATED_JAR_NAME, preferencesSchemaVersion)) {
                    Log.e(LOG_LABEL, "Failed to load updated schema from stored file; reverting to backup");
                    loadBackupSchema();
                }
            } else {
                loadBackupSchema();
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_LABEL, "Certificate URL for model packages is invalid");
            e.printStackTrace();
        }

        mContext = this;
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        record = null;
        databaseManager = new RecordDatabaseManager(mContext, amTesting);
        useHijri = DriverUtilities.isInSaudiArabiaOrArabic();
    }

    public static Context getContext() {
        return mContext;
    }

    public static String getCurrentSchema() {
        return currentSchemaVersion;
    }

    public static RecordDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Sets current user for app and sets user info in shared preferences.
     * Clears shared preferences for app if null user set (can be used on logout.)
     * @param userInfo DriverUserInfo built from API user response in LoginTask
     */
    public void setUserInfo(DriverUserInfo userInfo) {
        this.userInfo = userInfo;

        if (userInfo != null) {
            userInfo.writeToSharedPreferences(mContext);
        } else {
            // clear shared preferences if user info is reset
            SharedPreferences preferences = mContext.getSharedPreferences(
                    mContext.getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear(); // clears last saved user, if there is one
            editor.apply();
        }
    }

    public DriverUserInfo getUserInfo() {
        // try reading in saved user info if app has none set yet
        if (userInfo == null) {
            userInfo = new DriverUserInfo();
            userInfo.readFromSharedPreferences(mContext);
        }
        return userInfo;
    }

    public boolean useHijri() {
        return useHijri;
    }

    public Object getEditObject() {
        if (record == null) {
            record = new Record();
        }

        return record.getEditObject();
    }

    public DriverConstantFields getEditConstants() {
        if (record != null) {
            return record.getEditConstants();
        }

        Log.w(LOG_LABEL, "No record currently being edited to get constants for!");
        return null;
    }

    /**
     * Check if currently editing record is missing a location reading.
     *
     * @return false if a location is set and it's not on Null Island (or if there is no current record)
     */
    public boolean isLocationMissing() {
        if (record == null) {
            return false;
        }
        DriverConstantFields constantFields = record.getEditConstants();
        if (constantFields != null) {
            if (constantFields.location != null) {
                if (constantFields.location.getLongitude() != 0 || constantFields.location.getLatitude() != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Save currently editing record to the database, and unset the currently editing record.
     * Will clear the currently editing record, whether or not the changes saved successfully.
     *
     * @return True on success
     */
    public boolean saveRecordAndClearCurrentlyEditing() {
        boolean wasSaved = saveRecord();
        clearCurrentlyEditingRecord();
        return wasSaved;
    }

    /**
     * Delete currently editing record. The database manager may not have anything to delete, if
     * current record has not been saved yet. In that case it will simply clear current record
     * from memory and exit to record list.
     */
    public void deleteRecordAndClearCurrentlyEditing() {
        if (record == null) {
            Log.w(LOG_LABEL, "No record to delete");
            return;
        }
        databaseManager.deleteRecord(record.getRecordId());
        clearCurrentlyEditingRecord();
    }

    /**
     * Save currently editing record to the database.
     *
     * @return True on success
     */
    private boolean saveRecord() {
        if (record == null) {
            Log.e(LOG_LABEL, "No currently editing record to save!");
            return false;
        }

        return record.save();
    }

    public Cursor getAllRecords() {
        return databaseManager.readAllRecords();
    }

    public void clearCurrentlyEditingRecord() {
        record = null;
    }

    /**
     * Set the record to be edited by database record ID.
     *
     * @param databaseId _id of record in database
     * @return true on success; will clear edit object on failure
     */
    public boolean setCurrentlyEditingRecord(long databaseId) {
        record = databaseManager.getRecordById(databaseId);
        return record != null;
    }

    public static boolean getIsNetworkAvailable() {
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private String getSchemaVersionFromSharedPreferences() {
        SharedPreferences preferences = getSharedPreferences(
                getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);
        return preferences.getString(getString(R.string.shared_preferences_schema_version), "");
    }

    /**
     * Store schema version on app object and in shared preferences.
     * @param newVersion UUID fo the new schema to use.
     */
    public void setCurrentSchemaVersion(String newVersion) {
        SharedPreferences preferences = getSharedPreferences(
                getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(getString(R.string.shared_preferences_schema_version), newVersion);
        editor.apply();
        currentSchemaVersion = newVersion;
        Log.d("DriverUserInfo", "Updated schema written to shared preferences");
    }

    public static Class getSchemaClass() {
        DriverApp driverApp = (DriverApp) DriverApp.getContext();
        try {
            return driverApp.schemaClassLoader.loadClass(RecordFormSectionManager.MODEL_PACKAGE + "DriverSchema");
        } catch (ClassNotFoundException e) {
            Log.e(LOG_LABEL, "Could not load DriverSchema class!");
            e.printStackTrace();
        }

        return null;
    }

    public static SecureDexClassLoader getSchemaClassLoader() {
        DriverApp driverApp = (DriverApp) DriverApp.getContext();
        return driverApp.schemaClassLoader;
    }

    /**
     * Helper to revert to the backup model classes, if updates not found or could not be loaded.
     */
    public void loadBackupSchema() {
        if (loadSchemaClasses(BACKUP_JAR_NAME, BACKUP_JAR_SCHEMA_VERSION)) {
            Log.d(LOG_LABEL, "Reverted to backup schema");
        } else {
            Log.e(LOG_LABEL, "Could not load backup schema!");
            // should never happen
            Toast toast = Toast.makeText(this, getString(R.string.error_schema_update), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * Helper to check if an updated schema model jar file is available locally.
     *
     * @return True if updated jar available
     */
    public boolean haveUpdatedSchemaJar() {
        File updatedJarPath = new File(getDir("dex", Context.MODE_PRIVATE), UPDATED_JAR_NAME);
        return updatedJarPath.exists();
    }

    /**
     * Load a model schema jar file. Should only be called on app start or after all records are cleared;
     * otherwise, old schema class references in memory may interfere with  the new ones.
     *
     * @param jarPath Relative path to the jar file containing the new models.
     * @param schemaVersion UUID of new schema, to be stored after successful class load
     * @return True on success
     */
    public boolean loadSchemaClasses(String jarPath, String schemaVersion) {
        Log.d(LOG_LABEL, "loading schema classes...");

        try {
            File dexInternalStoragePath = new File(getDir("dex", Context.MODE_PRIVATE), jarPath);

            // if loading fallback model jar file from assets for the first time, it will need
            // to be copied first out to the app data directory

            boolean copiedOk = false;
            if (!dexInternalStoragePath.exists()) {
                InputStream inputStream = getAssets().open(jarPath);
                OutputStream outputStream = new FileOutputStream(dexInternalStoragePath);
                try {
                    IOUtils.copy(inputStream, outputStream);
                    copiedOk = true;
                } catch (IOException e) {
                    Log.e(LOG_LABEL, "Failed to copy out model jar file");
                    // return after streams have been closed
                } finally {
                    IOUtils.closeQuietly(inputStream);
                    IOUtils.closeQuietly(outputStream);
                }

                if (!copiedOk) {
                    return false;
                }
            }

            schemaClassLoader = null;
            String modelPackageName = RecordFormSectionManager.MODEL_PACKAGE;
            SecureLoaderFactory secureLoaderFactory = new SecureLoaderFactory(this);
            schemaClassLoader = secureLoaderFactory.createDexClassLoader(dexInternalStoragePath.getAbsolutePath(),
                    null, getClass().getClassLoader(), packageNameCertMap);

            Class newSchema = schemaClassLoader.loadClass(modelPackageName + "DriverSchema");

            if (newSchema == null) {
                // might get here if cert link not HTTPS, or is a redirect,
                // or if cert does not match key used to sign model jar file
                Log.e(LOG_LABEL, "Failed to load class! Is signing certificate available?");
                return false;
            }

            // recursively reload all the child classes from DriverSchema and its fields
            recursiveClassLoad(newSchema);
            Field[] fields = newSchema.getDeclaredFields();
            for (Field field: fields) {
                String fieldClassName = modelPackageName + StringUtils.capitalize(field.getName());
                Log.d(LOG_LABEL, "Found section " + fieldClassName + " for field " + field.getName());
                Log.d(LOG_LABEL, "Dynamically loading section " + fieldClassName);
                Class sectionClass = schemaClassLoader.loadClass(fieldClassName);
                recursiveClassLoad(sectionClass);
            }

            Log.d(LOG_LABEL, "Done dynamically loading schema classes");
            setCurrentSchemaVersion(schemaVersion);
            return true;

        } catch (ClassNotFoundException e) {
            Log.e(LOG_LABEL, "Could not find class");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(LOG_LABEL, "Error copying jar file out to data directory");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Dynamically load inner classes on sections
     *
     * @param clazz Already-loaded class to examine for inner classes
     */
    private void recursiveClassLoad(Class clazz) {
        try {
            enumClassLoad(clazz);
            Class[] hasClasses = clazz.getDeclaredClasses();
            if (hasClasses != null && hasClasses.length > 0) {
                for (Class child : hasClasses) {
                    Log.d(LOG_LABEL, "Going to dynamically load class: " + child.getName());
                    child = schemaClassLoader.loadClass(child.getName());
                    // recurse
                    recursiveClassLoad(child);
                }
            }
        } catch (ClassNotFoundException e) {
            Log.e(LOG_LABEL, "Could not find class to dynamically load");
            e.printStackTrace();
        }
    }

    /**
     * Find and load enums that are in separate classes (not inner classes)
     * @param clazz Section class to introspect for enum class references
     */
    private void enumClassLoad(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Class fieldType = field.getType();
            if (fieldType.equals(Set.class) || fieldType.equals(List.class)) {
                Annotation[] annotations = field.getDeclaredAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().equals(FieldType.class)) {
                        FieldTypes type = ((FieldType) annotation).value();
                        if (type.equals(FieldTypes.selectlist)) {
                            String enumClassName = RecordFormSectionManager.MODEL_PACKAGE +
                                    StringUtils.capitalize(field.getName()) + "Enum";
                            Log.d(LOG_LABEL, "Going to dynamically load class: " + enumClassName);
                            try {
                                schemaClassLoader.loadClass(enumClassName);
                            } catch (ClassNotFoundException e) {
                                Log.e(LOG_LABEL, "Could not find enum class to dynamically load for " + field.getName());
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
}
