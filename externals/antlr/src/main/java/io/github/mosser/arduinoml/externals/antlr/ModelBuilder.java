package io.github.mosser.arduinoml.externals.antlr;

import io.github.mosser.arduinoml.externals.antlr.grammar.ArduinomlBaseListener;
import io.github.mosser.arduinoml.externals.antlr.grammar.ArduinomlParser;
import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.behavioral.Action;
import io.github.mosser.arduinoml.kernel.behavioral.SignalTransition;
import io.github.mosser.arduinoml.kernel.behavioral.State;
import io.github.mosser.arduinoml.kernel.generator.PinAllocator;
import io.github.mosser.arduinoml.kernel.structural.Actuator;
import io.github.mosser.arduinoml.kernel.structural.Brick;
import io.github.mosser.arduinoml.kernel.structural.SIGNAL;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelBuilder extends ArduinomlBaseListener {

    /********************
     ** Business Logic **
     ********************/

    private App theApp = null;
    private boolean built = false;

    public App retrieve() {
        if (built) { return theApp; }
        throw new RuntimeException("Cannot retrieve a model that was not created!");
    }

    /*******************
     ** Symbol tables **
     *******************/

    private Map<String, Sensor>   sensors   = new HashMap<>();
    private Map<String, Actuator> actuators = new HashMap<>();
    private Map<String, State>    states  = new HashMap<>();
    private Map<String, Binding>  bindings  = new HashMap<>();

    private PinAllocator pinAllocator = new PinAllocator();

    private State currentState = null;

    private static class Condition {
        Sensor trigger;
        SIGNAL value;
    }


    public static class CompositeCondition extends Condition {
        private String operator; // "AND", "OR"
        private Condition leftCondition;
        private Condition rightCondition;

        public CompositeCondition(String operator, Condition leftCondition, Condition rightCondition) {
            this.operator = operator;
            this.leftCondition = leftCondition;
            this.rightCondition = rightCondition;
        }

        public String getOperator() {
            return operator;
        }

        public Condition getLeftCondition() {
            return leftCondition;
        }

        public Condition getRightCondition() {
            return rightCondition;
        }
    }


    private static class Binding {
        String to; // Nom de l'état suivant
        List<Condition> conditions = new ArrayList<>();
    }



    /**************************
     ** Listening mechanisms **
     **************************/

    @Override
    public void enterRoot(ArduinomlParser.RootContext ctx) {
        built = false;
        theApp = new App();
    }

    @Override
    public void exitRoot(ArduinomlParser.RootContext ctx) {
        bindings.forEach((key, binding) -> {
            if (!states.containsKey(binding.to)) {
                throw new RuntimeException("Transition in state '" + key + "' points to an undefined state: " + binding.to);
            }
            SignalTransition t = new SignalTransition();
            for (Condition condition : binding.conditions) {
                if (condition instanceof CompositeCondition) {
                    CompositeCondition composite = (CompositeCondition) condition;
                    t.addCondition(composite.getLeftCondition().trigger, composite.getLeftCondition().value);
                    t.addCondition(composite.getRightCondition().trigger, composite.getRightCondition().value);
                } else {
                    // Condition simple
                    if (condition.trigger == null) {
                        throw new RuntimeException("Transition in state '" + key + "' uses an undefined sensor.");
                    }
                    t.addCondition(condition.trigger, condition.value);
                }
            }
            t.setNext(states.get(binding.to));
            states.get(key).setTransition(t);
        });
        this.built = true;
    }



    @Override
    public void enterDeclaration(ArduinomlParser.DeclarationContext ctx) {
        theApp.setName(ctx.name.getText());
    }

    private void validatePort(int port) {
        if (port < 1 || port > 12) {
            throw new RuntimeException("Invalid port number: " + port + ". Must be between 1 and 12.");
        }
    }

    private void validateIdentifier(String name) {
        if (!name.matches("[a-z][a-zA-Z]+")) {
            throw new RuntimeException("Invalid identifier: " + name + ". Must start with a lowercase letter.");
        }
    }

    @Override
    public void enterSensor(ArduinomlParser.SensorContext ctx) {
        String sensorName = ctx.location().id.getText();
        validateIdentifier(sensorName);

        Sensor sensor = new Sensor();
        sensor.setName(ctx.location().id.getText());

        // Déterminer le type de capteur
        String type = ctx.location().type.getText().toUpperCase(); // Ajoutez un champ `type` dans la grammaire
        sensor.setType(Brick.BrickType.valueOf(type));

        int allocatedPin = pinAllocator.allocatePin(sensor);
        sensor.setPin(allocatedPin);
        this.theApp.getBricks().add(sensor);
        sensors.put(sensor.getName(), sensor);
    }

    @Override
    public void enterActuator(ArduinomlParser.ActuatorContext ctx) {
        Actuator actuator = new Actuator();
        actuator.setName(ctx.location().id.getText());

        String type = ctx.location().type.getText().toUpperCase();
        actuator.setType(Brick.BrickType.valueOf(type));

        int allocatedPin = pinAllocator.allocatePin(actuator);
        actuator.setPin(allocatedPin);

        this.theApp.getBricks().add(actuator);
        actuators.put(actuator.getName(), actuator);
    }

    @Override
    public void enterState(ArduinomlParser.StateContext ctx) {
        State local = new State();
        local.setName(ctx.name.getText());
        this.currentState = local;
        this.states.put(local.getName(), local);
    }

    @Override
    public void exitState(ArduinomlParser.StateContext ctx) {
        this.theApp.getStates().add(this.currentState);
        this.currentState = null;
    }

    private Actuator getActuator(String name) {
        Actuator actuator = actuators.get(name);
        if (actuator == null) {
            throw new RuntimeException("Undefined actuator: " + name);
        }
        return actuator;
    }

    @Override
    public void enterAction(ArduinomlParser.ActionContext ctx) {
        Action action = new Action();
        action.setActuator(getActuator(ctx.receiver.getText()));
        action.setValue(SIGNAL.valueOf(ctx.value.getText()));
        currentState.getActions().add(action);
    }

    @Override
    public void enterTransition(ArduinomlParser.TransitionContext ctx) {
        Binding toBeResolvedLater = new Binding();
        toBeResolvedLater.to = ctx.next.getText();

        System.out.println("Transition to " + toBeResolvedLater.to);

        // Gérer les conditions dans la transition
        ArduinomlParser.ConditionContext conditionCtx = ctx.condition();
        Condition rootCondition = parseCondition(conditionCtx);
        toBeResolvedLater.conditions.add(rootCondition);

        bindings.put(currentState.getName(), toBeResolvedLater);
    }

    private Condition parseCondition(ArduinomlParser.ConditionContext ctx) {
        if (ctx.sensorCondition() != null) {
            return parseSensorCondition(ctx.sensorCondition());
        } else if (ctx.unaryCondition() != null) {
            return parseUnaryCondition(ctx.unaryCondition());
        } else if (ctx.binaryCondition() != null) {
            return parseBinaryCondition(ctx.binaryCondition());
        }
        throw new RuntimeException("Unknown condition type in context: " + ctx.getText());
    }

    private Condition parseSensorCondition(ArduinomlParser.SensorConditionContext ctx) {
        String sensorName = ctx.receiver.getText();
        Sensor sensor = sensors.get(sensorName);
        if (sensor == null) {
            throw new RuntimeException("Undefined sensor: " + sensorName);
        }
        Condition condition = new Condition();
        condition.trigger = sensor;
        condition.value = SIGNAL.valueOf(ctx.value.getText());
        return condition;
    }

    private Condition parseUnaryCondition(ArduinomlParser.UnaryConditionContext ctx) {
        String operator = ctx.operator.getText();
        if ("NOT".equals(operator)) {
            System.out.println("NOT operator");
            ArduinomlParser.ConditionContext conditionCtx = ctx.condition();
            Condition condition = parseCondition(conditionCtx);
            condition = invertCondition(condition);
            return condition;
        }
        throw new RuntimeException("Unsupported unary operator: " + operator);
    }

    private Condition invertCondition(Condition condition) {
        if(condition instanceof CompositeCondition) {
            ((CompositeCondition) condition).leftCondition = invertCondition(((CompositeCondition) condition).leftCondition);
            ((CompositeCondition) condition).rightCondition = invertCondition(((CompositeCondition) condition).rightCondition);
        }
        else {
            if (condition.value == SIGNAL.HIGH) {condition.value = SIGNAL.LOW;}
            else {condition.value = SIGNAL.HIGH;}
        }
        return condition;
    }

    private CompositeCondition parseBinaryCondition(ArduinomlParser.BinaryConditionContext ctx) {
        String operator = ctx.operator.getText();
        Condition left = parseCondition(ctx.left);
        Condition right = parseCondition(ctx.right);
        return new CompositeCondition(operator, left, right);
    }

    @Override
    public void enterInitial(ArduinomlParser.InitialContext ctx) {
        this.theApp.setInitial(this.currentState);
    }

}

