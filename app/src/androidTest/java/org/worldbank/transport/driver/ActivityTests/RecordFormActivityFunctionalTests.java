package org.worldbank.transport.driver.ActivityTests;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import org.worldbank.transport.driver.activities.RecordFormActivity;


/**
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

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
