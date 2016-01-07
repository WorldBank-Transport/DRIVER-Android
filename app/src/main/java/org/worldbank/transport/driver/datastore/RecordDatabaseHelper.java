package org.worldbank.transport.driver.datastore;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.security.InvalidParameterException;


/**
 * Created by kathrynkillebrew on 1/5/16.
 */
public class RecordDatabaseHelper extends SQLiteOpenHelper {

    private static final String LOG_LABEL = "DatabaseHelper";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "driverdb";

    /**
     * Constructor to use in-memory database. For use in testing only!
     *
     * @param context Context for database
     * @param amTesting If true, will use in-memory database
     */
    public RecordDatabaseHelper(Context context, boolean amTesting) {
        super(context, null, null, DATABASE_VERSION);
        if (amTesting) {
            Log.w(LOG_LABEL, "Creating in-memory database. Should be used in testing only!");
        } else {
            Log.w(LOG_LABEL, "Using database on file system. Should use the other constructor!");
            throw(new InvalidParameterException(
                    "Called testing-only constructor with amTesting=false. Use other constructor if not testing."));
        }
    }

    public RecordDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
