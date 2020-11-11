package taintAnalysis;

import assertion.Assert;
import soot.*;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;

import java.util.HashSet;
import java.util.Map;
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
    private final boolean methodContextSwitched;

    public static Taint getEmptyTaint() {
        return emptyTaint;
    }

    /**
     * Gets a globally unique taint object for a given pair of value and its statement context
     *
     * @param v             the value which the taint is on
     * @param stmt          the statement context of the taint
     * @param method        the method context of the taint
     * @param taintCache    the taint cache of the method into which the taint is transferred,
     *                      used to ensure global uniqueness
     * @return The corresponding globally unique taint object
     */
    public static Taint getTaintFor(Value v, Stmt stmt, SootMethod method,
                              Map<Taint, Taint> taintCache) {
        Taint newTaint = new Taint(v, stmt, method);
        if (taintCache.containsKey(newTaint)) {
            newTaint = taintCache.get(newTaint);
        } else {
            taintCache.put(newTaint, newTaint);
        }
        return newTaint;
    }

    /**
     * Gets a globally unique taint object for a value in the callee/caller whose taint is
     * transferred from a taint object in the caller/callee along call/return edges in the ICFG.
     *
     * @param t             the taint from which to transfer
     * @param v             the value which the taint is on
     * @param stmt          the statement context of the taint
     * @param method        the method context of the taint
     * @param taintCache    the taint cache of the method into which the taint is transferred,
     *                      used to ensure global uniqueness
     * @return The corresponding globally unique taint object after transfer
     */
    public static Taint getTransferredTaintFor(Taint t, Value v, Stmt stmt, SootMethod method,
                                         Map<Taint, Taint> taintCache) {
        Taint newTaint = t.transferTaintTo(v, stmt, method);
        if (taintCache.containsKey(newTaint)) {
            newTaint = taintCache.get(newTaint);
        } else {
            taintCache.put(newTaint, newTaint);
        }
        return newTaint;
    }

    /**
     * taints is field-sensitive.
     */
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

    /**
     * associatesWith is field-insensitive.
     */
    public boolean associatesWith(Value v) {
        return taints(v) || (base != null && base.equivTo(v));
    }

    private Taint(Value value, Stmt stmt, SootMethod method) {
        this(value, stmt, method, new HashSet<>());
    }

    private Taint(Value value, Stmt stmt, SootMethod method, Set<Taint> successors) {
        this.value = value;
        this.stmt = stmt;
        this.method = method;
        this.successors = successors;
        this.methodContextSwitched = false;

        if (value instanceof InstanceFieldRef) {
            InstanceFieldRef fieldRef = (InstanceFieldRef) value;
            this.base = fieldRef.getBase();
            this.field = fieldRef.getField();
        } else {
            this.base = null;
            this.field = null;
        }
    }

    private Taint transferTaintTo(Value v, Stmt stmt, SootMethod method) {
        if (base != null) {
            return new Taint(v, v, field, stmt, method, new HashSet<>());
        } else {
            return new Taint(v, null, null, stmt, method, new HashSet<>());
        }
    }

    /**
     * Used solely by transferTaintTo.
     */
    private Taint(Value value, Value base, SootField field, Stmt stmt, SootMethod method, Set<Taint> successors) {
        this.value = value;
        this.base = base;
        this.field = field;
        this.stmt = stmt;
        this.method = method;
        this.successors = successors;
        this.methodContextSwitched = true;
    }

    public boolean isEmpty() {
        return value == null;
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

    public boolean isMethodContextSwitched() {
        return methodContextSwitched;
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
