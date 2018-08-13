package map.poc2.model;

import java.io.Serializable;

public class GetResult implements Serializable {

    private double[][][][] value;
    private double durationSec;

    public double[][][][] getValue() {
        return value;
    }

    public void setValue(double[][][][] value) {
        this.value = value;
    }

    public double getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(double durationSec) {
        this.durationSec = durationSec;
    }
}
