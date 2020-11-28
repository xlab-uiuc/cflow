package configInterface;

import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.StringConstant;

import java.util.List;

public class SparkInterface implements ConfigInterface {
    private static final String configClass = "org.apache.spark.SparkConf";

    @Override
    public boolean isGetter(InvokeExpr iexpr) {
        SootMethod callee = iexpr.getMethod();
        if (callee.getDeclaringClass().toString().contains(configClass) &&
                callee.getName().startsWith("get")) {
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
        if (callee.getDeclaringClass().toString().contains(configClass) &&
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

}
