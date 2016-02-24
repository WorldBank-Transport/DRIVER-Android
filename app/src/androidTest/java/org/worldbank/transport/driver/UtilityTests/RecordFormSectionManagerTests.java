package org.worldbank.transport.driver.UtilityTests;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.worldbank.transport.driver.activities.RecordFormSectionActivity;
import org.worldbank.transport.driver.activities.RecordItemListActivity;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.lang.reflect.Field;
import java.util.ArrayList;

import it.necst.grabnrun.SecureDexClassLoader;

/**
 * Unit tests form RecordFormSectionManager static methods
 *
 * Created by kathrynkillebrew on 1/7/16.
 */
public class RecordFormSectionManagerTests extends AndroidTestCase {

    private static final String LOG_LABEL = "SectionMgrTests";

    Object driverSchema;
    Class personClass;
    Class detailsClass;
    Class vehicleClass;

    Field personField;
    Field nameField;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Class driverClass = DriverApp.getSchemaClass();
        SecureDexClassLoader modelClassLoader = DriverApp.getSchemaClassLoader();

        if (driverClass == null) {
            fail("top level model class not found");
        }
        driverSchema = driverClass.newInstance();

        personClass = modelClassLoader.loadClass(RecordFormSectionManager.MODEL_PACKAGE + "Person");
        detailsClass = modelClassLoader.loadClass(RecordFormSectionManager.MODEL_PACKAGE + "AccidentDetails");
        vehicleClass = modelClassLoader.loadClass(RecordFormSectionManager.MODEL_PACKAGE + "Vehicle");

        personField = driverSchema.getClass().getField("person");
        nameField = personClass.getField("Name");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @SmallTest
    public void testGetActivityClassForSection() {
        // first section is Details
        Class activityClass = RecordFormSectionManager.getActivityClassForSection(0);
        assertEquals("Expected single section form", RecordFormSectionActivity.class, activityClass);

        // second class is Vehicle
        activityClass = RecordFormSectionManager.getActivityClassForSection(1);
        assertEquals("Expected list activity for section with multiple items", RecordItemListActivity.class, activityClass);
    }

    @SmallTest
    public void testGetSectionName() {
        String sectionName = RecordFormSectionManager.getSectionName(0);
        assertEquals("accidentDetails", sectionName);

        sectionName = RecordFormSectionManager.getSectionName(2);
        assertEquals("person", sectionName);

        // no fourth section
        sectionName = RecordFormSectionManager.getSectionName(42);
        assertNull("Should not have a name for a non-extant section", sectionName);
    }

    @SmallTest
    public void testGetSectionClass() {
        Class foundClass = RecordFormSectionManager.getSectionClass("person");
        if (foundClass == null) {
            fail("class not found");
        }
        assertEquals("Did not find expected class for Person", "Person", foundClass.getSimpleName());
    }

    @SmallTest
    public void testSectionHasNext() {
        boolean hasNext = RecordFormSectionManager.sectionHasNext(0);
        assertTrue(hasNext);

        hasNext = RecordFormSectionManager.sectionHasNext(3);
        assertFalse(hasNext);
    }

    @SmallTest
    public void testSectionHasMultiple() {
        boolean multiple = RecordFormSectionManager.sectionHasMultiple(0);
        assertFalse(multiple);

        multiple = RecordFormSectionManager.sectionHasMultiple(1);
        assertTrue(multiple);
    }

    @SmallTest
    public void testFieldForSectionName() {
        Field foundField = RecordFormSectionManager.getFieldForSectionName("person");
        assertNotNull(foundField);
        assertEquals("Did not find expected person field", "person", foundField.getName());
    }

    @SmallTest
    public void testGetPluralTitle() {
        Field foundField = RecordFormSectionManager.getFieldForSectionName("person");
        String foundPlural = RecordFormSectionManager.getPluralTitle(foundField, "some default");
        assertEquals("Unexpected plural title", "People", foundPlural);

        try {
            Field invalidField = personClass.getField("Injury");
            foundPlural  = RecordFormSectionManager.getPluralTitle(invalidField, "some default");
            assertEquals("Did not get default plural title", foundPlural, "some default");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("Could not test default plural title");
        }
    }

