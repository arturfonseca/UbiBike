package pt.ulisboa.tecnico.cmov.ubibike.domain;

import java.io.Serializable;

/**
 * Created by ferna on 01/05/2016.
 */
public class GPSCoordinate implements Serializable {

    private double latitude;
    private double longitude;

    public GPSCoordinate(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
