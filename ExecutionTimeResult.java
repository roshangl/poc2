package map.poc2.model;

import java.io.Serializable;

public class ExecutionTimeResult implements Serializable {

    private double durationSec;

    public double getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(double durationSec) {
        this.durationSec = durationSec;
    }
}
