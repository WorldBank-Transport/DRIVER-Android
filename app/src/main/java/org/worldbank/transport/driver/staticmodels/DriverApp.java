package org.worldbank.transport.driver.staticmodels;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.util.Log;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.DriverSchemaSerializer;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.models.DriverSchema;

import java.sql.Driver;


/**
 * Singleton to hold data used across the application.
 *
 * Created by kathrynkillebrew on 12/9/15.
 */
public class DriverApp extends Application {

    private static final String LOG_LABEL = "DriverApp";

    // TODO: track current schema version
    private static final String CURRENT_SCHEMA = "4407772d-939a-4dcb-9e62-1aec284c2d77";

    /**
     * Current user.
     */
    private DriverUserInfo userInfo;

    /**
     * Object currently being edited (if any).
     */
    private DriverSchema editObject;
    private long editObjectDatabaseId;
    private DriverConstantFields editConstants;

    private static Context mContext;
    private static ConnectivityManager connMgr;
    private static RecordDatabaseManager databaseManager;

    private boolean amTesting = false;

    /**
     * Constructor for use in testing. Can use default constructor instead if not testing.
     *
     * @param amTesting True if in test environment.
     */
    public DriverApp(boolean amTesting) {
        super();
        this.amTesting = amTesting;
    }

    public DriverApp() {
        super();
        this.amTesting = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        editObject = null;
        editObjectDatabaseId = -1;
        editConstants = null;
        databaseManager = new RecordDatabaseManager(mContext, amTesting);
    }

    public static Context getContext() {
        return mContext;
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

    public DriverSchema getEditObject() {
        if (editObject == null) {
            editObject = new DriverSchema();
            editObjectDatabaseId = -1;
            Log.d(LOG_LABEL, "Created new object to edit");
        }

        return editObject;
    }

    public DriverConstantFields getEditConstants() {
        if (editConstants == null) {
            editConstants = new DriverConstantFields();
            Log.d(LOG_LABEL, "Created new constant fields set");
        }

        return editConstants;
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
        if (editObject == null) {
            Log.e(LOG_LABEL, "No currently editing record to save!");
            return false;
        }

        String serializedEditObject = DriverSchemaSerializer.serializeRecord(editObject);

        if (serializedEditObject == null) {
            Log.e(LOG_LABEL, "Failed to serialize record to JSON");
            return false;
        }

        if (editObjectDatabaseId > -1) {
            // update an existing record
            int affected = databaseManager.updateRecord(serializedEditObject, editObjectDatabaseId);
            if (affected == 1) {
                return true;
            } else {
                Log.e(LOG_LABEL, "Failed to update record. Number of affected rows: " + affected);
            }
        } else {
            // add new record
            long newId = databaseManager.addRecord(CURRENT_SCHEMA, serializedEditObject);
            if (newId > -1) {
                return true;
            } else {
                Log.e(LOG_LABEL, "Error inserting record");
            }
        }

        return false;
    }

    public Cursor getAllRecords() {
        return databaseManager.readAllRecords();
    }

    public void clearCurrentlyEditingRecord() {
        editObjectDatabaseId = -1;
        editObject = null;
    }

    /**
     * Set the record to be edited by database record ID.
     *
     * @param databaseId _id of record in database
     * @return true on success; will clear edit object on failure
     */
    public boolean setCurrentlyEditingRecord(long databaseId) {
        editObjectDatabaseId = databaseId;
        String serializedRecordData = databaseManager.getSerializedRecordWithId(databaseId);

        if (serializedRecordData == null) {
            Log.e(LOG_LABEL, "Failed to get record with ID: " + databaseId);
            clearCurrentlyEditingRecord();
            return false;
        }

        editObject = DriverSchemaSerializer.readRecord(serializedRecordData);

        if (editObject == null) {
            clearCurrentlyEditingRecord();
            return false;
        }

        editObjectDatabaseId = databaseId;
        return true;
    }

    public static boolean getIsNetworkAvailable() {
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
