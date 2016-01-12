package org.worldbank.transport.driver.ActivityTests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.support.v7.widget.AppCompatTextView;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
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
import com.robotium.solo.Solo;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.activities.RecordFormActivity;
import org.worldbank.transport.driver.activities.RecordFormConstantsActivity;
import org.worldbank.transport.driver.activities.RecordFormSectionActivity;


import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test RecordFormConstantsActivity.
 *
 * Created by kathrynkillebrew on 1/12/16.
 */
public class ConstantsActivityFunctionalTests extends ActivityInstrumentationTestCase2<RecordFormConstantsActivity>
        implements RecordFormActivity.FormReadyListener{

    private RecordFormConstantsActivity activity;
    private CountDownLatch displayLock;
    private Instrumentation instrumentation;

    public ConstantsActivityFunctionalTests() {
        super(RecordFormConstantsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        instrumentation = getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), RecordFormConstantsActivity.class);
        intent.putExtra(RecordFormActivity.SECTION_ID, -1);
        setActivityIntent(intent);

        activity = getActivity();

        // wait for the form to display
        if (!activity.isFormReady()) {
            activity.setFormReadyListener(this);
            displayLock = new CountDownLatch(1);
            displayLock.await(3000, TimeUnit.MILLISECONDS);
        }

        // make sure form is done rendering
        FormActivityTestHelpers.waitForLoaderToDisappear(instrumentation, activity);
    }

    @Override
    public void formReadyCallback() {
        if (displayLock != null) {
            displayLock.countDown();
        }
    }

    @SmallTest
    public void testActivityExists() {
        assertNotNull("Record constants form activity is null", activity);
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
    public void testValidationErrorDisplay() {
        Button goButton = (Button) activity.findViewById(R.id.record_save_button_id);

        FormController formController = activity.getFormController();
        FormElementController whenCtl =  formController.getElement("occurredFrom");

        assertNotNull(whenCtl);

        LinearLayout ctlView = (LinearLayout) whenCtl.getView();
        List<View> whenViews = FormActivityTestHelpers.getAllChildViews(ctlView);

        // the first text view is the field label, and the second is for field error messages
        boolean foundLabel = false;
        AppCompatTextView errorMsgView = null;

        EditText foundWhenField = null;
        for (View view : whenViews) {
            if (view instanceof AppCompatTextView) {
                if (foundLabel) {
                    errorMsgView = (AppCompatTextView) view;
                } else {
                    foundLabel = true;
                }
            } else if (view instanceof EditText) {
                // the actual text entry field
                foundWhenField = (EditText) view;
            }
        }

        final EditText whenField = foundWhenField;

        assertNotNull("Did not find error message view for when occurred field", errorMsgView);
        assertNotNull("Did not find text entry field for when occurred field", whenField);

        // test validation on occurred from view
        Solo solo = new Solo(instrumentation, activity);
        solo.clickOnView(goButton);

        // wait for validation to finish
        solo.waitForView(activity.findViewById(R.id.form_progress));

        assertEquals("Did not get expected when occurred field error", "Occurred is a required field", errorMsgView.getText());
        assertEquals("When occurred error view is not visible", View.VISIBLE, errorMsgView.getVisibility());

        // now test that it progresses to next form section when validation error has been fixed
        solo.clickOnView(whenField);
        solo.clickOnButton("Done"); // date dialog confirm button

        // go!
        solo.clickOnView(goButton);

        assertTrue("Details form did not get launched after constants form", solo.waitForActivity(RecordFormSectionActivity.class));

        solo.finishOpenedActivities();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
