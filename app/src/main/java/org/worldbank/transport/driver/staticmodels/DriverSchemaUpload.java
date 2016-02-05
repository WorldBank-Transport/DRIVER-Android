package org.worldbank.transport.driver.staticmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.worldbank.transport.driver.models.DriverSchema;

/**
 * Convenience class for serializing records for upload, with their associated constant fields and metadata.
 *
 * Created by kathrynkillebrew on 2/5/16.
 */
public class DriverSchemaUpload {

    @Expose
    @SerializedName("schema")
    public String schemaVersion;

    @Expose
    @SerializedName("data")
    public DriverSchema driverData;

    @Expose
    @SerializedName("weather")
    public String driverWeather;

    @Expose
    @SerializedName("light")
    public String driverLight;

    @Expose
    public DriverUploadGeom geom;

    @Expose
    @SerializedName("occurred_from")
    public String occurredFrom;

    @Expose
    @SerializedName("occurred_to")
    public String occurredTo;

    @Expose
    @SerializedName("created")
    public String createdAt;

    @SerializedName("modified")
    public String modifiedAt;

}
