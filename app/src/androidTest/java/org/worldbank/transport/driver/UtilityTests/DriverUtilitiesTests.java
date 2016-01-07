package org.worldbank.transport.driver.UtilityTests;

import android.test.AndroidTestCase;
import android.util.Log;

import org.worldbank.transport.driver.TestModels.TestPerson;
import org.worldbank.transport.driver.utilities.DriverUtilities;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Tests for DriverUtilities methods.
 *
 * Created by kathrynkillebrew on 1/5/16.
 */
public class DriverUtilitiesTests extends AndroidTestCase {

    private static final String LOG_LABEL = "DriverUtilitiesTests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFieldOrder() {
        String[] fieldOrder = DriverUtilities.getFieldOrder(TestPerson.class);

        String[] expectedFieldOrder = {
                "Name",
                "Address",
                "LicenseNumber",
                "Sex",
                "Age",
                "Injury",
                "DriverError",
                "AlcoholDrugs",
                "SeatBeltHelmet",
                "Hospital",
                "Involvement",
                "Vehicle"};

        assertTrue("Field order does not match JsonPropertyOrder",
                Arrays.equals(expectedFieldOrder, fieldOrder));
    }

    public void testListItemLabel() {
        TestPerson bigBird = new TestPerson();
        bigBird.Name = "Big Bird";
        bigBird.Address = "Sesame Street";
        bigBird.LicenseNumber = null;

        ArrayList<TestPerson> people = new ArrayList<>();
        people.add(bigBird);

        ArrayList<String> itemLabels = DriverUtilities.getListItemLabels(people, TestPerson.class, "Test Person");

        assertEquals("Unexpected number of item labels returned", 1, itemLabels.size());
        String bigBirdLabel = itemLabels.get(0);
        assertEquals("Unexpected item label", "Big Bird - Sesame Street", bigBirdLabel);
    }

    public void testListItemDefaultLabel() {
        TestPerson bigBird = new TestPerson();
        bigBird.Name = null;
        bigBird.Address = null;
        bigBird.LicenseNumber = null;

        ArrayList<TestPerson> people = new ArrayList<>();
        people.add(bigBird);

        ArrayList<String> itemLabels = DriverUtilities.getListItemLabels(people, TestPerson.class, "Test Person");

        assertEquals("Unexpected number of item labels returned", 1, itemLabels.size());
        String bigBirdLabel = itemLabels.get(0);
        assertEquals("Unexpected default " +
                "item label", "Test Person - 1", bigBirdLabel);
    }

}
