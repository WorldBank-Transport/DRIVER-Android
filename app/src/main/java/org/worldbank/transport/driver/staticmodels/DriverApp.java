package org.worldbank.transport.driver.staticmodels;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;
import android.util.Log;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.models.DriverSchema;


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
    private Record record;

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
        Log.i(LOG_LABEL, "Setting up app with in-memory database for testing");
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

    public DriverSchema getEditObject() {
        if (record == null) {
            record = new Record();
        }

        return record.getEditObject();
    }

    public DriverConstantFields getEditConstants() {
        return record.getEditConstants();
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
}
