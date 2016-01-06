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

    public RecordDatabaseManager(Context context) {
        dbHelper = new RecordDatabaseHelper(context);
    }

    /**
     * Add a record to the database.
     *
     * @param schemaVersion UUID of the schema used to create the record
     * @param data Serialized JSON representation of the record
     * @return The row ID of the added record
     */
    public long addRecord(String schemaVersion, String data) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DriverRecordContract.RecordEntry.COLUMN_SCHEMA_VERSION, schemaVersion);
        values.put(DriverRecordContract.RecordEntry.COLUMN_DATA, data);

        long newId = db.insert(DriverRecordContract.RecordEntry.TABLE_NAME, null, values);
        db.close();
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

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String[] whereArgs = { String.valueOf(recordId) };

        ContentValues values = new ContentValues();
        values.put(DriverRecordContract.RecordEntry.COLUMN_DATA, data);

        int affected = db.update(DriverRecordContract.RecordEntry.TABLE_NAME, values, WHERE_ID, whereArgs);
        db.close();
        return affected;
    }

    /**
     * Get a cursor to fetch all records.
     *
     * @return Database cursor to retrieve all records
     */
    public Cursor readAllRecords() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sortOrder = DriverRecordContract.RecordEntry.COLUMN_ENTERED_AT + " DESC";

        return db.query(
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
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] dataField = { DriverRecordContract.RecordEntry.COLUMN_DATA };
        String[] whereArgs = { String.valueOf(recordId) };

        Cursor cursor = db.query(
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
        db.close();
        return recordData;
    }
}
