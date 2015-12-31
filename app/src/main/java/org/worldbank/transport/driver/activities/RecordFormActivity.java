package org.worldbank.transport.driver.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.FormWithAppCompatActivity;
import com.azavea.androidvalidatedforms.controllers.EditTextController;
import com.azavea.androidvalidatedforms.controllers.FormSectionController;
import com.azavea.androidvalidatedforms.controllers.SelectionController;

import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;

import com.azavea.androidvalidatedforms.tasks.ValidationTask;
import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotNull;

// annotations that apply only to sections
import org.jsonschema2pojo.annotations.Description;
import org.jsonschema2pojo.annotations.Title;
import org.jsonschema2pojo.annotations.PluralTitle;
import org.jsonschema2pojo.annotations.Multiple;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by kathrynkillebrew on 12/9/15.
 */
public class RecordFormActivity extends FormWithAppCompatActivity {

    public interface FormReadyListener {
        void formReadyCallback();
    }

    // path to model classes created by jsonschema2pojo
    // this must match the targetPackage declared in the gradle build file (with a trailing period)
    private static final String MODEL_PACKAGE = "org.worldbank.transport.driver.models.";
    public static final String SECTION_ID = "driver_section_id";
    private static final String LOG_LABEL = "RecordFormActivity";

    private DriverAppContext mAppContext;
    DriverApp app;

    // flag that is true once form has been displayed
    private boolean formReady = false;
    private FormReadyListener formReadyListener;

    private DriverSchema currentlyEditing;
    private int sectionId;
    private String sectionName;
    private String[] sectionOrder;
    private Class sectionClass;
    private String sectionLabel;

    // have next/previous sections
    private boolean haveNext = false;
    private boolean havePrevious = false;

    // if selected action is to go to previous (if false, go to next or save)
    private boolean goPrevious = false;

    /**
     * Non-default constructor for testing, to set the application context.
     * @param context Mock context
     */
    public RecordFormActivity(DriverAppContext context) {
        super();
        mAppContext = context;
    }

    /**
     * Default constructor, for testing.
     */
    public RecordFormActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set up some state before calling super
        mAppContext = new DriverAppContext((DriverApp) getApplicationContext());
        app = mAppContext.getDriverApp();
        currentlyEditing = app.getEditObject();

        Bundle bundle = getIntent().getExtras();
        sectionId = bundle.getInt(SECTION_ID);
        sectionOrder = app.getSchemaSectionOrder();

