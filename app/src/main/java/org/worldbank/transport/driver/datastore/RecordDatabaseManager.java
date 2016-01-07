package org.worldbank.transport.driver.datastore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


/**
 * Use this class to access the records database.
 *
 * Created by kathrynkillebrew on 1/5/16.
 */
public class RecordDatabaseManager {

    private static final String LOG_LABEL = "DatabaseManager";

    private static final String DATABASE_NAME = "driverdb";

    // use as WHERE clause to match on ID
    private static final String WHERE_ID = "_id= ?";

    // use to return all columns from record table
    private static final String[] ALL_FIELDS = {
            DriverRecordContract.RecordEntry._ID,
            DriverRecordContract.RecordEntry.COLUMN_ENTERED_AT,
            DriverRecordContract.RecordEntry.COLUMN_SCHEMA_VERSION,
            DriverRecordContract.RecordEntry.COLUMN_DATA
    };

    RecordDatabaseHelper dbHelper;

    private final SQLiteDatabase writableDb;
    private final SQLiteDatabase readableDb;

    /**
     * Set up database for use. Will use in-memory database if amTesting flag is true.
     *
     * @param context Context for database
     * @param amTesting Use in-memory DB if true, otherwise use file-based DB.
     */
    public RecordDatabaseManager(Context context, boolean amTesting) {
        if (amTesting) {
            Log.w(LOG_LABEL, "DB Manager will use in-memory DB. This should only happen in testing!");
            dbHelper = new RecordDatabaseHelper(context, null);
        } else {
            dbHelper = new RecordDatabaseHelper(context, DATABASE_NAME);
        }

        writableDb = dbHelper.getWritableDatabase();
        readableDb = dbHelper.getReadableDatabase();
    }

    /**
     * Add a record to the database.
     *
     * @param schemaVersion UUID of the schema used to create the record
     * @param data Serialized JSON representation of the record
     * @return The row ID of the added record
     */
    public long addRecord(String schemaVersion, String data) {

        ContentValues values = new ContentValues();
        values.put(DriverRecordContract.RecordEntry.COLUMN_SCHEMA_VERSION, schemaVersion);
        values.put(DriverRecordContract.RecordEntry.COLUMN_DATA, data);

        writableDb.beginTransaction();
        long newId = -1;
        try {
            newId = writableDb.insert(DriverRecordContract.RecordEntry.TABLE_NAME, null, values);
            writableDb.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(LOG_LABEL, "Database record insert failed");
            e.printStackTrace();
            newId = -1;
        } finally {
            writableDb.endTransaction();
        }

        return newId;
    }

    /**
     * Update an existing record in the database. Should be called on record 'save'.
     *
     * @param data Serialized JSON string of the DriverSchema object to save
     * @param recordId Database ID of the record to update
     * @return Number of rows affected (should be 1 on success)
     */
    public int updateRecord(String data, long recordId) {

        String[] whereArgs = { String.valueOf(recordId) };

        ContentValues values = new ContentValues();
        values.put(DriverRecordContract.RecordEntry.COLUMN_DATA, data);

        writableDb.beginTransaction();
        int affected = -1;
        try {
            affected = writableDb.update(DriverRecordContract.RecordEntry.TABLE_NAME, values, WHERE_ID, whereArgs);
            writableDb.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(LOG_LABEL, "Database record update failed for ID " + recordId);
            e.printStackTrace();
            affected = -1;
        } finally {
            writableDb.endTransaction();
        }
        return affected;
    }

    /**
     * Get a cursor to fetch all records.
     *
     * @return Database cursor to retrieve all records
     */
    public Cursor readAllRecords() {

        String sortOrder = DriverRecordContract.RecordEntry.COLUMN_ENTERED_AT + " DESC";

        return readableDb.query(
                DriverRecordContract.RecordEntry.TABLE_NAME,
                ALL_FIELDS, // columns
                null,       // WHERE
                null,       // WHERE args
                null,       // GROUP BY
                null,       // HAVING
                sortOrder   // ORDER BY
        );
    }

    /**
     * Fetch the JSON representation of a record from the database by its _id
     *
     * @param recordId Database ID for record to get
     * @return Serialized string of the record data, or null on failure
     */
    public String getSerializedRecordWithId(long recordId) {

        String[] dataField = { DriverRecordContract.RecordEntry.COLUMN_DATA };
        String[] whereArgs = { String.valueOf(recordId) };

        Cursor cursor = readableDb.query(
                DriverRecordContract.RecordEntry.TABLE_NAME,
                dataField, // columns
                WHERE_ID,   // WHERE
                whereArgs,  // WHERE args
                null,       // GROUP BY
                null,       // HAVING
                null        // ORDER BY
        );

        if (!cursor.moveToFirst()) {
            Log.e(LOG_LABEL, "Record with ID " + recordId + " not found!");
            return null;
        }

        String recordData = cursor.getString(0);
        cursor.close();
        return recordData;
    }
}
