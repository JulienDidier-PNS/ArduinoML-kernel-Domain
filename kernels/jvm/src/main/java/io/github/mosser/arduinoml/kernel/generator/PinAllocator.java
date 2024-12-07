package io.github.mosser.arduinoml.kernel.generator;

import io.github.mosser.arduinoml.kernel.structural.Brick;
import io.github.mosser.arduinoml.kernel.structural.Sensor;
import io.github.mosser.arduinoml.kernel.structural.Actuator;

import java.util.ArrayList;
import java.util.List;

public class PinAllocator {
    private List<Integer> availableAnalogInputs;
    private List<Integer> availableDigitalIO;
    private List<Integer> reservedPins;

    public PinAllocator() {
        // Définir les broches disponibles selon les contraintes matérielles
        availableAnalogInputs = new ArrayList<>(List.of(14, 15, 16, 17, 18, 19));
        availableDigitalIO = new ArrayList<>(List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        reservedPins = new ArrayList<>();
    }

    public int allocatePin(Brick brick) {
        if (brick instanceof Sensor) {
            return allocateAnalogInput();
        } else if (brick instanceof Actuator) {
            return allocateDigitalIO();
        } else {
            throw new IllegalArgumentException("Unknown brick type");
        }
    }

    private int allocateAnalogInput() {
        if (availableAnalogInputs.isEmpty()) {
            throw new RuntimeException("No available analog inputs");
        }
        int pin = availableAnalogInputs.get(0);
        availableAnalogInputs.remove(0);
        reservedPins.add(pin);
        return pin;
    }

    private int allocateDigitalIO() {
        if (availableDigitalIO.isEmpty()) {
            throw new RuntimeException("No available digital IO pins");
        }
        int pin = availableDigitalIO.get(0);
        availableDigitalIO.remove(0);
        reservedPins.add(pin);
        return pin;
    }
}
