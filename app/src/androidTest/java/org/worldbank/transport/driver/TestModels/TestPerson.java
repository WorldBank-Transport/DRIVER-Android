
package org.worldbank.transport.driver.TestModels;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jsonschema2pojo.annotations.FieldType;
import org.jsonschema2pojo.annotations.FieldTypes;
import org.jsonschema2pojo.annotations.IsHidden;
import org.jsonschema2pojo.annotations.ReferenceTitlePattern;
import org.jsonschema2pojo.annotations.WatchTarget;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;


/**
 * TestPerson (This is the title)
 * <p>
 * A person involved in the accident
 * 
 */
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "Name",
    "Address",
    "License number",
    "Sex",
    "Age",
    "Injury",
    "Driver error",
    "Alcohol/drugs",
    "Seat belt/helmet",
    "Hospital",
    "Involvement",
    "Vehicle"
})
public class TestPerson {

    @SerializedName("License number")
    @Expose
    @FieldType(FieldTypes.text)
    @Size(min = 6, max = 8)
    public String LicenseNumber;
    @SerializedName("Name")
    @Expose
    @FieldType(FieldTypes.text)
    public String Name;
    @SerializedName("Driver error")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public TestPerson.DriverError DriverError;
    @SerializedName("Age")
    @Expose
    @FieldType(FieldTypes.text)
    public String Age;
    @SerializedName("Vehicle")
    @Expose
    @FieldType(FieldTypes.reference)
    @WatchTarget("Vehicle")
    @ReferenceTitlePattern("{{item.Vehicle type}} {{item.Make}} {{item.Model}}")
    public String Vehicle;
    @SerializedName("Involvement")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public TestPerson.Involvement Involvement;
    @SerializedName("Seat belt/helmet")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public TestPerson.SeatBeltHelmet SeatBeltHelmet;
    @SerializedName("Sex")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public TestPerson.Sex Sex;
    @SerializedName("Alcohol/drugs")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public TestPerson.AlcoholDrugs AlcoholDrugs;
    @SerializedName("Address")
    @Expose
    @FieldType(FieldTypes.text)
    public String Address;
    @SerializedName("Injury")
    @Expose
    @FieldType(FieldTypes.selectlist)
    public TestPerson.Injury Injury;
    @SerializedName("Hospital")
    @Expose
    @FieldType(FieldTypes.text)
    public String Hospital;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("_localId")
    @Expose
    @IsHidden(true)
    @Pattern(regexp = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$")
    @NotNull
    public String LocalId;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(LicenseNumber).append(Name).append(DriverError).append(Age).append(Vehicle).append(Involvement).append(SeatBeltHelmet).append(Sex).append(AlcoholDrugs).append(Address).append(Injury).append(Hospital).append(LocalId).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TestPerson) == false) {
            return false;
        }
        TestPerson rhs = ((TestPerson) other);
        return new EqualsBuilder().append(LicenseNumber, rhs.LicenseNumber).append(Name, rhs.Name).append(DriverError, rhs.DriverError).append(Age, rhs.Age).append(Vehicle, rhs.Vehicle).append(Involvement, rhs.Involvement).append(SeatBeltHelmet, rhs.SeatBeltHelmet).append(Sex, rhs.Sex).append(AlcoholDrugs, rhs.AlcoholDrugs).append(Address, rhs.Address).append(Injury, rhs.Injury).append(Hospital, rhs.Hospital).append(LocalId, rhs.LocalId).isEquals();
    }

    @Generated("org.jsonschema2pojo")
    public static enum AlcoholDrugs {

        @SerializedName("Alcohol suspected")
        ALCOHOL_SUSPECTED("Alcohol suspected"),
        @SerializedName("Drugs suspected")
        DRUGS_SUSPECTED("Drugs suspected");
        private final String value;
        private final static Map<String, TestPerson.AlcoholDrugs> CONSTANTS = new HashMap<String, TestPerson.AlcoholDrugs>();

        static {
            for (TestPerson.AlcoholDrugs c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private AlcoholDrugs(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static TestPerson.AlcoholDrugs fromValue(String value) {
            TestPerson.AlcoholDrugs constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("org.jsonschema2pojo")
    public static enum DriverError {

        @SerializedName("Fatigued/asleep")
        FATIGUED_ASLEEP("Fatigued/asleep"),
        @SerializedName("Inattentive")
        INATTENTIVE("Inattentive"),
        @SerializedName("Too fast")
        TOO_FAST("Too fast"),
        @SerializedName("Too close")
        TOO_CLOSE("Too close"),
        @SerializedName("No signal")
        NO_SIGNAL("No signal"),
        @SerializedName("Bad overtaking")
        BAD_OVERTAKING("Bad overtaking"),
        @SerializedName("Bad turning")
        BAD_TURNING("Bad turning"),
        @SerializedName("Using cell phone")
        USING_CELL_PHONE("Using cell phone");
        private final String value;
        private final static Map<String, TestPerson.DriverError> CONSTANTS = new HashMap<String, TestPerson.DriverError>();

        static {
            for (TestPerson.DriverError c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private DriverError(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static TestPerson.DriverError fromValue(String value) {
            TestPerson.DriverError constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("org.jsonschema2pojo")
    public static enum Injury {

        @SerializedName("Fatal")
        FATAL("Fatal"),
        @SerializedName("Serious")
        SERIOUS("Serious"),
        @SerializedName("Minor")
        MINOR("Minor"),
        @SerializedName("Not injured")
        NOT_INJURED("Not injured");
        private final String value;
        private final static Map<String, TestPerson.Injury> CONSTANTS = new HashMap<String, TestPerson.Injury>();

        static {
            for (TestPerson.Injury c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Injury(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static TestPerson.Injury fromValue(String value) {
            TestPerson.Injury constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("org.jsonschema2pojo")
    public static enum Involvement {

        @SerializedName("Pedestrian")
        PEDESTRIAN("Pedestrian"),
        @SerializedName("Witness")
        WITNESS("Witness"),
        @SerializedName("Passenger")
        PASSENGER("Passenger"),
        @SerializedName("Driver")
        DRIVER("Driver");
        private final String value;
        private final static Map<String, TestPerson.Involvement> CONSTANTS = new HashMap<String, TestPerson.Involvement>();

        static {
            for (TestPerson.Involvement c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Involvement(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static TestPerson.Involvement fromValue(String value) {
            TestPerson.Involvement constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("org.jsonschema2pojo")
    public static enum SeatBeltHelmet {

        @SerializedName("Seat belt/helmet worn")
        SEAT_BELT_HELMET_WORN("Seat belt/helmet worn"),
        @SerializedName("Not worn")
        NOT_WORN("Not worn"),
        @SerializedName("Not worn correctly")
        NOT_WORN_CORRECTLY("Not worn correctly");
        private final String value;
        private final static Map<String, TestPerson.SeatBeltHelmet> CONSTANTS = new HashMap<String, TestPerson.SeatBeltHelmet>();

        static {
            for (TestPerson.SeatBeltHelmet c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private SeatBeltHelmet(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static TestPerson.SeatBeltHelmet fromValue(String value) {
            TestPerson.SeatBeltHelmet constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("org.jsonschema2pojo")
    public static enum Sex {

        @SerializedName("Male")
        MALE("Male"),
        @SerializedName("Female")
        FEMALE("Female");
        private final String value;
        private final static Map<String, TestPerson.Sex> CONSTANTS = new HashMap<String, TestPerson.Sex>();

        static {
            for (TestPerson.Sex c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Sex(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public static TestPerson.Sex fromValue(String value) {
            TestPerson.Sex constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
