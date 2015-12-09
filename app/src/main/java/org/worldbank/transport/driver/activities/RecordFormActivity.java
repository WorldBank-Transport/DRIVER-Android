package org.worldbank.transport.driver.activities;

import android.util.Log;

import com.github.dkharrat.nexusdialog.FormController;
import com.github.dkharrat.nexusdialog.FormModel;
import com.github.dkharrat.nexusdialog.FormWithAppCompatActivity;
import com.github.dkharrat.nexusdialog.controllers.EditTextController;
import com.github.dkharrat.nexusdialog.controllers.FormSectionController;
import com.github.dkharrat.nexusdialog.controllers.SelectionController;

import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;
import com.google.gson.annotations.SerializedName;

// TODO: use these. They apply only to sections
import org.jsonschema2pojo.annotations.Description;
import org.jsonschema2pojo.annotations.Title;
import org.jsonschema2pojo.annotations.PluralTitle;
import org.jsonschema2pojo.annotations.Multiple;

import org.worldbank.transport.driver.models.Person;
import org.worldbank.transport.driver.models.Vehicle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.validation.constraints.NotNull;

/**
 * Created by kathrynkillebrew on 12/9/15.
 */
public class RecordFormActivity extends FormWithAppCompatActivity {
    @Override
    protected void initForm() {

        FormController formController = getFormController();
        formController.addSection(addSectionModel(Person.class));

        final Person person = new Person();

        FormModel formModel = new FormModel() {
            @Override
            protected void setBackingValue(String name, Object newValue) {
                Log.d("RecordFormActivity", "Going to set " + name);
                try {
                    Person.class.getField(name).set(person, newValue);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected Object getBackingValue(String name) {
                Log.d("RecordFormActivity", "Going to get " + name);

                try {
                    return Person.class.getField(name).get(person);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        formController.setModel(formModel);

        //getFormController().addSection(addSectionModel(Vehicle.class));

    }

    private FormSectionController addSectionModel(Class sectionModel) {
        // TODO: section label would come from parent model
        FormSectionController section = new FormSectionController(this, sectionModel.getSimpleName());
        Field[] fields = sectionModel.getDeclaredFields();

        // TODO: read/respect JsonPropertyOrder annotation, if present

        HashMap<String, Class> enums = new HashMap<>();

        // find enums for select lists
        Class[] classes = sectionModel.getDeclaredClasses();
        for (Class clazz : classes) {
            if (clazz.isEnum()) {
                Log.d("RecordFormActivity", "Found enum named " + clazz.getSimpleName());
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
                Log.d("RecordFormActivity", fieldName + " has annotation " + annotationType.getName());

                if (annotationType.equals(IsHidden.class)) {
                    IsHidden isHidden = (IsHidden) annotation;
                    if (isHidden.value()) {
                        Log.d("RecordFormActivity", "Skipping hidden field " + fieldName);
                        continue field_loop;
                    } else {
                        Log.w("RecordFormActivity", "Have false isHidden annotation, which is inefficient. Better just leave it off.");
                    }
                } else if (annotationType.equals(FieldType.class)) {
                    Log.d("RecordFormActivity", "Found a field type annotation");
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
                Log.e("RecordFormActivity", "No field type found for field " + fieldName);
                continue;
            }

            switch (fieldType) {
                case image:
                    Log.w("RecordFormActivity", "TODO: implement image field type");
                    break;
                case selectlist:
                    Log.d("RecordFormActivity", "Going to add select field");
                    // find enum with the options in it
                    Class enumClass = enums.get(fieldName);

                    if (enumClass == null) {
                        Log.e("RecordFormActivity", "No enum class found for field named " + fieldName);
                        continue;
                    }

                    Field[] enumFields = enumClass.getFields();

                    ArrayList<String> enumLabels = new ArrayList<>(enumFields.length);
                    for (Field enumField: enumFields) {
                        if (enumField.isEnumConstant()) {
                            String enumLabel = ((SerializedName)enumField.getAnnotation(SerializedName.class)).value();
                            enumLabels.add(enumLabel);
                        }
                    }

                    ArrayList<Object> enumValueObjectList = new ArrayList<>(Arrays.asList(enumClass.getEnumConstants()));

                    // TODO: no matter what gets passed for the prompt argument, it seems to always display "Select"
                    section.addElement(new SelectionController(this, fieldName, fieldLabel, isRequired, "Select", enumLabels, enumValueObjectList));

                    break;
                case text:
                    section.addElement(new EditTextController(this, fieldName, fieldLabel, fieldLabel, isRequired));
                    break;
                case reference:
                    Log.w("RecordFormActivity", "TODO: implement reference field type");
                    break;
                default:
                    Log.e("RecordFormActivity", "Don't know what to do with field type " + fieldType.toString());
                    break;
            }
        }

        return section;
    }
}
