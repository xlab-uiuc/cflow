package checking;

import acai.configInterface.ConfigInterface;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class UnusedParamChk implements CheckPass {

   @Override
   public void runChecking(ConfigInterface configInterface, InfoflowResults results, String[][] considered) {
       HashSet<String> paramSet = new HashSet<>();
       for (ResultSinkInfo sink : results.getResults().keySet()) {
           for (ResultSourceInfo source : results.getResults().get(sink)) {
               Stmt sourceStmt = source.getStmt();
               String configName = configInterface.getConfigName(sourceStmt.getInvokeExpr());
               paramSet.add(configName);
           }
       }

       Set<String> defaultConfigParams = null;
       try {
           defaultConfigParams = parseDefaultConfig();
       } catch (ParserConfigurationException e) {
           e.printStackTrace();
       } catch (IOException e) {
           e.printStackTrace();
       } catch (SAXException e) {
           e.printStackTrace();
       }
       defaultConfigParams.removeAll(paramSet);
       System.out.println("#####\nunused default parameters");
       for (String s : defaultConfigParams) {
           System.out.println(s);
       }
   }

   private Set<String> parseDefaultConfig() throws ParserConfigurationException, IOException, SAXException {
       DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
       factory.setValidating(true);
       factory.setIgnoringElementContentWhitespace(true);
       DocumentBuilder builder = factory.newDocumentBuilder();
       // TODO: hardcode hadoop default configuration for simplicity
       File file = new File("app/hadoop-3.3.0/share/doc/hadoop/hadoop-project-dist/hadoop-common/core-default.xml");
       Document doc = builder.parse(file);
       Set<String> defaultConfig = new HashSet<>();
       NodeList nodes = doc.getElementsByTagName("configuration");
       NodeList childList = nodes.item(0).getChildNodes();
       for (int i = 0; i < childList.getLength(); i++) {
           Node propertyNode = childList.item(i);
           NodeList propertyNodeList = propertyNode.getChildNodes();
           String nodeName = "";
           for (int j = 0; j < propertyNodeList.getLength(); j++) {
               Node node= propertyNodeList.item(j);
               if (node.getNodeName() == "name") {
                   nodeName = node.getTextContent();
               } else if (node.getNodeName() == "value") {
                   // if value tag is empty, we don't consider it as unused parameter
                   if (node.getTextContent() != "") {
                       defaultConfig.add(nodeName);
                   }
                   break;
               }
           }
       }

       return defaultConfig;
   }

}
