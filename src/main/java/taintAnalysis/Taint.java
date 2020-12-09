package taintAnalysis;

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

    private final Value plainValue;
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
     * Gets a globally unique taint object for a given pair of value and its statement context.
     * The whole value is tainted, whose taint is transferred from another taint object (can be null).
     *
     * @param t             the taint from which to transfer (null when a new taint is created)
     * @param v             the value which the taint is on
     * @param stmt          the statement context of the taint
     * @param method        the method context of the taint
     * @param taintCache    the taint cache of the method into which the taint is transferred,
     *                      used to ensure global uniqueness
     * @return The corresponding globally unique taint object
     */
    public static Taint getTaintFor(Taint t, Value v, Stmt stmt, SootMethod method,
                                    Map<Taint, Taint> taintCache) {
        Taint newTaint = new Taint(v, stmt, method);
        if (taintCache.containsKey(newTaint)) {
            newTaint = taintCache.get(newTaint);
        } else {
            taintCache.put(newTaint, newTaint);
        }
        if (t != null) {
            t.addSuccessor(newTaint);
        }
        return newTaint;
    }

    /**
     * Gets a globally unique taint object whose taint is transferred from another taint object
     * with method context transfer type None.
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
        return getTransferredTaintFor(t, v, stmt, method, taintCache, TransferType.None);
    }

    /**
     * Gets a globally unique taint object whose taint is transferred from another taint object,
     * the method context transfer type is to indicate taint transfer along call/return edges.
     *
     * @param t             the taint from which to transfer
     * @param v             the value which the taint is on
     * @param stmt          the statement context of the taint
     * @param method        the method context of the taint
     * @param taintCache    the taint cache of the method into which the taint is transferred,
     *                      used to ensure global uniqueness
     * @param transferType  the type of method context transfer
     * @return The corresponding globally unique taint object after transfer
     */
    public static Taint getTransferredTaintFor(Taint t, Value v, Stmt stmt, SootMethod method,
                                               Map<Taint, Taint> taintCache, TransferType transferType) {
        Taint newTaint = new Taint(t, v, stmt, method, transferType);
        if (taintCache.containsKey(newTaint)) {
            newTaint = taintCache.get(newTaint);
        } else {
            taintCache.put(newTaint, newTaint);
        }
        t.addSuccessor(newTaint);
        return newTaint;
    }

    public boolean taints(Value v) {
        // Empty taint doesn't taint anything
        if (isEmpty()) return false;

        // Taint on V must taint V, taint on B.* also taints B
        if (plainValue.equivTo(v)) return true;

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
            InstanceFieldRef fieldRef = (InstanceFieldRef) r;
            if (field == null) return false;
            return plainValue.equivTo(fieldRef.getBase()) && field.equals(fieldRef.getField());
        }
        if (r instanceof ArrayRef) {
            ArrayRef arrayRef = (ArrayRef) r;
            return plainValue.equivTo(arrayRef.getBase());
        }
        // static field ref not supported
        return false;
    }

    private Taint(Value value, Stmt stmt, SootMethod method) {
        this(null, value, stmt, method, TransferType.None);
    }

    private Taint(Taint transferFrom, Value value, Stmt stmt, SootMethod method, TransferType transferType) {
        this.stmt = stmt;
        this.method = method;
        this.successors = new HashSet<>();
        this.transferType = transferType;

        if (value instanceof Ref) {
            // if value is of ref type, ignore the taint from which to transfer
            if (value instanceof InstanceFieldRef) {
                InstanceFieldRef fieldRef = (InstanceFieldRef) value;
                this.plainValue = fieldRef.getBase();
                this.field = fieldRef.getField();
            } else {
                // array ref and static field ref is not currently supported,
                // just taint the entire value
                this.plainValue = value;
                this.field = null;
            }
        } else if (transferFrom != null) {
            // for a non-ref object-typed value, transfer taint from t
            this.plainValue = value;
            this.field = transferFrom.getField();
        } else {
            this.plainValue = value;
            this.field = null;
        }
    }

    public boolean isEmpty() {
        return plainValue == null;
    }

    public Value getPlainValue() {
        return plainValue;
    }

    public SootField getField() {
        return field;
    }

    public Stmt getStmt() {
        return stmt;
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

    public void setSink() {
        isSink = true;
    }

    @Override
    public String toString() {
        if (isEmpty()) return "Empty Taint";

        String str = "";
        if (transferType != TransferType.None) {
            str += "[" + transferType + "] ";
        }
        str += plainValue + (field != null ? "." + field : "") +
                " in " + stmt + " in method " + method;

        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Taint taint = (Taint) o;
        return Objects.equals(plainValue, taint.plainValue) &&
                Objects.equals(field, taint.field) &&
                Objects.equals(stmt, taint.stmt) &&
                Objects.equals(method, taint.method) &&
                transferType == taint.transferType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(plainValue, field, stmt, method, transferType);
    }

}
