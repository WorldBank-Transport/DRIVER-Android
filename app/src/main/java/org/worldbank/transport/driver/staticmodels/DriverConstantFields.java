package org.worldbank.transport.driver.staticmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;

import java.util.HashMap;
import java.util.Map;

/**
 * Constant fields that exist for every record, in addition to those specified in DriverSchema.
 * Unlike the models built from the DriverSchema JSON, these are not generated automatically;
 * the same annotations have been used here to make forms built from this class compatible with
 * those built from the generated DriverSchema classes.
 *
 * Created by kathrynkillebrew on 1/8/16.
 */
public class DriverConstantFields {

    public String schemaVersion;

    // TODO: are these settable at creation? Make them so if not?  Fields on base AshlarModel class
    // TODO: store directly as fields on DB for use/modification? createdAt already there.
    // public String createdAt;
    // public String lastModified;

    // present these in user form
    // constant fields on Record model in DRF
    // https://github.com/azavea/ashlar/blob/develop/ashlar/models.py

    // TODO: present these fields as dates, but store as properly formatted strings?
    // DatePickerController is available in form builder library
    // set to 'hidden' so can be presented on their own

    @IsHidden(true)
    public String occurredFrom;

    @IsHidden(true)
    public String occuredTo;

    // TODO: how to set/control?
    // Can do GPS with only satellite (and no Internet). Could do offline maps too, maybe with OSMDroid.
    @IsHidden(true)
    public double latitude;
    @IsHidden(true)
    public double longitude;


    // TODO: make enumerations for these two options
    @SerializedName("Weather")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public Weather weather;

    @SerializedName("Light")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public Light light;

    public enum Weather {

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
        private final static Map<String, DriverConstantFields.Weather> CONSTANTS = new HashMap<>();

        static {
            for (DriverConstantFields.Weather c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Weather(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static DriverConstantFields.Weather fromValue(String value) {
            DriverConstantFields.Weather constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }

    public enum Light {

        @SerializedName("Dawn")
        DAWN("dawn"),
        @SerializedName("Day")
        DAY("day"),
        @SerializedName("Dusk")
        DUSK("dusk"),
        @SerializedName("Night")
        NIGHT("night");

        private final String value;
        private final static Map<String, DriverConstantFields.Light> CONSTANTS = new HashMap<>();

        static {
            for (DriverConstantFields.Light c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Light(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static DriverConstantFields.Light fromValue(String value) {
            DriverConstantFields.Light constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }


    /*
    // geocoder fields. do not show to user. TODO: will those ever be useful in android client?
    // could check for Internet, and reverse-geocode if available,
    // attempt reverse geocode right before upload (when there is Internet),
    // or set up server to do that async after upload, if missing.
    @IsHidden(true)
    public String locationText;

    @IsHidden(true)
    public String city;

    @IsHidden(true)
    public String cityDistrict;

    @IsHidden(true)
    public String neighborhood;

    @IsHidden(true)
    public String road;

    @IsHidden(true)
    public String state;
    */
}
