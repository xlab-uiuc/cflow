package taintAnalysis;

import soot.jimple.Stmt;
import taintAnalysis.utility.PhantomRetStmt;

import java.util.*;

public class PathVisitor {

    private enum Color {
        WHITE,
        GREY,
        BLACK
    }

    long cnt = 0;

    public void visit(Taint t) {
        Map<Taint, Color> status = new HashMap<>();
        dfs(t, 0, status, new Stack<>(), true);
        long cnt1 = cnt;
        System.out.println(t);
        System.out.println("run1: " + Long.toString(cnt1));
        cnt = 0;

        dfs(t, 0, status, new Stack<>(), false);
        long cnt2 = cnt;
//        System.out.println(t);
        System.out.println("run2: " + Long.toString(cnt2));
        if (cnt1 != cnt2)
            System.out.println("##### non-deterministic");

    }

    private void dfs(Taint t, int depth, Map<Taint, Color> status, Stack<Stmt> callerStack, boolean sorted) {
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);

        status.put(t, Color.GREY);
        Stmt curStmt = t.getStmt();
        boolean isEndPoint = true;
        ArrayList<Taint> lst = new ArrayList<>(t.getSuccessors());
        if (sorted)
            lst.sort(Comparator.comparing(Taint::toString));
        for (Taint successor : lst) {
            if (status.get(successor) != Color.GREY) {
                if (t.getTransferType() == Taint.TransferType.Call) {
                    callerStack.push(t.getStmt());
                    isEndPoint = false;
                    dfs(successor, depth + 1, status, callerStack, sorted);
                    callerStack.pop();
                } else if (curStmt instanceof PhantomRetStmt && !callerStack.isEmpty()) {
                    if (callerStack.peek() == successor.getStmt()) {
                        Stmt callSite = callerStack.peek();
                        callerStack.pop();
                        isEndPoint = false;
                        dfs(successor, depth + 1, status, callerStack, sorted);
                        callerStack.push(callSite);
                    }
                } else {
                    isEndPoint = false;
                    dfs(successor, depth + 1, status, callerStack, sorted);
                }
            }
        }
        if (isEndPoint) {
            cnt++;
        }
        status.put(t, Color.BLACK);
    }

}