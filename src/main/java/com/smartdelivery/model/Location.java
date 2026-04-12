package com.smartdelivery.model;

public class Location {
    private final double lat;
    private final double lon;
    private final String name;

    public Location(double lat,double lon,String name) {
        this.lat=lat;
        this.lon=lon;
        this.name=name;
    }
    //distance in km
    public double distanceTo(Location other) {
        final int R = 6371;//earth R
        double dLat = Math.toRadians(other.lat-this.lat);
        double dLon = Math.toRadians(other.lon-this.lon);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(this.lat))*Math.cos(Math.toRadians(other.lat))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }

    public double getLatitude() {
        return lat;
    }
    public double getLongitude() {
        return lon;
    }
    public String getName() {
        return name;
    }
    @Override
    public String toString() {
        return name + "(" +lat+","+lon+ ")";
    }
}