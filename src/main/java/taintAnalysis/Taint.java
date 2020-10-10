package taintAnalysis;

import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;

import java.util.HashSet;
import java.util.Set;

public class Taint {

    private final Value value;
    private final Value base;
    private final SootField field;
    private final Stmt stmt;
    private final SootMethod method;
    private final Set<Taint> successors;

    public Taint(Value value, Stmt stmt) {
        this(value, stmt, null, new HashSet<>());
    }

    public Taint(Value value, Stmt stmt, SootMethod method) {
        this(value, stmt, method, new HashSet<>());
    }

    public Taint(Value value, Stmt stmt, SootMethod method, Set<Taint> successors) {
        this.value = value;
        this.stmt = stmt;
        this.method = method;
        this.successors = successors;

        if (value instanceof InstanceFieldRef) {
            InstanceFieldRef fieldRef = (InstanceFieldRef) value;
            this.base = fieldRef.getBase();
            this.field = fieldRef.getField();
        } else {
            this.base = null;
            this.field = null;
        }
    }

    public Value getValue() {
        return value;
    }

    public Stmt getStmt() {
        return stmt;
    }

    public Value getBase() {
        return base;
    }

    public SootField getField() {
        return field;
    }

    public SootMethod getMethod() {
        return method;
    }

    public Set<Taint> getSuccessors() {
        return successors;
    }

    public void addSuccessor(Taint successor) {
        this.successors.add(successor);
    }

    public boolean taints(Value v) {
        if (v instanceof InstanceFieldRef) {
            if (base == null || field == null) return false;
            InstanceFieldRef fieldRef = (InstanceFieldRef) v;
            return base.equivTo(fieldRef.getBase()) && field.equals(fieldRef.getField());
        }
        return value.equivTo(v);
    }

    @Override
    public String toString() {
        return value + " in " + stmt + " in method " + method;
    }

}
