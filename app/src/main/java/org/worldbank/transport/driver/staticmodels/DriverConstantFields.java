package org.worldbank.transport.driver.staticmodels;

import android.location.Location;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;
import org.worldbank.transport.driver.annotations.ConstantFieldType;
import org.worldbank.transport.driver.annotations.ConstantFieldTypes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

/**
 * Constant fields that exist for every record, in addition to those specified in DriverSchema.
 * Unlike the models built from the DriverSchema JSON, these are not generated automatically;
 * the same annotations have been used here to make forms built from this class compatible with
 * those built from the generated DriverSchema classes.
 *
 * Created by kathrynkillebrew on 1/8/16.
 */
@JsonPropertyOrder({
        "Occurred",
        "Weather",
        "Light"
})
public class DriverConstantFields {

    // present these in user form
    // constant fields on Record model in DRF
    // https://github.com/azavea/ashlar/blob/develop/ashlar/models.py

    @SerializedName("Occurred")
    @ConstantFieldType(ConstantFieldTypes.date)
    @NotNull
    public Date occurredFrom;

    // occurredTo is set to match value of occurredFrom before upload


    // location form component is added outside of the form builder
    @SerializedName("Location")
    @ConstantFieldType(ConstantFieldTypes.location)
    @IsHidden(true)
    public Location location;


    // select fields with enumerations
    @SerializedName("Weather")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public WeatherEnum Weather;

    @SerializedName("Light")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public LightEnum Light;

    public enum WeatherEnum {

        @SerializedName("Clear day")
        CLEAR_DAY("clear-day"),
        @SerializedName("Clear night")
        CLEAR_NIGHT("clear-night"),
        @SerializedName("Cloudy")
        CLOUDY("cloudy"),
        @SerializedName("Fog")
        NIGHT("fog"),
        @SerializedName("Hail")
        HAIL("hail"),
        @SerializedName("Partly cloudy day")
        PARTLY_CLOUDY_DAY("partly-cloudy-day"),
        @SerializedName("Partly cloudy night")
        PARTLY_CLOUDY_NIGHT("partly-cloudy-night"),
        @SerializedName("Rain")
        RAIN("rain"),
        @SerializedName("Sleet")
        SLEET("sleet"),
        @SerializedName("Snow")
        SNOW("snow"),
        @SerializedName("Thunderstorm")
        THUNDERSTORM("thunderstorm"),
        @SerializedName("Tornado")
        TORNADO("tornado"),
        @SerializedName("Wind")
        WIND("wind");

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

        @SerializedName("Dawn")
        DAWN("dawn"),
        @SerializedName("Day")
        DAY("day"),
        @SerializedName("Dusk")
        DUSK("dusk"),
        @SerializedName("Night")
        NIGHT("night");

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
