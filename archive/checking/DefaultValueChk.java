package checking;

import acai.configInterface.ConfigInterface;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultValueChk implements CheckPass {

   @Override
   public void runChecking(ConfigInterface configInterface, InfoflowResults results, String[][] considered) {
       HashMap<String, Set<Value>> defaultValueMap = new HashMap<>();
       for (ResultSinkInfo sink : results.getResults().keySet()) {
           for (ResultSourceInfo source : results.getResults().get(sink)) {
               InvokeExpr sourceInvokeExpr = source.getStmt().getInvokeExpr();
               String configName = configInterface.getConfigName(sourceInvokeExpr);
               Value defaultValue = configInterface.getDefaultValue(sourceInvokeExpr);
               if (!defaultValueMap.containsKey(configName)) {
                   defaultValueMap.put(configName, new HashSet<>());
               }
               if (defaultValue != null) {
                   defaultValueMap.get(configName).add(defaultValue);
               }
           }
       }

       System.out.println("=======runDefaultValueChecking=======");
       int numParams = defaultValueMap.size();
       int cnt = 0;
       for (Map.Entry<String, Set<Value>> pair : defaultValueMap.entrySet()) {
           String configName = pair.getKey();
           Set<Value> defaultValues = pair.getValue();
           if (defaultValues.size() > 1) {
               cnt++;
               System.out.printf("Config %s has more than one default values:\n", configName);
               System.out.printf("    %s\n", defaultValues.toString());
           }
       }
       System.out.printf("%d/%d configs have more than one default values.\n", cnt, numParams);
   }

}
