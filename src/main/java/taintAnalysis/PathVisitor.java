package taintAnalysis;

import java.util.HashMap;
import java.util.Map;

public class PathVisitor {

    private enum Color {
        WHITE,
        GREY,
        BLACK
    }

    public void visit(Taint t) {
        Map<Taint, Color> status = new HashMap<>();
        dfs(t, 0, status);
    }

    private void dfs(Taint t, int depth, Map<Taint, Color> status) {
        for (int i = 0; i < depth; i++) {
            System.out.print("-");
        }
        System.out.println(t);

        status.put(t, Color.GREY);
        for (Taint successor : t.getSuccessors()) {
            if (status.get(successor) != Color.GREY) {
                dfs(successor, depth + 1, status);
            }
        }
        status.put(t, Color.BLACK);
    }

}
