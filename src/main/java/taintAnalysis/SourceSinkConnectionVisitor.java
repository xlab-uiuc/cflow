package taintAnalysis;

import soot.SootMethod;
import soot.jimple.Stmt;
import taintAnalysis.utility.PhantomRetStmt;

import java.util.*;
import java.util.concurrent.Callable;

public class SourceSinkConnectionVisitor implements Callable<Object> {

    private final Taint source;
    private final long threshold;
    private final Set<Taint> sinks;
    private final List<List<Taint>> paths;

    private long cnt;

    public SourceSinkConnectionVisitor(Taint source) {
        this(source, 20000);
    }

    public SourceSinkConnectionVisitor(Taint source, long threshold) {
        this.source = source;
        this.threshold = threshold;
        this.sinks = new HashSet<>();
        this.paths = new ArrayList<>();
    }

    @Override
    public Object call() throws Exception {
        visit(source);
        return null;
    }

    public void visit(Taint t) {
        cnt = 0;
        Stack<Taint> intermediatePath = new Stack<>();
        Stack<Stmt> callerStack = new Stack<>();
        Set<SootMethod> methodSet = new HashSet<>();
        methodSet.add(t.getMethod());
        Stack<Set<Taint>> visitedStack = new Stack<>();
        visitedStack.push(new HashSet<>());
        dfs(t, callerStack, methodSet, visitedStack, sinks, intermediatePath, paths);
    }

    private void dfs(Taint t, Stack<Stmt> callerStack, Set<SootMethod> methodSet,
                     Stack<Set<Taint>> visitedStack, Set<Taint> sinks,
                     Stack<Taint> intermediatePath, List<List<Taint>> paths) {
        if (cnt > threshold) {
            return;
        }
        Set<Taint> visited = visitedStack.peek();
        if (visited.contains(t)) {
            return;
        }
        visited.add(t);
        intermediatePath.push(t);

        if (t.isSink()) {
            if (!sinks.contains(t)) {
                sinks.add(t);
                paths.add(new ArrayList(intermediatePath));
            }
        }

        boolean isEndPoint = true;
        Stmt currStmt = t.getStmt();
        ArrayList<Taint> successors = new ArrayList<>(t.getSuccessors());
        successors.sort(Comparator.comparing(Taint::toString));
        for (Taint successor : successors) {
            if (t.getTransferType() == Taint.TransferType.Call) {
                // Visit callee
                SootMethod callee = successor.getMethod();
                if (!methodSet.contains(callee)) {
                    callerStack.push(currStmt);
                    methodSet.add(callee);
                    visitedStack.push(new HashSet<>());
                    isEndPoint = false;
                    dfs(successor, callerStack, methodSet, visitedStack, sinks, intermediatePath, paths);
                    visitedStack.pop();
                    methodSet.remove(callee);
                    callerStack.pop();
                }
            } else if (currStmt instanceof PhantomRetStmt) {
                SootMethod callee = t.getMethod();
                SootMethod caller = successor.getMethod();
                if (!callerStack.isEmpty()) {
                    // Return to the previous callee
                    Stmt callSite = callerStack.peek();
                    if (callSite == successor.getStmt()) {
                        callerStack.pop();
                        methodSet.remove(callee);
                        visitedStack.pop();
                        isEndPoint = false;
                        dfs(successor, callerStack, methodSet, visitedStack, sinks, intermediatePath, paths);
                        visitedStack.push(visited);
                        methodSet.add(callee);
                        callerStack.push(callSite);
                    }
                } else {
                    // Return to an unexplored caller
                    if (!methodSet.contains(caller)) {
                        methodSet.add(caller);
                        visitedStack.push(new HashSet<>());
                        isEndPoint = false;
                        dfs(successor, callerStack, methodSet, visitedStack, sinks, intermediatePath, paths);
                        visitedStack.pop();
                        methodSet.remove(caller);
                    }
                }
            } else {
                // Visit within the same method
                isEndPoint = false;
                dfs(successor, callerStack, methodSet, visitedStack, sinks, intermediatePath, paths);
            }
        }
        if (isEndPoint) {
            cnt++;
        }

        intermediatePath.pop();
    }

    public long getThreshold() {
        return threshold;
    }

    public Set<Taint> getSinks() {
        return sinks;
    }

    public Taint getSource() {
        return source;
    }

    public List<List<Taint>> getPaths() {
        return paths;
    }

}
