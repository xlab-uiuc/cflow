package taintAnalysis;

import soot.SootMethod;
import soot.jimple.Stmt;
import taintAnalysis.utility.PhantomRetStmt;

import java.util.*;

public class PathVisitor {

    private final long threshold;
    private long cnt;

    public PathVisitor() {
        this(20000);
    }

    public PathVisitor(long threshold) {
        this.threshold = threshold;
    }

    public void visit(Taint t) {
        cnt = 0;
        Set<Taint> sinks = new HashSet<>();
        Stack<Stmt> callerStack = new Stack<>();
        Set<SootMethod> methodSet = new HashSet<>();
        methodSet.add(t.getMethod());
        Stack<Set<Taint>> visitedStack = new Stack<>();
        visitedStack.push(new HashSet<>());
        System.out.println("source: " + t);
        dfs(t, 0, callerStack, methodSet, visitedStack, sinks);
        System.out.println("Number of paths: " + cnt);
        System.out.println();
    }

    private void dfs(Taint t, int depth, Stack<Stmt> callerStack, Set<SootMethod> methodSet,
                     Stack<Set<Taint>> visitedStack, Set<Taint> sinks) {
        if (cnt > threshold) {
            return;
        }
        Set<Taint> visited = visitedStack.peek();
        if (visited.contains(t)) {
            return;
        }
        visited.add(t);

        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);

        if (t.isSink()) {
            sinks.add(t);
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
                    dfs(successor, depth+1, callerStack, methodSet, visitedStack, sinks);
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
                        dfs(successor, depth+1, callerStack, methodSet, visitedStack, sinks);
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
                        dfs(successor, depth+1, callerStack, methodSet, visitedStack, sinks);
                        visitedStack.pop();
                        methodSet.remove(caller);
                    }
                }
            } else {
                // Visit within the same method
                isEndPoint = false;
                dfs(successor, depth+1, callerStack, methodSet, visitedStack, sinks);
            }
        }
        if (isEndPoint) {
            cnt++;
        }
    }

}
