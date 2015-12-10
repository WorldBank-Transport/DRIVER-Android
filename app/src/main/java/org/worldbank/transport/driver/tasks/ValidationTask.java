package org.worldbank.transport.driver.tasks;

/**
 * Created by kathrynkillebrew on 12/7/15.
 */

import javax.validation.ConstraintViolation;
import android.os.AsyncTask;
import android.util.Log;

import org.hibernate.validator.HibernateValidator;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.Validation;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.spi.ValidationProvider;

/**
 * Created by kat on 11/28/15.
 */
@SuppressWarnings("unchecked")
public class ValidationTask<T> extends AsyncTask<T, String, Set<ConstraintViolation<T>>> {

    public interface ValidationCallbackListener {
        void callback(Set<ConstraintViolation> violations);
    }

    ValidationCallbackListener listener;
    String propertyName;

    private static Validator validator = getValidator();

    public ValidationTask(ValidationCallbackListener listener) {
        this.listener = listener;
    }

    /**
     * Optionally create a validation task for a specific property on the object
     *
     * @param listener The callback handler
     * @param propertyName Name of the field on the object to validate
     */
    public ValidationTask(ValidationCallbackListener listener, String propertyName) {
        this(listener);
        this.propertyName = propertyName;
    }

    @Override
    protected Set<ConstraintViolation<T>> doInBackground(T... objects) {
        Log.d("ValidationTask", "in validation task...");

        T obj = objects[0];

        long startTime = System.currentTimeMillis();

        Set<ConstraintViolation<T>> errors;
        if (propertyName != null) {
            // validate only the specified property on the object
            errors = validator.validateProperty(obj, propertyName);
        } else {
            // validate entire object if property not specified
            errors = validator.validate(obj);
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        Log.d("ValidationTask", "Total validation time: " +
                twoDForm.format(elapsedTime / 1000.0f) + " seconds");

        for (ConstraintViolation<T> error : errors) {
            Log.d("ValidationTask", "Found constraint violation:" + error.getMessage());
        }

        return errors;
    }

    protected void onPostExecute(Set<ConstraintViolation> errors) {
        if (errors.isEmpty()) {
            Log.d("ValidationTask", "Hooray, object is valid");
        } else {
            Log.d("ValidationTask", "Validation errors found");
        }

        listener.callback(errors);
    }

    private static Validator getValidator() {
        return Validation
                .byProvider(HibernateValidator.class)
                .providerResolver(new ValidationProviderResolver() {
                    @Override
                    public List<ValidationProvider<?>> getValidationProviders() {
                        return Collections.<ValidationProvider<?>>singletonList(new HibernateValidator());
                    }
                })
                .configure()
                .ignoreXmlConfiguration()

                /*
                // Can do something like this for custom messages pulled from strings file
                .messageInterpolator(new MessageInterpolator() {
                    @Override
                    public String interpolate(String messageTemplate, Context context) {
                        int id = ApplicationContext.getApplication().getResources().getIdentifier(messageTemplate, "string", R.class.getPackage().getName());
                        return ApplicationContext.getApplication().getString(id);
                    }

                    @Override
                    public String interpolate(String messageTemplate, Context context, Locale locale) {
                        return interpolate(messageTemplate, context);
                    }
                })
                */

                .buildValidatorFactory().getValidator();
    }
}
