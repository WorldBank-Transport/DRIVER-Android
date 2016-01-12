package org.worldbank.transport.driver.DatastoreTests;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import org.worldbank.transport.driver.datastore.RecordDatabaseManager;
import org.worldbank.transport.driver.models.AccidentDetails;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverConstantFields;
import org.worldbank.transport.driver.staticmodels.Record;

import java.util.Date;

/**
 * Test Record class that manages record currently being edited.
 *
 * Created by kathrynkillebrew on 1/11/16.
 */
public class RecordTests extends AndroidTestCase {

    DriverApp app;
    RecordDatabaseManager databaseManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create app in test context
        app = new DriverApp(true);
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
        DriverSchema driverSchema = record.getEditObject();
        driverSchema.AccidentDetails = new AccidentDetails();
        driverSchema.AccidentDetails.Description = "something";

        record.save();

        // check that database ID got for new record
        long id = record.getRecordId();
        assertNotSame(-1, id);

        // check that loading it back from the DB has the same values set
        Record found = databaseManager.getRecordById(id);

        assertEquals(id, found.getRecordId());
        assertEquals(DriverConstantFields.WeatherEnum.CLEAR_DAY, found.getEditConstants().Weather);
        assertEquals("something", found.getEditObject().AccidentDetails.Description);
    }

}
