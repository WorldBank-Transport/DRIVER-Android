package org.worldbank.transport.driver.UtilityTests;

import android.test.AndroidTestCase;
import android.util.Log;

import org.worldbank.transport.driver.TestModels.TestPerson;
import org.worldbank.transport.driver.utilities.DriverUtilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

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

        ArrayList<String> itemLabels = DriverUtilities.getListItemLabels(people, TestPerson.class, "Test Person").labels;

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

        ArrayList<String> itemLabels = DriverUtilities.getListItemLabels(people, TestPerson.class, "Test Person").labels;

        assertEquals("Unexpected number of item labels returned", 1, itemLabels.size());
        String bigBirdLabel = itemLabels.get(0);
        assertEquals("Unexpected default " +
                "item label", "Test Person - 1", bigBirdLabel);
    }

    public void testUmmalQuara() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        // Friday, December 31, 1999 11:59:59 PM UTC
        Date date = new Date(946684799000L);
        calendar.setTime(date);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d MMMM, y", Locale.ENGLISH);
        String formattedDate = DriverUtilities.formatDateAsUmmalqura(date, sdf, Locale.ENGLISH);
        assertEquals("Incorrect date format for " + date.toString(), "Friday 23 Ramadhan, 1420", formattedDate);
    }

}
