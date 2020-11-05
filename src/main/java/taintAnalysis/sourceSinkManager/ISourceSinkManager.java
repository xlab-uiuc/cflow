package taintAnalysis.sourceSinkManager;

import soot.jimple.Stmt;

public interface ISourceSinkManager {

    /**
     * Checks whether the given statement is a source statement
     *
     * @param stmt  The statement to check
     * @return True if the given statement is a source statement, otherwise false
     */
    boolean isSource(Stmt stmt);

    /**
     * Checks whether the given statement is a sink statement
     *
     * @param stmt  The statement to check
     * @return True if the given statement is a sink statement, otherwise false
     */
    boolean isSink(Stmt stmt);

}