        // calling super will call createFormController in turn
        super.onCreate(savedInstanceState);
    }

    /**
     * This method gets called by the form building background task after it finishes.
     */
    @Override
    public void displayForm() {
        super.displayForm();

        // now form has been built, add previous, next, and/or save buttons to it
        ViewGroup containerView = (ViewGroup) findViewById(R.id.form_elements_container);
        RelativeLayout buttonBar = buildButtonBar();
        containerView.addView(buttonBar);

        // alert that form is ready to go
        formReady = true;
        if (formReadyListener != null) {
            formReadyListener.formReadyCallback();
        }
    }

    /**
     * Helper to build a layout with previous/next/save buttons.
     *
     * @return View with buttons with handlers added to it
     */
    private RelativeLayout buildButtonBar() {
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
            RelativeLayout.LayoutParams backBtnLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
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

        if (sectionId < sectionOrder.length - 1) {
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

    public boolean isFormReady() {
        return formReady;
    }

    public void setFormReadyListener(FormReadyListener listener) {
        formReadyListener = listener;
    }

    @Override
    public void validationComplete(boolean isValid) {
        Log.d(LOG_LABEL, "Valid? " + String.valueOf(isValid));

        if (isValid) {
            proceed();
        } else {
            // validation errors found in section
            // TODO: show warning dialog with options to proceed or stay to fix errors?
        }
    }

    private void proceed() {
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

            try {
                sectionName = sectionOrder[sectionId + 1];
                Field sectionField = DriverSchema.class.getField(sectionName);
                if (sectionHasMultiple(sectionField)) {

                    // TODO: next section has multiple entries
                    // launch a separate list view intent
                    Log.d(LOG_LABEL, "Next section has multiple entries");

                }
            } catch (NoSuchFieldException e) {
                Log.e(LOG_LABEL, "Could not find section field " + sectionName);
                e.printStackTrace();
            } catch (IndexOutOfBoundsException e) {
                Log.e(LOG_LABEL, "Invalid form section offset: " + String.valueOf(sectionId + 1));
            }

            //////////////////////////////

            goToSectionId++;
        } else {
            // TODO: at end; save
            Log.d(LOG_LABEL, "Form complete! Now what?");
            ////////////////////////
            return;
        }

        Log.d(LOG_LABEL, "Going to section #" + String.valueOf(goToSectionId));

        Intent intent = new Intent(this, RecordFormActivity.class);
        intent.putExtra(RecordFormActivity.SECTION_ID, goToSectionId);
        startActivity(intent);
    }

    private boolean sectionHasMultiple(Field sectionField) {
        Multiple multipleAnnotation = sectionField.getAnnotation(Multiple.class);

        if (multipleAnnotation != null && multipleAnnotation.value()) {
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public FormController createFormController() {

        Log.d(LOG_LABEL, "createFormController called");

        // section offset was passed to activity in intent; find section to use here
        try {
            sectionName = sectionOrder[sectionId];
            Field sectionField = DriverSchema.class.getField(sectionName);

            Log.d(LOG_LABEL, "Found sectionField " + sectionField.getName());

            // attempt to get the section from the currently editing model object;
            // it will not exist if creating a new record
            Object section = sectionField.get(currentlyEditing);
            sectionClass = Class.forName(MODEL_PACKAGE + sectionName);
            sectionLabel = sectionClass.getSimpleName();

            if (section == null) {
                Log.d(LOG_LABEL, "No section found with name " + sectionName);
                // instantiate a new thing then
                section = sectionClass.newInstance();

                // add the new section to the currently editing model object
                sectionField.set(currentlyEditing, section);

                if (sectionField.get(currentlyEditing) == null) {
                    Log.e(LOG_LABEL, "Section field is still null after set to new instance!");
                } else {
                    Log.d(LOG_LABEL, "Section field successfully set to new instance");
                }
            } else {
                // have existing values to edit
                Log.d(LOG_LABEL, "Found existing section " + sectionName);
            }

            if (sectionHasMultiple(sectionField)) {
                Log.d(LOG_LABEL, "Section " + sectionName + " has multiples");
                // TODO: make list of things instead of the thing

                // get plural title for section label if have multiple
                PluralTitle pluralAnnotation = sectionField.getAnnotation(PluralTitle.class);
                if (pluralAnnotation != null) {
                    String pluralTitle = pluralAnnotation.value();
                    if (pluralTitle.length() > 0) {
                        sectionLabel = pluralTitle;
                    } else {
                        Log.w(LOG_LABEL, "No plural title found for section");
                    }
                } else {
                    Log.w(LOG_LABEL, "No plural title found for section");
                }

                // expect an ArrayList
                Log.d(LOG_LABEL, "Section class is " + section.getClass().getName());

                if (ArrayList.class.isInstance(section)) {
                    Log.d(LOG_LABEL, "Section is a list class");
                    ArrayList sectionList = (ArrayList) section;

                    if (sectionList.size() == 0) {
                        Log.d(LOG_LABEL, "Adding a thing to the section collection");
                        // create a new thing of correct type and add it

                        // this call produces an unchecked warning
                        sectionList.add(sectionClass.newInstance());
                    }

                    // TODO: deal with list presentation. For now, just use first one
                    return new FormController(this, sectionList.get(0));
                    ////////////////////////////////////////////////////

                } else {
                    Log.e(LOG_LABEL, "Section has unexpected type for multiple annotation");
                }
            } else {
                Log.d(LOG_LABEL, "Section " + sectionName + " does NOT have multiples");

                // use singular title for section label
                Title titleAnnotation = sectionField.getAnnotation(Title.class);
                if (titleAnnotation != null) {
                    String title = titleAnnotation.value();
                    if (title.length() > 0) {
                        sectionLabel = title;
                    } else {
                        Log.w(LOG_LABEL, "No title found for section");
                    }
                } else {
                    Log.w(LOG_LABEL, "No title found for section");
                }
            }

            return new FormController(this, section);

        } catch (ClassNotFoundException e) {
            Log.e(LOG_LABEL, "Could not fine class named " + sectionName);
            e.printStackTrace();
        } catch (InstantiationException e) {
            Log.e(LOG_LABEL, "Failed to instantiate new section " + sectionName);
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            Log.e(LOG_LABEL, "Could not find section field " + sectionName);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(LOG_LABEL, "Do not have access to section field " + sectionName);
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            Log.e(LOG_LABEL, "Invalid form section offset: " + String.valueOf(sectionId));
        }
        return null;
    }

    @Override
    public void initForm() {
        final FormController formController = getFormController();
        if (sectionClass != null) {

            // TODO: drop the variables, since they're class fields?
            // or will we ever want multiple sections on a screen?
            formController.addSection(addSectionModel(sectionClass, sectionLabel));
        } else {
            // TODO: getting here if have list of things for section, or existing section
            Log.e(LOG_LABEL, "No section class; cannot initialize form");
        }
    }

    private FormSectionController addSectionModel(Class sectionModel, String sectionLabel) {
        FormSectionController section = new FormSectionController(this, sectionLabel);
        Field[] fields = sectionModel.getDeclaredFields();

        // TODO: read/respect JsonPropertyOrder annotation of fields, if present

        HashMap<String, Class> enums = new HashMap<>();

        // find enums for select lists
        Class[] classes = sectionModel.getDeclaredClasses();
        for (Class clazz : classes) {
            if (clazz.isEnum()) {
                Log.d(LOG_LABEL, "Found enum named " + clazz.getSimpleName());
                enums.put(clazz.getSimpleName(), clazz);
            }
        }

        field_loop:
        for (Field field: fields) {
            String fieldName = field.getName();
            String fieldLabel = fieldName;
            FieldTypes fieldType = null;
            boolean isRequired = false;

            Annotation[] annotations = field.getDeclaredAnnotations();

            for (Annotation annotation: annotations) {

                Class annotationType = annotation.annotationType();
                Log.d(LOG_LABEL, fieldName + " has annotation " + annotationType.getName());

                if (annotationType.equals(IsHidden.class)) {
                    IsHidden isHidden = (IsHidden) annotation;
                    if (isHidden.value()) {
                        Log.d(LOG_LABEL, "Skipping hidden field " + fieldName);
                        continue field_loop;
                    } else {
                        Log.w(LOG_LABEL, "Have false isHidden annotation, which is inefficient. Better just leave it off.");
                    }
                } else if (annotationType.equals(FieldType.class)) {
                    Log.d(LOG_LABEL, "Found a field type annotation");
                    FieldType fieldTypeAnnotation = (FieldType) annotation;
                    fieldType = fieldTypeAnnotation.value();
                } else if (annotationType.equals(SerializedName.class)) {
                    SerializedName serializedName = (SerializedName) annotation;
                    fieldLabel = serializedName.value();
                } else if (annotationType.equals(NotNull.class)) {
                    isRequired = true;
                }
            }

            if (fieldType == null) {
                // TODO: in this case, it's probably a subsection
                Log.e(LOG_LABEL, "No field type found for field " + fieldName);
                continue;
            }

            switch (fieldType) {
                case image:
                    Log.w(LOG_LABEL, "TODO: implement image field type");
                    break;
                case selectlist:
                    Log.d(LOG_LABEL, "Going to add select field");
                    // find enum with the options in it
                    Class enumClass = enums.get(fieldName);

                    if (enumClass == null) {
                        Log.e(LOG_LABEL, "No enum class found for field named " + fieldName);
                        continue;
                    }

                    Field[] enumFields = enumClass.getFields();

                    ArrayList<String> enumLabels = new ArrayList<>(enumFields.length);
                    for (Field enumField: enumFields) {
                        if (enumField.isEnumConstant()) {
                            String enumLabel = (enumField.getAnnotation(SerializedName.class)).value();
                            enumLabels.add(enumLabel);
                        }
                    }

                    ArrayList<Object> enumValueObjectList = new ArrayList<>(Arrays.asList(enumClass.getEnumConstants()));

                    // TODO: no matter what gets passed for the prompt argument, it seems to always display "Select"
                    section.addElement(new SelectionController(this, fieldName, fieldLabel, isRequired, "Select", enumLabels, enumValueObjectList));

                    break;
                case text:
                    section.addElement(new EditTextController(this, fieldName, fieldLabel, fieldLabel));
                    break;
                case reference:
                    Log.w(LOG_LABEL, "TODO: implement reference field type");
                    break;
                default:
                    Log.e(LOG_LABEL, "Don't know what to do with field type " + fieldType.toString());
                    break;
            }
        }

        return section;
    }
}

