package configInterface;

import soot.Value;
import soot.jimple.InvokeExpr;

public interface ConfigInterface {

    boolean isGetter(InvokeExpr iexpr);

    boolean isSetter(InvokeExpr iexpr);

    String getConfigName(InvokeExpr iexpr);

    Value getDefaultValue(InvokeExpr iexpr);

}
