package org.worldbank.transport.driver.staticmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Convenience class for serializing records for upload. Holds point geometry.
 *
 * Created by kathrynkillebrew on 2/5/16.
 */
public class DriverUploadGeom {

    public DriverUploadGeom(double latitude, double longitude) {
        this.coordinates = new double[2];
        this.coordinates[0] = longitude;
        this.coordinates[1] = latitude;
        this.geomType = "Point";
    }

    @SerializedName("type")
    @Expose
    public final String geomType;

    @Expose
    public double[] coordinates;
}
