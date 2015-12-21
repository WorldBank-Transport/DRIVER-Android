package org.worldbank.transport.driver.ActivityTests;

import android.app.Instrumentation;
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
import android.widget.LinearLayout;

import com.github.dkharrat.nexusdialog.FormController;
import com.github.dkharrat.nexusdialog.FormElementController;

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
    public void testValidationErrorDisplay() {
        ViewGroup containerView = (ViewGroup) activity.findViewById(R.id.form_elements_container);
        Button goButton = (Button) activity.findViewById(R.id.record_save_button_id);

        FormController formController = activity.getFormController();
        FormElementController licenseNoCtl =  formController.getElement("LicenseNumber");
        assertNotNull(licenseNoCtl);

        LinearLayout ctlView = (LinearLayout) licenseNoCtl.getView();
        List<View> licenseNoViews = getAllChildViews(ctlView);

        ArrayList<AppCompatTextView> textViews = new ArrayList<>(2);
        for (View view : licenseNoViews) {
            if (view instanceof AppCompatTextView) {
                textViews.add((AppCompatTextView) view);
                Log.d("RecordFormTests", "License # field text: " + ((AppCompatTextView) view).getText());
            }
        }

        assertEquals("Unexpected number of text views in form control", 2, textViews.size());

        // test validation on license view
        Instrumentation instrumentation = getInstrumentation();
        instrumentation.waitForIdleSync();
        TouchUtils.tapView(this, ctlView);
        instrumentation.waitForIdleSync();
        instrumentation.sendStringSync("123");
        instrumentation.waitForIdleSync();
        TouchUtils.scrollToBottom(this, activity, containerView);
        instrumentation.waitForIdleSync();
        TouchUtils.tapView(this, goButton);
        instrumentation.waitForIdleSync();

        assertEquals("Did not get expected license # field error", "size must be between 6 and 8", textViews.get(1).getText());
        assertEquals("License # error view is not visible", View.VISIBLE, textViews.get(1).getVisibility());

        // now test that error clears from display once it has been fixed
        instrumentation.waitForIdleSync();
        TouchUtils.tapView(this, ctlView);
        instrumentation.waitForIdleSync();
        instrumentation.sendStringSync("456");
        instrumentation.waitForIdleSync();
        TouchUtils.tapView(this, goButton);
        instrumentation.waitForIdleSync();

        assertNotSame("License # error not cleared", View.VISIBLE, textViews.get(1).getVisibility());
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
