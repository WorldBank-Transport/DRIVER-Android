package org.worldbank.transport.driver.datastore;

import android.provider.BaseColumns;


/**
 * Schema for database table used to store records.
 * The record itself is serialized to JSON before being stored as a string;
 * the other columns are metadata.
 *
 * Created by kathrynkillebrew on 1/5/16.
 */
public final class DriverRecordContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DriverRecordContract() {}

    // BaseColumns provides a primary key called _ID expected by cursor adapters
    public static abstract class RecordEntry implements BaseColumns {
        public static final String TABLE_NAME = "driver_records";


        // fields
        public static final String COLUMN_ENTERED_AT = "entered_at";
        public static final String COLUMN_UPDATED_AT = "last_updated";
        public static final String COLUMN_SCHEMA_VERSION = "schema_version";
        public static final String COLUMN_DATA = "data";

        // fields for record constants
        public static final String COLUMN_OCCURRED_FROM = "occurred_from";
        public static final String COLUMN_OCCURRED_TO = "occurred_to";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_WEATHER = "weather";
        public static final String COLUMN_LIGHT = "light";
    }

    public static final String RECORD_TABLE_CREATE = "CREATE TABLE " + RecordEntry.TABLE_NAME + " (" +
            RecordEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            RecordEntry.COLUMN_ENTERED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
            RecordEntry.COLUMN_UPDATED_AT + " TIMESTAMP, " +
            RecordEntry.COLUMN_SCHEMA_VERSION + " TEXT NOT NULL, " +
            RecordEntry.COLUMN_DATA + " TEXT NOT NULL, " +
            RecordEntry.COLUMN_OCCURRED_FROM + " TIMESTAMP NOT NULL, " +
            RecordEntry.COLUMN_OCCURRED_TO + " TIMESTAMP NOT NULL, " +

            // TODO: require these to be non-null when UI figured out?
            RecordEntry.COLUMN_LATITUDE + " DOUBLE, " +
            RecordEntry.COLUMN_LONGITUDE + " DOUBLE, " +

            RecordEntry.COLUMN_WEATHER + " TEXT, " +
            RecordEntry.COLUMN_LIGHT + " TEXT" +
            ");";

    public static final String RECORD_TABLE_DROP = "DROP TABLE IF EXISTS " + RecordEntry.TABLE_NAME;
}
