package map.poc2.model;

import java.io.Serializable;

public class IntializeArgs implements Serializable {

    private int customerChoice;
    private int cluster;
    private int week;
    private int measure;

    public int getCustomerChoice() {
        return customerChoice;
    }

    public void setCustomerChoice(int customerChoice) {
        this.customerChoice = customerChoice;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public int getMeasure() {
        return measure;
    }

    public void setMeasure(int measure) {
        this.measure = measure;
    }
}
