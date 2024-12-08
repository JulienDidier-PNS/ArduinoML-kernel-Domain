package io.github.mosser.arduinoml.kernel.generator;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.behavioral.*;
import io.github.mosser.arduinoml.kernel.structural.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Quick and dirty visitor to support the generation of Wiring code
 */
public class ToWiring extends Visitor<StringBuffer> {
	enum PASS {ONE, TWO}


	public ToWiring() {
		this.result = new StringBuffer();
	}

	private void w(String s) {
		result.append(String.format("%s",s));
	}

	@Override
	public void visit(App app) {
		//first pass, create global vars
		context.put("pass", PASS.ONE);
		w("// Wiring code generated from an ArduinoML model\n");
		w(String.format("// Application name: %s\n", app.getName())+"\n");

		//Explaination of PIN allocation

		// Initialisation de l'allocation des broches
		PinAllocator allocator = new PinAllocator();

		for (Brick brick : app.getBricks()) {
			if (brick.getPin() == -1) { // Vérifie si aucune broche n'est attribuée
				int allocatedPin = allocator.allocatePin(brick);
				brick.setPin(allocatedPin);
			}
		}


		w("// Pin allocation\n");
		w("// Analog inputs: A0, A1, A2, A3, A4, A5\n");
		w("// Analog outputs: 3, 5, 6, 9, 10, 11\n");
		w("// Digital IO: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13\n\n");
		for (Brick brick : app.getBricks()) {
			if(brick instanceof Sensor) {
				w(String.format("//%s is a %s INPUT allocated on PIN %d\n", brick.getName(),brick.getType(), brick.getPin()));
			} else if(brick instanceof Actuator) {
				w(String.format("//%s is an %s OUTPUT allocated on PIN %d\n", brick.getName(),brick.getType(), brick.getPin()));
			}
		}

		w("long debounce = 200;\n");
		w("\nenum STATE {");
		String sep ="";
		for(State state: app.getStates()){
			w(sep);
			state.accept(this);
			sep=", ";
		}
		w("};\n");
		if (app.getInitial() != null) {
			w("STATE currentState = " + app.getInitial().getName()+";\n");
		}

		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}

		//second pass, setup and loop
		context.put("pass",PASS.TWO);
		w("\nvoid setup(){\n");
		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}
		w("}\n");

		w("\nvoid loop() {\n" +
			"\tswitch(currentState){\n");
		for(State state: app.getStates()){
			state.accept(this);
		}
		w("\t}\n" +
			"}");
	}

	@Override
	public void visit(Actuator actuator) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("  pinMode(%d, OUTPUT); // %s [Actuator]\n", actuator.getPin(), actuator.getName()));
			return;
		}
	}


	@Override
	public void visit(Sensor sensor) {
		if(context.get("pass") == PASS.ONE) {
			w(String.format("\nboolean %sBounceGuard = false;\n", sensor.getName()));
			w(String.format("long %sLastDebounceTime = 0;\n", sensor.getName()));
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("  pinMode(%d, INPUT);  // %s [Sensor]\n", sensor.getPin(), sensor.getName()));
			return;
		}
	}

	@Override
	public void visit(State state) {
		if(context.get("pass") == PASS.ONE){
			w(state.getName());
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w("\t\tcase " + state.getName() + ":\n");
			for (Action action : state.getActions()) {
				action.accept(this);
			}

			if (state.getTransition() != null) {
				state.getTransition().accept(this);
				w("\t\tbreak;\n");
			}
			return;
		}

	}

	@Override
	public void visit(SignalTransition transition) {
			if (context.get("pass") == PASS.ONE) {return;}
			if (context.get("pass") == PASS.TWO) {
				// Gestion des conditions multiples
				StringBuilder conditionLogic = new StringBuilder();

				for (SignalTransition.Condition condition : transition.getConditions()) {
					String sensorName = condition.getSensor().getName();
					String signalValue = condition.getValue().toString();

					// Ajouter le code pour gérer le debounce de chaque capteur
					w(String.format("\t\t\t%sBounceGuard = millis() - %sLastDebounceTime > debounce;\n",
							sensorName, sensorName));

					// Construire la logique conditionnelle pour ce capteur
					if (conditionLogic.length() > 0) {
						conditionLogic.append(" && ");
					}
					conditionLogic.append(String.format("(digitalRead(%d) == %s && %sBounceGuard)",
							condition.getSensor().getPin(), signalValue, sensorName));
				}

				// Générer le bloc conditionnel combiné
				w(String.format("\t\t\tif (%s) {\n", conditionLogic.toString()));
				for (SignalTransition.Condition condition : transition.getConditions()) {
					String sensorName = condition.getSensor().getName();
					w(String.format("\t\t\t\t%sLastDebounceTime = millis();\n", sensorName));
				}

				// Transitionner vers l'état suivant
				w(String.format("\t\t\t\tcurrentState = %s;\n", transition.getNext().getName()));
				w("\t\t\t}\n");
			}
	}

	@Override
	public void visit(TimeTransition transition) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			int delayInMS = transition.getDelay();
			w(String.format("\t\t\tdelay(%d);\n", delayInMS));
			w("\t\t\t\tcurrentState = " + transition.getNext().getName() + ";\n");
			w("\t\t\t}\n");
			return;
		}
	}

	@Override
	public void visit(Action action) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("\t\t\tdigitalWrite(%d,%s);\n",action.getActuator().getPin(),action.getValue()));
			return;
		}
	}

}
