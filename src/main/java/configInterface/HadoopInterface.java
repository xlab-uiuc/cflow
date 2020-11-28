package configInterface;

import java.util.List;

import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.StringConstant;

public class HadoopInterface implements ConfigInterface {
    private static final String superConfigClass = "org.apache.hadoop.conf.Configuration";

    @Override
    public boolean isGetter(InvokeExpr iexpr) {
        SootMethod callee = iexpr.getMethod();
        if (isSubClass(callee.getDeclaringClass()) &&
                callee.getName().startsWith("get")) {
//            System.out.println(callee.getName());
            List<Value> args = iexpr.getArgs();
            if (args.size() > 0 && args.get(0) instanceof StringConstant) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSetter(InvokeExpr iexpr) {
        SootMethod callee = iexpr.getMethod();
        if (isSubClass(callee.getDeclaringClass()) &&
                callee.getName().startsWith("set")) {
            List<Value> args = iexpr.getArgs();
            if (args.size() == 2 && args.get(0) instanceof StringConstant) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getConfigName(InvokeExpr iexpr) {
        if (iexpr.getArgCount() > 0 &&
                iexpr.getArg(0) instanceof StringConstant) {
            Value name = iexpr.getArg(0);
            StringConstant strVal = (StringConstant) name;
            return strVal.value;
        }
        return null;
    }

    @Override
    public Value getDefaultValue(InvokeExpr iexpr) {
        if (iexpr.getArgCount() == 2 &&
                iexpr.getArg(0) instanceof StringConstant &&
                iexpr.getArg(1) instanceof Constant) {
            Value defaultValue = iexpr.getArg(1);
            return defaultValue;
        }
        return null;
    }

    private boolean isSubClass(SootClass cls) {
        if (cls.toString().contains(superConfigClass)) {
            return true;
        }
        if (cls.hasSuperclass() && cls.getSuperclass().toString().contains(superConfigClass)) {
            return true;
        }
        return false;
    }

}
