package com.inalogy.midpoint.connector.ais2;

public class Ais2Filter {

    public String byId = null;

    public Interval byInterval = null;

    @Override
    public String toString() {
        return "Ais2Filter {" +
                "byId= " + byId +
                "byInterval= " + byInterval +
                "}";
    }

    public boolean isEmpty() {
        return byId == null && byInterval == null;
    }
}
