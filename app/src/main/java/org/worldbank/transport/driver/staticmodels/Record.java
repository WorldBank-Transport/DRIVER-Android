package org.worldbank.transport.driver.staticmodels;

import android.util.Log;

import org.worldbank.transport.driver.datastore.DriverSchemaSerializer;
import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.models.DriverSchema;

/**
 * Manages the data associated with a record.
 * DriverApp holds a private reference to an instance of this class to manage the record
 * currently being edited.
 *
 * Created by kathrynkillebrew on 1/11/16.
 */
public class Record {

    private static final String LOG_LABEL = "Record";

    private DriverSchema editObject;
    private long editObjectDatabaseId;
    private DriverConstantFields editConstants;
    private String recordSchemaVersion;

    // constructor for editing an existing record
    public Record(DriverSchema editObject, long editObjectDatabaseId, DriverConstantFields editConstants, String recordSchemaVersion) {
        this.editObject = editObject;
        this.editObjectDatabaseId = editObjectDatabaseId;
        this.editConstants = editConstants;
        this.recordSchemaVersion = recordSchemaVersion;
    }

    // constructor to make a new record
    public Record() {
        editObject = new DriverSchema();
        editConstants = new DriverConstantFields();
        editObjectDatabaseId = -1;
        Log.d(LOG_LABEL, "Created new object to edit");
    }

    public long getRecordId() {
        return editObjectDatabaseId;
    }

    public DriverSchema getEditObject() {
        return editObject;
    }

    public DriverConstantFields getEditConstants() {
        return editConstants;
    }

    public boolean save() {
        if (editObject == null) {
            Log.e(LOG_LABEL, "No currently editing DRIVER data to save!");
            return false;
        }

        String serializedEditObject = DriverSchemaSerializer.serializeRecord(editObject);

        if (serializedEditObject == null) {
            Log.e(LOG_LABEL, "Failed to serialize record to JSON");
            return false;
        }

        RecordDatabaseManager databaseManager = DriverApp.getDatabaseManager();

        if (editObjectDatabaseId > -1) {
            // update an existing record
            int affected = databaseManager.updateRecord(serializedEditObject, editConstants, editObjectDatabaseId);
            if (affected == 1) {
                return true;
            } else {
                Log.e(LOG_LABEL, "Failed to update record. Number of affected rows: " + affected);
            }
        } else {
            // add new record
            long newId = databaseManager.addRecord(DriverApp.getCurrentSchema(), serializedEditObject, editConstants);
            if (newId > -1) {
                editObjectDatabaseId = newId;
                return true;
            } else {
                Log.e(LOG_LABEL, "Error inserting record");
            }
        }

        return false;
    }
}