    @SmallTest
    public void testGetSingleTitle() {
        Field foundField = RecordFormSectionManager.getFieldForSectionName("accidentDetails");
        String foundTitle = RecordFormSectionManager.getSingleTitle(foundField, "some default");
        assertEquals("Unexpected title", "Accident Details", foundTitle);

        try {
            // this field is not a section, and so does not have a title
            Field invalidField = personClass.getField("Injury");
            foundTitle  = RecordFormSectionManager.getSingleTitle(invalidField, "some default");
            assertEquals("Did not get default plural title", foundTitle, "some default");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("Could not test default plural title");
        }
    }

    @SmallTest
    public void testGetOrCreateSectionObject() {
        // check new section creation
        Field detailsField = RecordFormSectionManager.getFieldForSectionName("accidentDetails");
        Object obj = RecordFormSectionManager.getOrCreateSectionObject(detailsField, detailsClass, driverSchema);
        assertNotNull(obj);

        // check fetch of existing section
        try {
            Field detailField = driverSchema.getClass().getField("accidentDetails");
            assertEquals(detailField, detailsField);
            Object details = detailField.get(driverSchema);
            details.getClass().getField("Description").set(details, "blah blah blah");

            obj = RecordFormSectionManager.getOrCreateSectionObject(detailsField, detailsClass, driverSchema);
            assertNotNull(obj);
            assertEquals(obj.getClass(), detailsClass);

            assertEquals(obj.getClass(), detailsClass);
            Object foundDescription = obj.getClass().getField("Description").get(obj);
            assertEquals(foundDescription, "blah blah blah");

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            fail("could not access field");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("could not find field");
        }
    }

    @SuppressWarnings("unchecked")
    @SmallTest
    public void testGetCreateDeleteListItems() {
        // create new list and add an object to it
        Field foundField = RecordFormSectionManager.getFieldForSectionName("person");
        Object obj = RecordFormSectionManager.getOrCreateListItem(foundField, personClass, driverSchema, 0);
        assertNotNull(obj);
        assertEquals(obj.getClass(), personClass);
        try {
            Object people = personField.get(driverSchema);
            assertEquals(ArrayList.class, people.getClass());
            ArrayList peopleList = (ArrayList) people;
            assertEquals(1, peopleList.size());

            // test getting an object that already exists
            Object newPerson = personClass.newInstance();

            nameField.set(newPerson, "Bob");
            peopleList.add(newPerson);

            obj = RecordFormSectionManager.getOrCreateListItem(foundField, personClass, driverSchema, 1);
            assertNotNull(obj);
            assertEquals(obj.getClass(), personClass);
            assertEquals("Unexpected Person returned from list", obj, newPerson);
            assertEquals("Unexpected name for Person returned from list", nameField.get(obj), "Bob");

            // delete first person
            boolean didItWork = RecordFormSectionManager.deleteListItem(foundField, personClass, driverSchema, 0);
            assertTrue(didItWork);
            assertEquals(((ArrayList) personField.get(driverSchema)).size(), 1);
            obj = RecordFormSectionManager.getOrCreateListItem(foundField, personClass, driverSchema, 0);

            // first person should now be Bob
            assertNotNull(obj);
            assertEquals(obj.getClass(), personClass);
            assertEquals("Unexpected name for Person returned from list after deletion", nameField.get(obj), "Bob");

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            fail("cannot access");
        } catch (InstantiationException e) {
            e.printStackTrace();
            fail("could not instantiate");
        }


    }

    @SuppressWarnings("unchecked")
    @SmallTest
    public void testGetSectionList() {
        try {
            Object personOne = personClass.newInstance();
            Object personTwo = personClass.newInstance();
            nameField.set(personOne, "ThingOne");
            nameField.set(personTwo, "ThingTwo");

            ArrayList personList = (ArrayList)personField.get(driverSchema);
            personList.add(personOne);
            personList.add(personTwo);

            ArrayList gotPeople = RecordFormSectionManager.getSectionList(personField.get(driverSchema));
            assertEquals(gotPeople, personField.get(driverSchema));

            Field vehicleField = driverSchema.getClass().getField("vehicle");
            ArrayList gotVehicles = RecordFormSectionManager.getSectionList(vehicleField.get(driverSchema));
            assertEquals(gotVehicles.size(), 0);

        } catch (InstantiationException e) {
            e.printStackTrace();
            fail("could not instantiate");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            fail("could not access");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("field not found");
        }
    }
}
