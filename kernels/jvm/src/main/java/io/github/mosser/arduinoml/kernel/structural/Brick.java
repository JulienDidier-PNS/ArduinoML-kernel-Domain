package io.github.mosser.arduinoml.kernel.structural;

import io.github.mosser.arduinoml.kernel.NamedElement;
import io.github.mosser.arduinoml.kernel.generator.Visitable;

public abstract class Brick implements NamedElement, Visitable {

	public enum BrickType { ANALOG, DIGITAL }

	private String name;
	private int pin;
	private BrickType type;

	public int getPin() {
		return pin;
	}

	public void setPin(int pin) {
		this.pin = pin;
	}

	public BrickType getType() {
		return type;
	}

	public void setType(BrickType type) {
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}