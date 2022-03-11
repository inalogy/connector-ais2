package io.artin.idm.connector.ais2;

@SuppressWarnings("ClassCanBeRecord")
public class Interval {

    /** Lower bound of records to be returned. Inclusive. */
    final Integer from;

    /** Upper bound of records to be returned. Inclusive. */
    final Integer to;

    public Interval(Integer from, Integer to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return from + "-" + to;
    }
}
