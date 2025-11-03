package com.example.cycleborrowingsystem.models;

public class Cycle {
    private long id;
    private String model;
    private double lat;
    private double lon;

    public Cycle() {}

    public Cycle(long id, String model, double lat, double lon) {
        this.id = id;
        this.model = model;
        this.lat = lat;
        this.lon = lon;
    }

    public Cycle(String model, double lat, double lon) {
        this(0L, model, lat, lon);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}


