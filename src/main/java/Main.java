import acai.Acai;
import acai.configInterface.ConfigInterface;
import acai.utility.AcaiConfig;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        String[] cfg = null;
        try {
            cfg = AcaiConfig.getCfg("hadoop_common");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        List<String> appPaths = AcaiConfig.getAppPaths(cfg);
        List<String> srcPaths = AcaiConfig.getSourcePaths(cfg);
        List<String> classPaths = AcaiConfig.getClassPaths(cfg);
        ConfigInterface configInterface = AcaiConfig.getInterface(cfg);

        Acai acai = new Acai(appPaths, srcPaths, classPaths,configInterface);
        acai.computeInfoflow();
        InfoflowResults results = acai.getResults();

        // Example: Get the data type of the config value that falls into the sink
        for (ResultSinkInfo sink : results.getResults().keySet()) {
            System.out.println(sink);
            for (ResultSourceInfo source : results.getResults().get(sink)) {
                System.out.println(source);
                Stmt[] path = source.getPath();
                if (path != null) {
                    Stmt stmt = path[path.length - 2];
                    DefinitionStmt ass = (DefinitionStmt)stmt;
                    Value value = ass.getLeftOp();
                    System.out.println(value.getType());
                }
                break;
            }
            break;
        }
    }
}
