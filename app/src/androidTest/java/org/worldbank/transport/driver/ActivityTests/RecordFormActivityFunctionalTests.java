package org.worldbank.transport.driver.ActivityTests;

import android.app.Instrumentation;
import android.support.v7.widget.AppCompatTextView;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.UiThreadTest;
import android.test.ViewAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.FormElementController;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.activities.RecordFormActivity;

import java.util.ArrayList;
import java.util.List;


/**
 * Functional tests for dynamically created form.
 *
 * Created by kathrynkillebrew on 12/21/15.
 */
public class RecordFormActivityFunctionalTests extends ActivityInstrumentationTestCase2<RecordFormActivity> {

    private RecordFormActivity activity;

    public RecordFormActivityFunctionalTests() {
        super(RecordFormActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        activity = getActivity();
    }
    @SmallTest
    public void testActivityExists() {
        assertNotNull("Record form activity is null", activity);
    }

    @SmallTest
    public void testLayout() {
        ViewGroup containerView = (ViewGroup) activity.findViewById(R.id.form_elements_container);
        Button goButton = (Button) activity.findViewById(R.id.record_save_button_id);

        ViewAsserts.assertGroupContains(containerView, goButton);
        View root = containerView.getRootView();
        ViewAsserts.assertOnScreen(root, containerView);
    }

    @MediumTest
    @UiThreadTest
    public void testValidationErrorDisplay() {
        ViewGroup containerView = (ViewGroup) activity.findViewById(R.id.form_elements_container);
        Button goButton = (Button) activity.findViewById(R.id.record_save_button_id);

        FormController formController = activity.getFormController();
        FormElementController licenseNoCtl =  formController.getElement("LicenseNumber");
        assertNotNull(licenseNoCtl);

        LinearLayout ctlView = (LinearLayout) licenseNoCtl.getView();
        List<View> licenseNoViews = getAllChildViews(ctlView);

        EditText licenseNoField = null;

        // the first text view is the field label, and the second is for field error messages
        boolean foundLabel = false;
        AppCompatTextView errorMsgView = null;

        for (View view : licenseNoViews) {
            if (view instanceof AppCompatTextView) {
                if (foundLabel) {
                    errorMsgView = (AppCompatTextView) view;
                } else {
                    foundLabel = true;
                }
            } else if (view instanceof EditText) {
                // the actual text entry field
                licenseNoField = (EditText) view;
            }
        }

        assertNotNull("Did not find error message view for license number field", errorMsgView);
        assertNotNull("Did not find text entry field for license number fiewd", licenseNoField);

        // test validation on license view
        licenseNoField.setText("123");
        goButton.performClick();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("Did not get expected license # field error", "size must be between 6 and 8", errorMsgView.getText());
        assertEquals("License # error view is not visible", View.VISIBLE, errorMsgView.getVisibility());

        // now test that error clears from display once it has been fixed
        licenseNoField.setText("123456");
        goButton.performClick();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertNotSame("License # error not cleared", View.VISIBLE, errorMsgView.getVisibility());
    }

    // helper to recursively find all child views in a view hierarchy
    // http://stackoverflow.com/questions/18668897/android-get-all-children-elements-of-a-viewgroup
    private List<View> getAllChildViews(View v) {
        List<View> visited = new ArrayList<>();
        List<View> unvisited = new ArrayList<>();
        unvisited.add(v);

        while (!unvisited.isEmpty()) {
            View child = unvisited.remove(0);
            visited.add(child);
            if (!(child instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) child;
            final int childCount = group.getChildCount();
            for (int i=0; i<childCount; i++) unvisited.add(group.getChildAt(i));
        }

        return visited;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
