package checking;

import acai.configInterface.ConfigInterface;
import soot.jimple.infoflow.results.InfoflowResults;

public interface CheckPass {

   void runChecking(ConfigInterface configInterface, InfoflowResults results, String[][] considered);

}
