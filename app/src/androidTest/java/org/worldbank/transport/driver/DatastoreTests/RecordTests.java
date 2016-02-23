package org.worldbank.transport.driver.DatastoreTests;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverConstantFields;
import org.worldbank.transport.driver.staticmodels.Record;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * TODO: find way to dynamically load schema to re-enable these unit tests
 *
 * Test Record class that manages record currently being edited.
 *
 * Created by kathrynkillebrew on 1/11/16.
 */
/*
public class RecordTests extends AndroidTestCase {

    DriverApp app;
    RecordDatabaseManager databaseManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create app in test context
        app = new DriverApp(true);
        // TODO: figure out how to set context to get asset directory to be able to re-enable these tests
        app.loadBackupSchema();
        databaseManager = DriverApp.getDatabaseManager();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @SmallTest
    public void testMakingNewRecord() {
        Record record = new Record();
        assertNotNull(record.getEditObject());
        assertNotNull(record.getEditConstants());
    }

    @SmallTest
    public void testSavingAndReloadingRecord() {
        Record record = new Record();
        DriverConstantFields constants = record.getEditConstants();
        constants.occurredFrom = new Date();
        constants.Weather = DriverConstantFields.WeatherEnum.CLEAR_DAY;
        Class schemaClass = DriverApp.getSchemaClass();
        Object driverSchema = record.getEditObject();

        assertEquals(schemaClass, driverSchema.getClass());

        if (schemaClass == null) {
            fail("Schema class not found");
        }

        try {
            Field detailsField = schemaClass.getField("accidentDetails");
            Object details = detailsField.getType().newInstance();
            Field descriptionField = details.getClass().getField("Description");
            descriptionField.set(details, "something");
            detailsField.set(driverSchema, details);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("Could not find field");
        } catch (InstantiationException e) {
            e.printStackTrace();
            fail("Could not instantiate");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            fail("Could not access");
        }

        record.save();

        // check that database ID got for new record
        long id = record.getRecordId();
        assertNotSame(-1, id);

        // check that loading it back from the DB has the same values set
        Record found = databaseManager.getRecordById(id);

        assertEquals(id, found.getRecordId());
        assertEquals(DriverConstantFields.WeatherEnum.CLEAR_DAY, found.getEditConstants().Weather);

        Object foundEdit = found.getEditObject();
        try {
            Field foundDetails = foundEdit.getClass().getField("accidentDetails");
            Object foundDescription = foundDetails.getClass().getField("Description");
            assertEquals("something", foundDescription);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("Could not find field");
        }
    }
}
*/
