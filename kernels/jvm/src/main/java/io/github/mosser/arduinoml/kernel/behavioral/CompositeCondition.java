package io.github.mosser.arduinoml.kernel.behavioral;

public class CompositeCondition extends Condition {
    private String operator; // "AND" ou "OR"
    private Condition left;
    private Condition right;

    public CompositeCondition(String operator, Condition left, Condition right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public String getOperator() {
        return operator;
    }

    public Condition getLeft() {
        return left;
    }

    public Condition getRight() {
        return right;
    }

    public void setLeft(Condition left) {
        this.left = left;
    }
    public void setRight(Condition right) {
        this.right = right;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}