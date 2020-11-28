package checking;

import acai.configInterface.ConfigInterface;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import soot.Type;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DataTypeChk implements CheckPass {

   @Override
   public void runChecking(ConfigInterface configInterface, InfoflowResults results, String[][] considered) {
       HashMap<String, Set<Type>> m = new HashMap<>();
       for (ResultSinkInfo sink : results.getResults().keySet()) {
           for (ResultSourceInfo source : results.getResults().get(sink)) {
               Stmt[] path = source.getPath();
               if (path != null) {
                   Stmt sourceStmt = path[0];
                   String configName = configInterface.getConfigName(sourceStmt.getInvokeExpr());
                   Type tp = ((DefinitionStmt)sourceStmt).getLeftOp().getType();
                   if (!m.containsKey(configName)) {
                       m.put(configName, new HashSet<>());
                   }
                   m.get(configName).add(tp);
               }
           }
       }
       for (String s : m.keySet()) {
           System.out.println(s + " : " + m.get(s).toString());
       }
   }

}
