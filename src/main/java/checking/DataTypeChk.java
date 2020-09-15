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

    public DataTypeChk() { }

    public void runChecking(ConfigInterface configInterface, InfoflowResults results) throws IOException, SAXException, ParserConfigurationException {
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

        Set<String> defaultConfigParams = parseDefaultConfig();
        defaultConfigParams.removeAll(m.keySet());
        System.out.println("#####\nunused default parameters");
        for (String s : defaultConfigParams) {
            System.out.println(s);
        }

    }

    public Set<String> parseDefaultConfig() throws ParserConfigurationException, IOException, SAXException {
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
            for (int j = 0; j < propertyNodeList.getLength(); j++) {
                Node node= propertyNodeList.item(j);
                if (node.getNodeName() == "name") {
                    defaultConfig.add(node.getTextContent());
                }
            }
        }

        return defaultConfig;
    }

}
