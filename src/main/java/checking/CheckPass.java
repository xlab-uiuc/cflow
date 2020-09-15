package checking;

import acai.configInterface.ConfigInterface;
import org.xml.sax.SAXException;
import soot.jimple.infoflow.results.InfoflowResults;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Set;

public interface CheckPass {
    void runChecking(ConfigInterface configInterface, InfoflowResults results) throws IOException, SAXException, ParserConfigurationException;

    Set<String> parseDefaultConfig() throws ParserConfigurationException, IOException, SAXException;
}
