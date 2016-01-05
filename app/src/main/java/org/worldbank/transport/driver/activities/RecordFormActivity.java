package org.worldbank.transport.driver.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.azavea.androidvalidatedforms.FormController;
import com.azavea.androidvalidatedforms.FormWithAppCompatActivity;
import com.azavea.androidvalidatedforms.controllers.EditTextController;
import com.azavea.androidvalidatedforms.controllers.FormSectionController;
import com.azavea.androidvalidatedforms.controllers.LabeledFieldController;
import com.azavea.androidvalidatedforms.controllers.SelectionController;

import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;

import com.fasterxml.jackson.databind.util.EnumValues;
import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotNull;

import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.models.DriverSchema;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.utilities.DriverUtilities;
import org.worldbank.transport.driver.utilities.RecordFormPaginator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Base class for creating dynamic forms for DriverSchema sections.
 *
 * Created by kathrynkillebrew on 12/9/15.
 */
public abstract class RecordFormActivity extends FormWithAppCompatActivity {

    public interface FormReadyListener {
        void formReadyCallback();
    }

    public static final String SECTION_ID = "driver_section_id";
    private static final String LOG_LABEL = "RecordFormActivity";

    private DriverAppContext mAppContext;

    // flag that is true once form has been displayed
    private boolean formReady = false;
    private FormReadyListener formReadyListener;

    protected DriverSchema currentlyEditing;

    // TODO: only belongs to sections
    protected int sectionId;

    // TODO: rename to remove 'section' ('model'?)
    protected Class sectionClass;
    protected String sectionLabel;
    protected Field sectionField;

    // have next/previous sections
    protected boolean haveNext = false;
    protected boolean havePrevious = false;

    // if selected action is to go to previous (if false, go to next or save)
    protected boolean goPrevious = false;

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
        currentlyEditing = mAppContext.getDriverApp().getEditObject();

        Bundle bundle = getIntent().getExtras();
        sectionId = bundle.getInt(SECTION_ID);

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
     * Helper to build a layout with buttons (previous/next/save/cancel).
     *
     * @return View with buttons with handlers added to it
     */
    public abstract RelativeLayout buildButtonBar();

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
            Log.w(LOG_LABEL, "TODO: handle validation errors");
            // validation errors found in section
            // TODO: show warning dialog with options to proceed or stay to fix errors?
        }
    }

    public abstract void proceed();

    protected abstract Object getModelObject();

    @Override
    public FormController createFormController() {

        Log.d(LOG_LABEL, "createFormController called");

        String sectionName = RecordFormPaginator.getSectionName(sectionId);

        // section offset was passed to activity in intent; find section to use here
        sectionField = RecordFormPaginator.getFieldForSectionName(sectionName);

        if (sectionField == null) {
            Log.e(LOG_LABEL, "Section field named " + sectionName + " not found.");
            return null;
        }

        Log.d(LOG_LABEL, "Found sectionField " + sectionField.getName());
        sectionClass = RecordFormPaginator.getSectionClass(sectionName);

        if (sectionClass == null) {
            Log.e(LOG_LABEL, "No section class; cannot initialize form");
            return null;
        }

        Object section = getModelObject();

        // use singular title for form section label
        // TODO: also use 'Description' annotation somewhere?
        sectionLabel = sectionClass.getSimpleName(); // default
        sectionLabel = RecordFormPaginator.getSingleTitle(sectionField, sectionLabel);

        if (section != null) {
            return new FormController(this, section);
        } else {
            Log.e(LOG_LABEL, "Section object not found for " + sectionName);
        }

        return null;
    }

    @Override
    public void initForm() {
        final FormController formController = getFormController();
        formController.addSection(addSectionModel());
    }

    private FormSectionController addSectionModel() {
        FormSectionController section = new FormSectionController(this, sectionLabel);
        Field[] fields = sectionClass.getDeclaredFields();

        HashMap<String, Class> enums = new HashMap<>();

        // find enums for select lists
        Class[] classes = sectionClass.getDeclaredClasses();
        for (Class clazz : classes) {
            if (clazz.isEnum()) {
                Log.d(LOG_LABEL, "Found enum named " + clazz.getSimpleName());
                enums.put(clazz.getSimpleName(), clazz);
            }
        }

        // map of field names to their form controls
        HashMap<String, LabeledFieldController> fieldControls = new HashMap<>(fields.length);

        field_loop:
        for (Field field: fields) {
            LabeledFieldController control = null;
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
                // TODO: in this case, it's probably a subsection. Does that ever happen?
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

                    // TODO: use serialized name annotation for enums?
                    // enum fields are *not* returned in declared order (getEnumConstants returns declared order)
                    /*
                    Field[] enumFields = enumClass.getDeclaredFields();

                    ArrayList<String> enumLabels = new ArrayList<>(enumFields.length);
                    for (Field enumField: enumFields) {
                        if (enumField.isEnumConstant()) {
                            String enumLabel = (enumField.getAnnotation(SerializedName.class)).value();
                            enumLabels.add(enumLabel);
                        }
                    }
                    */

                    ArrayList<Object> enumValueObjectList = new ArrayList<>(Arrays.asList(enumClass.getEnumConstants()));

                    ArrayList<String> enumLabels = new ArrayList<>(enumValueObjectList.size());
                    for (Object enumConstant: enumValueObjectList) {
                        enumLabels.add(enumConstant.toString());
                    }

                    Log.d(LOG_LABEL, "enumLabels: " + enumLabels.toString());
                    Log.d(LOG_LABEL, "enumValues: " + enumValueObjectList.toString());

                    // TODO: no matter what gets passed for the prompt argument, it seems to always display "Select"
                    control = new SelectionController(this, fieldName, fieldLabel, isRequired, "Select", enumLabels, enumValueObjectList);
                    break;
                case text:
                    control = new EditTextController(this, fieldName, fieldLabel);
                    break;
                case reference:
                    // TODO: implement
                    Log.w(LOG_LABEL, "TODO: implement reference field type");
                    break;
                default:
                    Log.e(LOG_LABEL, "Don't know what to do with field type " + fieldType.toString());
                    break;
            }

            if (control != null) {
                fieldControls.put(fieldName, control);
            }
        }

        // read/respect JsonPropertyOrder annotation of fields, if present
        // (if annotation missing, will add fields in order of declaration)
        String[] orderedFields = DriverUtilities.getFieldOrder(sectionClass);

        // add form controls in order
        for (String nextField : orderedFields) {
            LabeledFieldController control = fieldControls.get(nextField);
            if (control != null) {
                section.addElement(control);
            } else {
                Log.w(LOG_LABEL, "No control found for ordered field " + nextField);
            }
        }

        return section;
    }
}

