package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.structural.SIGNAL;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

public class SimpleCondition extends Condition {
    private Sensor sensor;
    private SIGNAL value;

    public SimpleCondition(Sensor sensor, SIGNAL value) {
        this.sensor = sensor;
        this.value = value;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public SIGNAL getValue() {
        return value;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public void setValue(SIGNAL value) {
        this.value = value;
    }
}

