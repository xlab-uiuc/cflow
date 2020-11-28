package taintAnalysis.sourceSinkManager;

import configInterface.ConfigInterface;
import soot.*;
import soot.jimple.*;

public class SourceSinkManager implements ISourceSinkManager {

    private ConfigInterface interfaces;

    public SourceSinkManager(ConfigInterface interfaces) {
        this.interfaces = interfaces;
    }

    @Override
    public boolean isSource(Stmt stmt) {
        // We only support method calls
        if (!stmt.containsInvokeExpr())
            return false;

        // Check for predefined getter patterns
        InvokeExpr expr = stmt.getInvokeExpr();
        if (interfaces.isGetter(expr))
            return true;

        // nothing found
        return false;
    }

    @Override
    public boolean isSink(Stmt stmt)  {
        // external lib calls considered as sink
        if (stmt.containsInvokeExpr()) {
            InvokeExpr expr = stmt.getInvokeExpr();

            // ignore logger calls
            if (isLogger(expr)) {
                return false;
            }

            // ignore certain java lib calls
            if (isExcludedJavaLangCall(expr)) {
                return false;
            }

            SootMethod callee = expr.getMethod();
            if (!callee.getDeclaringClass().isApplicationClass()) {
                return true;
            }
        }

        // nothing found
        return false;
    }

    protected boolean isLogger(InvokeExpr iexpr) {
        if (iexpr instanceof InstanceInvokeExpr) {
            Value base = ((InstanceInvokeExpr) iexpr).getBase();
            if (base.getType() instanceof RefType) {
                RefType rty = (RefType) base.getType();
                if (rty.getClassName().contains("Logger")) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isExcludedJavaLangCall(InvokeExpr iexpr) {
        SootClass sootClass = iexpr.getMethod().getDeclaringClass();
        String className = sootClass.getName();
        String packageName = sootClass.getPackageName();
        if (!packageName.startsWith("java")) {
            return false;
        }
        if (className.equals("java.io.PrintStream") ||
                className.equals("java.lang.Boolean") ||
                className.equals("java.lang.Byte") ||
                className.equals("java.lang.Character") ||
                className.equals("java.lang.Class") ||
                className.equals("java.lang.Double") ||
                className.equals("java.lang.Float") ||
                className.equals("java.lang.Integer") ||
                className.equals("java.lang.Long") ||
                className.equals("java.lang.Math") ||
                className.equals("java.lang.Number") ||
                className.equals("java.lang.Object") ||
                className.equals("java.lang.String") ||
                className.equals("java.lang.StringBuffer") ||
                className.equals("java.lang.StringBuilder") ||
                packageName.equals("java.lang.ref") ||
                packageName.equals("java.lang.reflect") ||
                packageName.equals("java.math") ||
                packageName.equals("java.util")) {
            return true;
        }
        return false;
    }

}
