package org.worldbank.transport.driver.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.azavea.androidvalidatedforms.tasks.ValidationTask;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.utilities.RecordFormPaginator;

import java.lang.reflect.Field;

/**
 * Form for a section that does not contain multiple elements.
 *
 * Created by kathrynkillebrew on 12/31/15.
 */
public class RecordFormSectionActivity extends RecordFormActivity {

    private static final String LOG_LABEL = "FormSectionActivity";

    /**
     * Helper to build a layout with previous/next/save buttons.
     *
     * @return View with buttons with handlers added to it
     */
    @Override
    public RelativeLayout buildButtonBar() {
        // reference to this, for use in button actions (validation task makes weak ref)
        final RecordFormActivity thisActivity = this;

        // put buttons in a relative layout for positioning on right or left
        RelativeLayout buttonBar = new RelativeLayout(this);
        buttonBar.setId(R.id.record_button_bar_id);
        buttonBar.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        // add 'previous' button
        if (sectionId > 0) {
            havePrevious = true;
            Button backBtn = new Button(this);
            RelativeLayout.LayoutParams backBtnLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);

            backBtnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            backBtn.setLayoutParams(backBtnLayoutParams);

            backBtn.setId(R.id.record_back_button_id);
            backBtn.setText(getText(R.string.record_previous_button));
            buttonBar.addView(backBtn);

            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(LOG_LABEL, "Back button clicked");

                    // set this to let callback know next action to take
                    goPrevious = true;
                    new ValidationTask(thisActivity).execute();
                }
            });
        } else {
            havePrevious = false;
            goPrevious = false;
        }

        // add next/save button
        Button goBtn = new Button(this);
        RelativeLayout.LayoutParams goBtnLayoutParams = new RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT);
        goBtnLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        goBtn.setLayoutParams(goBtnLayoutParams);

        goBtn.setId(R.id.record_save_button_id);

        if (RecordFormPaginator.sectionHasNext(sectionId)) {
            // add 'next' button
            haveNext = true;
            goBtn.setText(getString(R.string.record_next_button));

        } else {
            haveNext = false;
            // add 'save' button
            goBtn.setText(getString(R.string.record_save_button));
        }

        buttonBar.addView(goBtn);

        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_LABEL, "Next/save button clicked");
                goPrevious = false;
                new ValidationTask(thisActivity).execute();
            }
        });

        return buttonBar;
    }

    // TODO: this section movement logic will likely need to be shared somehow with the item list view
    // Maybe should have a shared form-activity-launcher thing that decides what to do to fire up a form section.

    @Override
    public void proceed() {
        {
            int goToSectionId = sectionId;

            if (goPrevious) {
                if (!havePrevious) {
                    Log.e(LOG_LABEL, "Trying to go to previous, but there is none!");
                    return;
                } else {
                    goToSectionId--;
                }
            } else if (haveNext) {
                Log.d(LOG_LABEL, "Proceed to next section now");
                goToSectionId++;
            } else {
                // TODO: at end; save
                Log.d(LOG_LABEL, "Form complete! Now what?");
                ////////////////////////
                return;
            }

            Log.d(LOG_LABEL, "Going to section #" + String.valueOf(goToSectionId));
            Intent intent = new Intent(this,
                    RecordFormPaginator.getActivityClassForSection(goToSectionId));

            intent.putExtra(RecordFormActivity.SECTION_ID, goToSectionId);
            startActivity(intent);
        }
    }
}

