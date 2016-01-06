package org.worldbank.transport.driver.datastore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Use this class to access the records database.
 *
 * Created by kathrynkillebrew on 1/5/16.
 */
public class RecordDatabaseManager {

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

        return db.insert(DriverRecordContract.RecordEntry.TABLE_NAME, null, values);
    }

    /**
     * Get a cursor to fetch all records.
     *
     * @return Database cursor to retrieve all records
     */
    public Cursor readAllRecords() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // which columns to return
        String[] projection = {
                DriverRecordContract.RecordEntry._ID,
                DriverRecordContract.RecordEntry.COLUMN_ENTERED_AT,
                DriverRecordContract.RecordEntry.COLUMN_SCHEMA_VERSION,
                DriverRecordContract.RecordEntry.COLUMN_DATA
        };

        String sortOrder = DriverRecordContract.RecordEntry.COLUMN_ENTERED_AT + " DESC";

        return db.query(
                DriverRecordContract.RecordEntry.TABLE_NAME,
                projection, // columns
                null,       // WHERE
                null,       // WHERE args
                null,       // GROUP BY
                null,       // HAVING
                sortOrder   // ORDER BY
        );
    }
}
