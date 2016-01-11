package org.worldbank.transport.driver.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.tasks.ValidationTask;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.staticmodels.DriverConstantFields;
import org.worldbank.transport.driver.utilities.RecordFormSectionManager;


/**
 * Present form for constant fields as first form, before those for DriverSchema fields.
 *
 * Created by kathrynkillebrew on 1/8/16.
 */
public class RecordFormConstantsActivity extends RecordFormActivity {

    private static final String LOG_LABEL = "RecordConstants";

    // section label (web app has none for this section)
    public static final String CONSTANTS_SECTION_NAME = "Record Basic Information";

    @Override
    public RelativeLayout buildButtonBar() {
        // put buttons in a relative layout for positioning on right or left
        RelativeLayout buttonBar = new RelativeLayout(this);
        buttonBar.setId(R.id.record_button_bar_id);
        buttonBar.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        // add next/save button
        Button goBtn = new Button(this);
        RelativeLayout.LayoutParams goBtnLayoutParams = new RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT);
        goBtnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        goBtn.setLayoutParams(goBtnLayoutParams);

        goBtn.setId(R.id.record_save_button_id);

        // add 'next' button
        haveNext = true;
        goBtn.setText(getString(R.string.record_next_button));

        final RecordFormActivity thisActivity = this;
        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // launch DriverSchema first form here
                goPrevious = false;
                Log.d(LOG_LABEL, "Going to validate constant fields");
                new ValidationTask(thisActivity).execute();
            }
        });

        buttonBar.addView(goBtn);

        return buttonBar;
    }

    @Override
    public void proceed() {
        int goToSectionId = 0;
        Log.d(LOG_LABEL, "Going to section #" + String.valueOf(goToSectionId));
        Intent intent = new Intent(this,
                RecordFormSectionManager.getActivityClassForSection(goToSectionId));

        intent.putExtra(RecordFormActivity.SECTION_ID, goToSectionId);
        startActivity(intent);
    }

    @Override
    protected Object getModelObject() {
        return app.getEditConstants();
    }

    @Override
    public FormController createFormController() {

        Log.d(LOG_LABEL, "createFormController called for constants activity");

        sectionField = null; // does not apply here

        sectionClass = DriverConstantFields.class;

        Object section = getModelObject();

        sectionLabel = CONSTANTS_SECTION_NAME;

        if (section != null) {
            return new FormController(this, section);
        } else {
            Log.e(LOG_LABEL, "Section object not found for record constants");
        }

        return null;
    }
}
