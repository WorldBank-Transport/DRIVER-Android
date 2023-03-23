package org.worldbank.transport.driver.staticmodels;

import android.location.Location;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;

import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;
import org.worldbank.transport.driver.annotations.ConstantFieldType;
import org.worldbank.transport.driver.annotations.ConstantFieldTypes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;

/**
 * Constant fields that exist for every record, in addition to those specified in DriverSchema.
 * Unlike the models built from the DriverSchema JSON, these are not generated automatically;
 * the same annotations have been used here to make forms built from this class compatible with
 * those built from the generated DriverSchema classes.
 *
 * Created by kathrynkillebrew on 1/8/16.
 */

// property order for this class uses field names instead of SerializedName gson annotation
@JsonPropertyOrder({
        "occurredFrom",
        "Weather",
        "Light"
})
public class DriverConstantFields {

    // present these in user form
    // constant fields on Record model in DRF
    // https://github.com/azavea/ashlar/blob/develop/ashlar/models.py

    @Past
    @ConstantFieldType(ConstantFieldTypes.date)
    @NotNull
    public Date occurredFrom;

    // occurredTo is set to match value of occurredFrom before upload

    // location form component is added outside of the form builder
    @ConstantFieldType(ConstantFieldTypes.location)
    @IsHidden(true)
    public Location location;


    // select fields with enumerations
    @Expose
    @FieldType(FieldTypes.selectlist)
    public WeatherEnum Weather;

    @Expose
    @FieldType(FieldTypes.selectlist)
    public LightEnum Light;

    public enum WeatherEnum {

        FAIR("Fair"),
        RAIN("Rain"),
        WIND("Wind"),
        FOG("Fog");

        private final String value;
        private final static Map<String, DriverConstantFields.WeatherEnum> CONSTANTS = new HashMap<>();

        static {
            for (DriverConstantFields.WeatherEnum c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        WeatherEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static DriverConstantFields.WeatherEnum fromValue(String value) {
            DriverConstantFields.WeatherEnum constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }

    public enum LightEnum {

        DAY("Day"),
        DAWN_DUSK("Dawn_dusk"),
        NIGHT_LIT("Night_lit"),
        NIGHT_UNLIT("Night_unlit");

        private final String value;
        private final static Map<String, LightEnum> CONSTANTS = new HashMap<>();

        static {
            for (LightEnum c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        LightEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static LightEnum fromValue(String value) {
            LightEnum constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }
}
