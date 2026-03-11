package com.smartdelivery.model;

public class Location {
    private final double latitude;
    private final double longitude;
    private final String name;

    public Location(double latitude, double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }
    public double distanceTo(Location other) {
        final int R = 6371; // Earth radius km
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(this.latitude)) *
                        Math.cos(Math.toRadians(other.latitude)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
    public double getLatitude()  { return latitude; }
    public double getLongitude() { return longitude; }
    public String getName()      { return name; }




    @Override
    public String toString() {
        return name + "(" + latitude + "," + longitude + ")";
    }
}
