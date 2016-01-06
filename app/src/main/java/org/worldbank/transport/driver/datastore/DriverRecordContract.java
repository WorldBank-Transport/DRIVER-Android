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

        public static final String COLUMN_ENTERED_AT = "entered_at";
        public static final String COLUMN_SCHEMA_VERSION = "schema_version";
        public static final String COLUMN_DATA = "data";
    }

    public static final String RECORD_TABLE_CREATE = "CREATE TABLE " + RecordEntry.TABLE_NAME + " (" +
            RecordEntry.COLUMN_ENTERED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            RecordEntry.COLUMN_SCHEMA_VERSION + " TEXT, " +
            RecordEntry.COLUMN_DATA + " TEXT, " +
            ")";

    public static final String RECORD_TABLE_DROP = "DROP TABLE IF EXISTS " + RecordEntry.TABLE_NAME;
}
