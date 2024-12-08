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

    public void addSimpleCondition(Sensor sensor, SIGNAL value) {
        this.conditions.add(new SimpleCondition(sensor, value));
    }

    public void addCompositeCondition(Condition left, Condition right, String operator) {
        this.conditions.add(new CompositeCondition(operator, left, right));
    }

    public SignalTransition() {
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
