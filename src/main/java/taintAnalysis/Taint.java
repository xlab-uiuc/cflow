package taintAnalysis;

import assertion.Assert;
import soot.*;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Taint {

    private static final Taint emptyTaint = new Taint(null, null, null);

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

    private Taint(Value value, Value base, SootField field, Stmt stmt, SootMethod method, Set<Taint> successors) {
        this.value = value;
        this.base = base;
        this.field = field;
        this.stmt = stmt;
        this.method = method;
        this.successors = successors;
    }

    public static Taint getEmptyTaint() {
        return emptyTaint;
    }

    public boolean isEmpty() {
        return value == null;
    }

    // taints is field-sensitive
    public boolean taints(Value v) {
        // Empty taint doesn't taint anything
        if (isEmpty()) return false;

        // Match exact field if v is a ref to instance field
        if (v instanceof InstanceFieldRef) {
            if (base == null) return false;
            Assert.assertNotNull(field);
            InstanceFieldRef fieldRef = (InstanceFieldRef) v;
            return base.equivTo(fieldRef.getBase()) && field.equals(fieldRef.getField());
        }

        // If curr taint is on an exact field and v is not a ref to instance field, return false
        if (base != null) {
            return false;
        }

        // Otherwise, compare the value
        return value.equivTo(v);
    }

    // associatesWith is field-insensitive
    public boolean associatesWith(Value v) {
        return taints(v) || (base != null && base.equivTo(v));
    }

    public Taint transferTaintTo(Value v, Stmt stmt, SootMethod method) {
        if (base != null) {
            return new Taint(v, v, field, stmt, method, new HashSet<>());
        } else {
            return new Taint(v, null, null, stmt, method, new HashSet<>());
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

    @Override
    public String toString() {
        if (isEmpty()) return "Empty Taint";

        if (base == null) {
            return value + " in " + stmt + " in method " + method;
        } else {
            Assert.assertNotNull(field);
            return base + "." + field + " in " + stmt + " in method " + method;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Taint taint = (Taint) o;
        return Objects.equals(value, taint.value) &&
                Objects.equals(base, taint.base) &&
                Objects.equals(field, taint.field) &&
                Objects.equals(stmt, taint.stmt) &&
                Objects.equals(method, taint.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, base, field, stmt, method);
    }

}
