package taintAnalysis;

import soot.jimple.Stmt;
import taintAnalysis.utility.PhantomRetStmt;

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
        dfs(t, 0, status, new Stack<>());
    }

    private void dfs(Taint t, int depth, Map<Taint, Color> status, Stack<Stmt> callerStack) {
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);

        status.put(t, Color.GREY);
        Stmt curStmt = t.getStmt();
        for (Taint successor : t.getSuccessors()) {
            if (status.get(successor) != Color.GREY) {
                if (t.getTransferType() == Taint.TransferType.Call) {
                    callerStack.push(t.getStmt());
                    dfs(successor, depth + 1, status, callerStack);
                } else if (curStmt instanceof PhantomRetStmt && !callerStack.isEmpty()) {
                    if (callerStack.peek() == successor.getStmt()) {
                        callerStack.pop();
                        dfs(successor, depth+1, status, callerStack);
                    }
                } else {
                    dfs(successor, depth + 1, status, callerStack);
                }
            }
        }
        status.put(t, Color.BLACK);
    }

}
