package configInterface;

import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;

public class TestInterface implements ConfigInterface {

    @Override
    public boolean isGetter(InvokeExpr iexpr) {
        SootMethod callee = iexpr.getMethod();
        if (callee.getName().contains("source")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSetter(InvokeExpr iexpr) {
        return false;
    }

    @Override
    public String getConfigName(InvokeExpr iexpr) {
        return null;
    }

    @Override
    public Value getDefaultValue(InvokeExpr iexpr) {
        return null;
    }

}
