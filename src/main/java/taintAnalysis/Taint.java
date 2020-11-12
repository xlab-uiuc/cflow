package taintAnalysis;

import assertion.Assert;
import soot.*;
import soot.jimple.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Taint {

    public enum TransferType {
        None,
        Call,
        Return
    }

    private static final Taint emptyTaint = new Taint(null, null, null);

    private final Value value;
    private final Value base;
    private final SootField field;
    private final Stmt stmt;
    private final SootMethod method;
    private final Set<Taint> successors;
    private final TransferType transferType;
    private boolean isSink = false;

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
     * @param transferType  the type of method context transfer, either TransferType.Call or
     *                      TransferType.Return
     * @return The corresponding globally unique taint object after transfer
     */
    public static Taint getTransferredTaintFor(Taint t, Value v, Stmt stmt, SootMethod method,
                                               Map<Taint, Taint> taintCache, TransferType transferType) {
        Taint newTaint = t.transferTaintTo(v, stmt, method, transferType);
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

        if (v instanceof Immediate) {
            return value.equivTo(v);
        }
        if (v instanceof Expr) {
            return taints((Expr) v);
        }
        if (v instanceof Ref) {
            return taints((Ref) v);
        }

        return false;
    }

    private boolean taints(Expr e) {
        if (e instanceof BinopExpr) {
            BinopExpr binopExpr = (BinopExpr) e;
            Value op1 = binopExpr.getOp1();
            Value op2 = binopExpr.getOp2();
            return taints(op1) || taints(op2);
        }
        if (e instanceof UnopExpr) {
            Value op = ((UnopExpr) e).getOp();
            return taints(op);
        }
        if (e instanceof CastExpr) {
            Value op = ((CastExpr) e).getOp();
            return taints(op);
        }
        if (e instanceof InstanceOfExpr) {
            Value op = ((InstanceOfExpr) e).getOp();
            return taints(op);
        }
        return false;
    }

    private boolean taints(Ref r) {
        if (r instanceof InstanceFieldRef) {
            if (base == null) return false;
            Assert.assertNotNull(field);
            InstanceFieldRef fieldRef = (InstanceFieldRef) r;
            return base.equivTo(fieldRef.getBase()) && field.equals(fieldRef.getField());
        }
        if (r instanceof ArrayRef) {
            return false;
        }
        return false;
    }

    /**
     * associatesWith is field-insensitive.
     */
    public boolean associatesWith(Value v) {
        return (base != null && base.equivTo(v)) || taints(v);
    }

    private Taint(Value value, Stmt stmt, SootMethod method) {
        this(value, stmt, method, new HashSet<>());
    }

    private Taint(Value value, Stmt stmt, SootMethod method, Set<Taint> successors) {
        this.value = value;
        this.stmt = stmt;
        this.method = method;
        this.successors = successors;
        this.transferType = TransferType.None;

        if (value instanceof InstanceFieldRef) {
            InstanceFieldRef fieldRef = (InstanceFieldRef) value;
            this.base = fieldRef.getBase();
            this.field = fieldRef.getField();
        } else {
            this.base = null;
            this.field = null;
        }
    }

    private Taint transferTaintTo(Value v, Stmt stmt, SootMethod method,
                                  TransferType transferType) {
        if (base != null) {
            return new Taint(v, v, field, stmt, method, new HashSet<>(), transferType);
        } else {
            return new Taint(v, null, null, stmt, method, new HashSet<>(), transferType);
        }
    }

    /**
     * Used solely by transferTaintTo.
     */
    private Taint(Value value, Value base, SootField field, Stmt stmt, SootMethod method, Set<Taint> successors,
                  TransferType transferType) {
        this.value = value;
        this.base = base;
        this.field = field;
        this.stmt = stmt;
        this.method = method;
        this.successors = successors;
        this.transferType = transferType;
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

    public TransferType getTransferType() {
        return transferType;
    }

    public boolean isSink() {
        return isSink;
    }

    public void setSink(boolean sink) {
        isSink = sink;
    }

    @Override
    public String toString() {
        if (isEmpty()) return "Empty Taint";

        String str;
        if (base == null) {
            str = value + " in " + stmt + " in method " + method;
        } else {
            Assert.assertNotNull(field);
            str = base + "." + field + " in " + stmt + " in method " + method;
        }
        if (transferType != TransferType.None) {
            str += " " + transferType;
        }
        if (isSink) {
            str += " [Sink]";
        }
        return str;
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
                Objects.equals(method, taint.method) &&
                transferType == taint.transferType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, base, field, stmt, method, transferType);
    }

}
