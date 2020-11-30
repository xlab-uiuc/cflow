package taintAnalysis.taintWrapper;

import soot.SootMethod;
import soot.jimple.Stmt;
import taintAnalysis.Taint;

import java.util.Map;
import java.util.Set;


public interface ITaintWrapper {

    /**
     * Checks an invocation statement for black-box taint propagation. This allows
     * the wrapper to artificially propagate taints over method invocations without
     * requiring the analysis to look inside the method.
     * @param in            The in-set of taints before the invocation statement
     * @param stmt          The invocation statement which to check for black-box taint propagation
     * @param caller        The caller method of the invocation statement
     * @param killSet       The kill sets of the invocation statement computed with the provided in-set
     * @param genSet        The gen sets of the invocation statement computed with the provided in-set
     * @param taintCache    The taint cache of the caller method,
     *                       used to ensure global uniqueness of generated taint objects
     */
    void genTaintsForMethodInternal(Set<Taint> in, Stmt stmt, SootMethod caller,
                                    Set<Taint> killSet, Set<Taint> genSet,
                                    Map<Taint, Taint> taintCache);

    /**
     * Checks whether this taint wrapper can in general produce artificial taints
     * for the given callee. If an implementation returns "false" for a callee,
     * all call sites for this callee might be removed if not needed elsewhere.
     * @param method The method to check
     * @return True if this taint wrapper can in general produce taints for the
     * given method.
     */
    boolean supportsCallee(SootMethod method);

}
