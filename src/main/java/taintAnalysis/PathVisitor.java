package taintAnalysis;

import soot.SootMethod;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class PathVisitor {

    private enum Color {
        WHITE,
        GREY,
        BLACK
    }

    public void visit(Taint t) {
        Map<Taint, Color> status = new HashMap<>();
        dfs(t, 0, status, new Stack<SootMethod>());
    }

    private void dfs(Taint t, int depth, Map<Taint, Color> status, Stack<SootMethod> callerStack) {
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);

        status.put(t, Color.GREY);
        Stmt curStmt = t.getStmt();
        for (Taint successor : t.getSuccessors()) {
            if (status.get(successor) != Color.GREY) {
                if (curStmt instanceof InvokeStmt) {
                    SootMethod currMethod = t.getMethod();
                    if (t.isMethodContextSwitched()) {
//                        System.out.println("###pushing " + currMethod.toString());
                        callerStack.push(currMethod);
                        dfs(successor, depth + 1, status, callerStack);
                    }
                } else if (curStmt instanceof ReturnStmt && !callerStack.isEmpty()) {
//                    System.out.println(callerStack.peek());
//                    System.out.println(successor.getMethod());
                    if (callerStack.peek() == successor.getMethod()) {
                        callerStack.pop();
                        dfs(successor, depth+1, status, callerStack);
                    }
                } else {
                    // if assign stmt contains invoker expr push currMethod to callerStack
                    if (curStmt.containsInvokeExpr()) {
                        // if t is not the leftOp, then this assign stmt has already been visited
                        // no need to push the method to the stack
                        if (t.getValue() != ((AssignStmt)curStmt).getLeftOp()) {
                            SootMethod currMethod = t.getMethod();
                            callerStack.push(currMethod);
                        }
                    }
                    dfs(successor, depth + 1, status, callerStack);
                }
            }
        }
        status.put(t, Color.BLACK);
    }

}
