package org.worldbank.transport.driver.ActivityTests;

import android.app.Instrumentation;
import android.content.Intent;
import android.support.v7.widget.AppCompatTextView;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.FormElementController;
import com.azavea.androidvalidatedforms.controllers.SelectionController;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.activities.RecordFormActivity;
import org.worldbank.transport.driver.activities.RecordFormItemActivity;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import it.necst.grabnrun.SecureDexClassLoader;


/**
 * Functional tests for dynamically created form.
 *
 * Created by kathrynkillebrew on 12/21/15.
 */
public class RecordFormActivityFunctionalTests extends ActivityInstrumentationTestCase2<RecordFormItemActivity>
    implements RecordFormActivity.FormReadyListener {

    private RecordFormItemActivity activity;
    private CountDownLatch displayLock;

    public RecordFormActivityFunctionalTests() {
        super(RecordFormItemActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Intent intent = new Intent(getInstrumentation().getTargetContext(), RecordFormItemActivity.class);

        // go to Persons section
        intent.putExtra(RecordFormActivity.SECTION_ID, 2);
        intent.putExtra(RecordFormItemActivity.ITEM_INDEX, 0);

        setActivityIntent(intent);

        activity = getActivity();

        // wait for the form to display
        if (!activity.isFormReady()) {
            activity.setFormReadyListener(this);
            displayLock = new CountDownLatch(1);
            displayLock.await(3000, TimeUnit.MILLISECONDS);
        }

        // make sure form is done rendering
        FormActivityTestHelpers.waitForLoaderToDisappear(getInstrumentation(), activity);

        setDummyData();
    }

    @Override
    public void formReadyCallback() {
        if (displayLock != null) {
            displayLock.countDown();
        }
    }

    @SmallTest
    public void testActivityExists() {
        assertNotNull("Record form activity is null", activity);
    }

    @SmallTest
    public void testLayout() {
        ViewGroup containerView = (ViewGroup) activity.findViewById(R.id.form_elements_container);
        RelativeLayout buttonBar = (RelativeLayout) activity.findViewById(R.id.record_button_bar_id);
        Button goButton = (Button) activity.findViewById(R.id.record_save_button_id);

        assertNotNull(buttonBar);
        assertNotNull(goButton);

        ViewAsserts.assertGroupContains(containerView, buttonBar);
        ViewAsserts.assertGroupContains(buttonBar, goButton);
        View root = containerView.getRootView();
        ViewAsserts.assertOnScreen(root, containerView);
    }

    @MediumTest
    public void testReferenceTypeField() {
        FormController formController = activity.getFormController();

        SelectionController vehicleCtl = (SelectionController)formController.getElement("vehicle");
        assertNotNull(vehicleCtl);

        Object vehicleObj = vehicleCtl.getModel().getValue(vehicleCtl.getName());

        // should have a UUID set
        assertEquals(String.class, vehicleObj.getClass());
        assertEquals(36, ((String) vehicleObj).length());
    }

    /* TODO: find a way to test validation that does not depend on base models
       base models have no validation rules for visible fields that require hibernate checks

    @MediumTest
    public void testValidationErrorDisplay() {
        final Instrumentation instrumentation = getInstrumentation();
        final Button goButton = (Button) activity.findViewById(R.id.record_save_button_id);

        FormController formController = activity.getFormController();
        FormElementController licenseNoCtl =  formController.getElement("LicenseNumber");

        assertNotNull(licenseNoCtl);

        LinearLayout ctlView = (LinearLayout) licenseNoCtl.getView();
        List<View> licenseNoViews = FormActivityTestHelpers.getAllChildViews(ctlView);

        // the first text view is the field label, and the second is for field error messages
        boolean foundLabel = false;
        AppCompatTextView errorMsgView = null;

        EditText foundLicenseNoField = null;
        for (View view : licenseNoViews) {
            if (view instanceof AppCompatTextView) {
                if (foundLabel) {
                    errorMsgView = (AppCompatTextView) view;
                } else {
                    foundLabel = true;
                }
            } else if (view instanceof EditText) {
                // the actual text entry field
                foundLicenseNoField = (EditText) view;
            }
        }

        final EditText licenseNoField = foundLicenseNoField;

        assertNotNull("Did not find error message view for license number field", errorMsgView);
        assertNotNull("Did not find text entry field for license number field", licenseNoField);


        // test validation on license view
        instrumentation.waitForIdleSync();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                licenseNoField.setText("123");
                goButton.performClick();
            }
        });

        // wait for validation to finish
        FormActivityTestHelpers.waitForLoaderToDisappear(instrumentation, activity);

        assertEquals("Did not get expected license # field error", "size must be between 6 and 8", errorMsgView.getText());
        assertEquals("License # error view is not visible", View.VISIBLE, errorMsgView.getVisibility());

        // now test that error clears from display once it has been fixed
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                licenseNoField.setText("123456");
            }
        });

        instrumentation.waitForIdleSync();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                goButton.performClick();
            }
        });

        // wait for validation to finish
        FormActivityTestHelpers.waitForLoaderToDisappear(instrumentation, activity);

        assertNotSame("License # error not cleared", View.VISIBLE, errorMsgView.getVisibility());
    }
    */

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (activity != null) {
            activity.finish();
        }
    }

    private void setDummyData() {
        DriverApp app = (DriverApp) activity.getApplication();
        Object editObj = app.getEditObject();
        Class driverClass = DriverApp.getSchemaClass();

        if (driverClass == null) {
            fail("Driver class not found");
        }

        try {
            Field vehicleField = driverClass.getField("vehicle");
            Field personField = driverClass.getField("person");

            SecureDexClassLoader classLoader = DriverApp.getSchemaClassLoader();
            assertNotNull(classLoader);
            Class vehicleClass = classLoader.loadClass(RecordFormSectionManager.MODEL_PACKAGE + "Vehicle");
            Object testVehicleOne = vehicleClass.newInstance();
            Object testVehicleTwo = vehicleClass.newInstance();

            Class[] vehicleInnerClasses = vehicleClass.getDeclaredClasses();

            Object[] vehicleTypes = null;
            for (Class clazz: vehicleInnerClasses) {
                if (clazz.getSimpleName().equals("VehicleTypeEnum")) {
                    vehicleTypes = clazz.getEnumConstants();
                }
            }

            if (vehicleTypes == null) {
                fail("Vehicle types not found");
            }

            Field vehicleTypeField = vehicleClass.getField("VehicleType");

            vehicleTypeField.set(testVehicleOne, vehicleTypes[0]);
            vehicleTypeField.set(testVehicleTwo, vehicleTypes[1]);

            ArrayList<Object> vehicles = new ArrayList<>(2);
            vehicles.add(testVehicleOne);
            vehicles.add(testVehicleTwo);
            vehicleField.set(editObj, vehicles);

            Object testPerson = classLoader.loadClass(RecordFormSectionManager.MODEL_PACKAGE + "Person").newInstance();
            testPerson.getClass().getField("FirstName").set(testPerson, "Evel");
            testPerson.getClass().getField("LastName").set(testPerson, "Knievel");

            Field vehicleIdField = vehicleClass.getField("LocalId");
            testPerson.getClass().getField("vehicle").set(testPerson, vehicleIdField.get(testVehicleOne));

            ArrayList<Object> people = new ArrayList<>(1);
            people.add(testPerson);
            personField.set(editObj, people);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail("field not found");
        } catch (InstantiationException e) {
            e.printStackTrace();
            fail("could not instantiate");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            fail("could not access");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail("could not find class");
        }

        // reload with the new data
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.showProgress(true);
                activity.recreate();
            }
        });

        // wait for the form to display
        if (!activity.isFormReady()) {
            activity.setFormReadyListener(this);
            displayLock = new CountDownLatch(1);
            try {
                displayLock.await(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // make sure form is done rendering
        FormActivityTestHelpers.waitForLoaderToDisappear(getInstrumentation(), activity);
    }
}
