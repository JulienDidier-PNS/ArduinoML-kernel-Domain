package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitor;
import io.github.mosser.arduinoml.kernel.structural.SIGNAL;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

import java.util.ArrayList;
import java.util.List;

public class SignalTransition extends Transition {

    private final List<Condition> conditions = new ArrayList<>();

    public List<Condition> getConditions() {
        return conditions;
    }

    public void addCondition(Sensor sensor, SIGNAL value) {
        this.conditions.add(new Condition(sensor, value));
    }

    public static class Condition {
        private Sensor sensor;
        private SIGNAL value;

        public Condition(Sensor sensor, SIGNAL value) {
            this.sensor = sensor;
            this.value = value;
        }

        public Sensor getSensor() {
            return sensor;
        }

        public SIGNAL getValue() {
            return value;
        }
    }
    public SignalTransition() {
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
