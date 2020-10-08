package taintAnalysis;

import soot.Unit;
import soot.Value;

import java.util.HashSet;
import java.util.Set;

public class Taint {

    private Unit unit;
    private Value value;
    private Set<Taint> successors;

    public Taint(Unit unit, Value value) {
        this(unit, value, new HashSet<>());
    }

    public Taint(Unit unit, Value value, Set<Taint> successors) {
        this.unit = unit;
        this.value = value;
        this.successors = successors;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Set<Taint> getSuccessors() {
        return successors;
    }

    public void setSuccessors(Set<Taint> successors) {
        this.successors = successors;
    }

    public void addSuccessor(Taint successor) {
        this.successors.add(successor);
    }

    @Override
    public String toString() {
        return unit.toString() + ":" + value.toString();
    }

}
