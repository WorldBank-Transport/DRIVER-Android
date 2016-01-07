package org.worldbank.transport.driver.datastore;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Handles creation and version management of database.
 *
 * Created by kathrynkillebrew on 1/5/16.
 */
public class RecordDatabaseHelper extends SQLiteOpenHelper {

    private static final String LOG_LABEL = "DatabaseHelper";

    private static final int DATABASE_VERSION = 1;

    /**
     * Set up database. If databaseName is null, will use in-memory DB. Only do so when testing!
     *
     * @param context Context for database
     * @param databaseName Name to use for database, or null for in-memory DB.
     */
    public RecordDatabaseHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);

        if (databaseName == null) {
            Log.w(LOG_LABEL, "Using in-memory database for testing");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DriverRecordContract.RECORD_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle migrations here.
        // This drops any existing entries and recreates the table.
        db.execSQL(DriverRecordContract.RECORD_TABLE_DROP);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // handle migrations here
        onUpgrade(db, oldVersion, newVersion);
    }
}
