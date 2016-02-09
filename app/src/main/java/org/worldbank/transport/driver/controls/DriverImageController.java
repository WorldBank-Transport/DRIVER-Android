package org.worldbank.transport.driver.controls;

import android.content.Context;

import com.azavea.androidvalidatedforms.controllers.ImageController;

import org.jsonschema2pojo.media.SerializableMedia;

/**
 * Subclass image controller to get and set path on holder class used for Gson serialization.
 *
 * Created by kathrynkillebrew on 2/4/16.
 */
public class DriverImageController extends ImageController {
    public DriverImageController(Context ctx, String name, String labelText, boolean isRequired) {
        super(ctx, name, labelText, isRequired);
    }

    @Override
    protected Object getModelValue() {
        Object current = super.getModelValue();

        if (current != null && current.getClass().equals(SerializableMedia.class)) {
            return ((SerializableMedia)current).path;
        }

        return null;
    }

    @Override
    protected void setModelValue(String newImagePath) {
        SerializableMedia media = null;

        if (newImagePath != null && !newImagePath.isEmpty()) {
            media = new SerializableMedia();
            media.path = newImagePath;
        }

        getModel().setValue(getName(), media);
    }
}
