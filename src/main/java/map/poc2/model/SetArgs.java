package map.poc2.model;

import java.io.Serializable;

public class SetArgs implements Serializable {

    private String customerChoice;
    private String cluster;
    private String week;
    private String measure;
    private double value;

    public String getCustomerChoice() {
        return customerChoice;
    }

    public void setCustomerChoice(String customerChoice) {
        this.customerChoice = customerChoice;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
