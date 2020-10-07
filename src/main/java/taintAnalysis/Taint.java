package taintAnalysis;

import soot.Unit;
import soot.Value;

import java.util.HashSet;
import java.util.Set;

public class Taint {

    private Unit source;
    private Value value;
    private Set<Taint> successors;

    public Taint(Unit source, Value value) {
        this(source, value, new HashSet<>());
    }

    public Taint(Unit source, Value value, Set<Taint> successors) {
        this.source = source;
        this.value = value;
        this.successors = successors;
    }

    public Unit getSource() {
        return source;
    }

    public void setSource(Unit source) {
        this.source = source;
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
        return source.toString() + ":" + value.toString();
    }

}
