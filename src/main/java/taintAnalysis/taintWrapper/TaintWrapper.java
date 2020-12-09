package taintAnalysis.taintWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import taintAnalysis.Taint;

import static assertion.Assert.assertNotNull;
import static assertion.Assert.assertTrue;


/**
 * Five lists of methods are passed which contain signatures of instance methods
 * with different tainting behavior.
 *
 * 1. TaintBoth:    taint both the base object and the return value
 *                  if the base object is tainted or one of the parameter is tainted
 * 2. TaintReturn:  taint only the return value
 *                  if the base object is tainted or one of the parameter is tainted
 * 3. TaintBase:    taint only the base object
 *                  if the base object is tainted or one of the parameter is tainted
 * 4. KillTaint:    kill the taint if the base object is tainted
 * 5. Exclude:      excluded, do nothing
 */
public class TaintWrapper implements ITaintWrapper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<String> taintBothList;
    private final Set<String> taintReturnList;
    private final Set<String> taintBaseList;
    private final Set<String> excludeList;
    private final Set<String> killList;

    /**
     * The possible effects this taint wrapper can have on a method invocation
     */
    private enum MethodWrapType {
        /**
         * This method can create a new taint for both the base object and the return value
         */
        TaintBoth,
        /**
         * This method can create a new taint only for the return value
         */
        TaintReturn,
        /**
         * This method can create a new taint only for the base object
         */
        TaintBase,
        /**
         * This method can kill a taint
         */
        KillTaint,
        /**
         * This method has not been named in the taint wrapper configuration
         */
        Exclude,
        /**
         * This method has not been named in the taint wrapper configuration
         */
        NotRegistered
    }

    public TaintWrapper(Set<String> taintBothList, Set<String> taintReturnList, Set<String> taintBaseList,
                        Set<String> excludeList, Set<String> killList) {
        this.taintBothList = taintBothList;
        this.taintReturnList = taintReturnList;
        this.taintBaseList = taintBaseList;
        this.excludeList = excludeList;
        this.killList = killList;
    }

    public static TaintWrapper getDefault() throws IOException {
        return new TaintWrapper("TaintWrapperSource.txt");
    }

    public TaintWrapper(String f) throws IOException {
        Reader reader = new FileReader(new File(f).getAbsoluteFile());
        BufferedReader bufReader = new BufferedReader(reader);
        try {
            String line = bufReader.readLine();
            this.taintBothList = new HashSet<>();
            this.taintReturnList = new HashSet<>();
            this.taintBaseList = new HashSet<>();
            this.excludeList = new HashSet<>();
            this.killList = new HashSet<>();
            while (line != null) {
                if (!line.isEmpty() && !line.startsWith("%")) {
                    if (line.startsWith("~"))
                        excludeList.add(line.substring(1));
                    else if (line.startsWith("-"))
                        killList.add(line.substring(1));
                    else if (line.startsWith("r"))
                        taintReturnList.add(line.substring(1));
                    else if (line.startsWith("b"))
                        taintBaseList.add(line.substring(1));
                    else
                        taintBothList.add(line);
                }
                line = bufReader.readLine();
            }
            logger.info("Loaded wrapper entries for {} taint-both methods, {} taint-return methods, " +
                            "{} taint-base methods, {} kill-taint methods, and {} exclusions",
                    taintBothList.size(), taintReturnList.size(), taintBaseList.size(),
                    excludeList.size(), killList.size());
        } finally {
            bufReader.close();
        }
    }

    public TaintWrapper(TaintWrapper taintWrapper) {
        this(taintWrapper.taintBothList, taintWrapper.taintReturnList, taintWrapper.taintBaseList,
                taintWrapper.excludeList, taintWrapper.killList);
    }

    @Override
    public void genTaintsForMethodInternal(Set<Taint> in, Stmt stmt, SootMethod caller,
                                           Set<Taint> killSet, Set<Taint> genSet,
                                           Map<Taint, Taint> taintCache) {
        assertTrue(stmt.containsInvokeExpr());
        InvokeExpr invoke = stmt.getInvokeExpr();
        SootMethod callee = invoke.getMethod();
        assertNotNull(callee);

        // Do not provide models for application classes
        if (callee.getDeclaringClass().isApplicationClass())
            return;

        MethodWrapType wrapType = getMethodWrapType(callee);
        if (wrapType == MethodWrapType.Exclude)
            return;

        // Get the base object of this invocation in caller and the corresponding this object in callee (if exists)
        Value base = null;
        if (invoke instanceof InstanceInvokeExpr) {
            base = ((InstanceInvokeExpr) invoke).getBase();
        }

        // Get the retVal of this invocation in caller (if applies)
        Value retVal = null;
        if (stmt instanceof AssignStmt) {
            retVal = ((AssignStmt) stmt).getLeftOp();
        }

        for (Taint t : in) {
            boolean baseTainted = false;
            boolean paramTainted = false;

            // Process base object
            if (base != null && t.taints(base)) {
                if (wrapType == MethodWrapType.KillTaint) {
                    killSet.add(t);
                    continue;
                }
                baseTainted = true;
            }

            // Process parameters
            for (int i = 0; i < invoke.getArgCount(); i++) {
                Value arg = invoke.getArg(i);
                if (t.taints(arg)) {
                    paramTainted = true;
                }
            }

            if (baseTainted || paramTainted) {
                // Taint base
                if (base != null &&
                        (wrapType == MethodWrapType.TaintBoth || wrapType == MethodWrapType.TaintBase)) {
                    if (t.taints(base)) {
                        killSet.add(t);
                    }
                    Taint newTaint = Taint.getTaintFor(t, base, stmt, caller, taintCache);
                    genSet.add(newTaint);
                }

                // Taint return val (Note that if base is tainted, we also taint the ret val)
                if (retVal != null && (baseTainted ||
                        wrapType == MethodWrapType.TaintBoth ||
                        wrapType == MethodWrapType.TaintReturn)) {
                    Taint newTaint = Taint.getTaintFor(t, retVal, stmt, caller, taintCache);
                    genSet.add(newTaint);
                }
            }
        }
    }

    @Override
    public boolean supportsCallee(SootMethod method) {
        if (!method.getDeclaringClass().isApplicationClass())
            return true;
        return false;
    }

    /**
     * Gets the type of action the taint wrapper shall perform on a given method
     *
     * @param method      The method to look for
     * @return The type of action to be performed on the given method
     */
    private MethodWrapType getMethodWrapType(SootMethod method) {
        String sig = method.getSignature();
        if (taintBothList.contains(sig))
            return MethodWrapType.TaintBoth;
        if (taintReturnList.contains(sig))
            return MethodWrapType.TaintReturn;
        if (taintBaseList.contains(sig))
            return MethodWrapType.TaintBase;
        if (excludeList.contains(sig))
            return MethodWrapType.Exclude;
        if (killList.contains(sig))
            return MethodWrapType.KillTaint;
        return MethodWrapType.NotRegistered;
    }

}
