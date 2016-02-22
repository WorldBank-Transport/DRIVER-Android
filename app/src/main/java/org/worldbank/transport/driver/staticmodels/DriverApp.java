package org.worldbank.transport.driver.staticmodels;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.util.Log;

import org.apache.commons.lang.StringUtils;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

    // TODO: track current schema version
    private static final String CURRENT_SCHEMA = "70c8eb79-c6c0-4aa3-859a-fdae45c9db65";

    // TODO: publish on app server; must be on HTTPS and a direct link (no redirect)
    private static final String SCHEMA_CERT_URL = "https://flibbertigibbet.github.io/DRIVER-Android/driver_android_certificate.pem";

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

        packageNameCertMap = new HashMap<>(1);
        try {
            packageNameCertMap.put(MODELS_BASE_PACKAGE, new URL(SCHEMA_CERT_URL));
            loadSchemaClasses("models.jar");
        } catch (MalformedURLException e) {
            Log.e(LOG_LABEL, "Certificate URL for model packages is invalid");
            e.printStackTrace();
        }

        mContext = this;
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        record = null;
        databaseManager = new RecordDatabaseManager(mContext, amTesting);
    }

    public static Context getContext() {
        return mContext;
    }

    public static String getCurrentSchema() {
        return CURRENT_SCHEMA;
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
     * @return false if a location is set and it's not on Null Island
     */
    public boolean isLocationMissing() {
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
     * Load a model schema jar file. Should only be called on app start or after all records are cleared;
     * otherwise, old schema class references in memory may interfere with  the new ones.
     *
     * @param jarPath Path to the jar file containing the new models.
     * @return True on success
     */
    public boolean loadSchemaClasses(String jarPath) {
        Log.d(LOG_LABEL, "loading schema classes...");

        try {
            // first copy jar file out of assets directory to app data directory for a path to pass
            // http://android-developers.blogspot.com/2011/07/custom-class-loading-in-dalvik.html
            File dexInternalStoragePath = new File(getDir("dex", Context.MODE_PRIVATE), jarPath);

            final int BUF_SIZE = 8 * 1024;
            BufferedInputStream bis = new BufferedInputStream(getAssets().open(jarPath));
            OutputStream dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalStoragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            dexWriter.close();
            bis.close();

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
}
