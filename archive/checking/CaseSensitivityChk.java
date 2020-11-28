package checking;

import acai.configInterface.ConfigInterface;
import org.xml.sax.SAXException;
import soot.Type;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

import acai.utility.Util;

public class CaseSensitivityChk implements CheckPass{

   // Case type of a string
   final int SOOT_TOKEN = 0; // tokens in soot such as $r1, r14. should not analyse their case type
   final int ALL_LOWER = 1; // AAA
   final int ALL_UPPER = 2; // aaa
   final int MIX = 3; // AAa

   private class CaseInfo{
       CaseInfo() {
           strings = new ArrayList<>();
           equalsExist = equalsIgnoreCaseExists = allLowerExists = allUpperExists = mixExists = false;
       }
       List<String> strings; // all the args of "equals" or "equalsIgnoreCase", used for log
       boolean equalsExist;
       boolean equalsIgnoreCaseExists;
       boolean allUpperExists;
       boolean allLowerExists;
       boolean mixExists;
   }

   @Override
   public void runChecking(ConfigInterface configInterface, InfoflowResults results, String[][] considered) {
       Map<String, CaseInfo> mp = new HashMap<>();
       for (ResultSinkInfo sink : results.getResults().keySet()) {
           for (ResultSourceInfo source : results.getResults().get(sink)) {
               Stmt[] path = source.getPath();

               if (path == null) continue;
               Stmt firstStmt = path[0];

               if (!(firstStmt instanceof DefinitionStmt)) continue;
               DefinitionStmt definitionFirstStmt = (DefinitionStmt) path[0];

               Type type = definitionFirstStmt.getLeftOp().getType();
               if (!type.toString().equals("java.lang.String")) continue; // No need to check case sensitivity of non-String type params

               //String paramName = definitionFirstStmt.getInvokeExpr().getArg(0).toString();
               String paramName = configInterface.getConfigName(definitionFirstStmt.getInvokeExpr());

               // ignore parameters whose flowdroid results are inconsistent
               Set<String> affectedParams = Util.getAffectedParams(considered);
               if (affectedParams.contains(paramName)) continue;

               for (Stmt stmt: path){
                   try {
                       DefinitionStmt definitionStmt = (DefinitionStmt) stmt;
                       String methodName = definitionStmt.getInvokeExpr().getMethod().getName();

                       if (methodName.equals("equals")) {
                           String arg = definitionStmt.getInvokeExpr().getArg(0).toString();
                           if (!mp.containsKey(paramName)){
                               CaseInfo caseInfo = new CaseInfo();
                               caseInfo.strings.add(arg + "(used in method \"equals\")");
                               caseInfo.equalsExist = true;
                               mp.put(paramName, caseInfo);
                           } else {
                               mp.get(paramName).strings.add(arg + "(used in method \"equals\")");
                               mp.get(paramName).equalsExist = true;
                           }
                           int argCaseType = caseType(arg);
                           switch (argCaseType){
                               case SOOT_TOKEN:
                                   break;
                               case ALL_UPPER:
                                   mp.get(paramName).allUpperExists = true;
                                   break;
                               case ALL_LOWER:
                                   mp.get(paramName).allLowerExists = true;
                                   break;
                               case MIX:
                                   mp.get(paramName).mixExists = true;
                                   break;
                           }

                       } else if (methodName.equals("equalsIgnoreCase")){
                           String arg = definitionStmt.getInvokeExpr().getArg(0).toString();
                           if (!mp.containsKey(paramName)){
                               CaseInfo caseInfo = new CaseInfo();
                               caseInfo.strings.add(arg + "(used in method \"equalsIgnoreCase\")");
                               caseInfo.equalsIgnoreCaseExists = true;
                               mp.put(paramName, caseInfo);
                           } else {
                               mp.get(paramName).strings.add(arg + "(used in method \"equalsIgnoreCase\")");
                               mp.get(paramName).equalsIgnoreCaseExists = true;
                           }
                           int argCaseType = caseType(arg);
                           switch (argCaseType){
                               case SOOT_TOKEN:
                                   break;
                               case ALL_UPPER:
                                   mp.get(paramName).allUpperExists = true;
                                   break;
                               case ALL_LOWER:
                                   mp.get(paramName).allLowerExists = true;
                                   break;
                               case MIX:
                                   mp.get(paramName).mixExists = true;
                                   break;
                           }
                       }
                   } catch (Exception exception){

                   }
               }

           }
       }



       // log information
       System.out.println("===== log of case sensitivity check =====");
       for (Map.Entry<String, CaseInfo> entry : mp.entrySet()) {
           String key = entry.getKey();
           CaseInfo caseInfo = entry.getValue();
           List<String> list = caseInfo.strings;
           System.out.println();
           System.out.println(key + " : ");
           for (String s : list){
               System.out.println(s);
           }
       }
       System.out.println("===== end of log of case sensitivity check =====");

       boolean findInconsistency = false;
       for (Map.Entry<String, CaseInfo> entry : mp.entrySet()){
           String key = entry.getKey();
           CaseInfo caseInfo = entry.getValue();
           if (caseInfo.equalsExist && caseInfo.equalsIgnoreCaseExists) {
               System.out.println("Case sensitivity inconsistency warning: Both \"equals\" and \"equalsIgnoreCase\" exist " +
                       "in the flow of param " + key);
               findInconsistency = true;
           }
           int a = caseInfo.allLowerExists ? 1 : 0;
           int b = caseInfo.allUpperExists ? 1 : 0;
           int c = caseInfo.mixExists ? 1 : 0;
           if (a + b + c > 1) {
               System.out.println("Case sensitivity inconsistency warning: More than one type of string case type (all uppercase, " +
                       "all lowercase, mixture of uppercase and lowercase) exist in the flow of param " + key);
               findInconsistency = true;
           }
       }
       if (!findInconsistency) {
           System.out.println("Case sensitivity check passed");
       }
   }

   private int caseType(String arg){
       if ((arg.length() > 2 && arg.startsWith("$r") && '0' <= arg.charAt(2) && arg.charAt(2) <= '9')
           || (arg.length() > 1 && arg.startsWith("r") && '0' <= arg.charAt(1) && arg.charAt(1) <= '9')){
           return SOOT_TOKEN;
       }
       boolean upperExists = false;
       boolean lowerExists = false;
       for (int i = 0;i < arg.length();i++){
           if ('a' <= arg.charAt(i) && arg.charAt(i) <= 'z') {
               lowerExists = true;
           }
           if ('A' <= arg.charAt(i) && arg.charAt(i) <= 'Z') {
               upperExists = true;
           }
       }
       if (upperExists && !lowerExists) return ALL_UPPER;
       if (!upperExists && lowerExists) return ALL_LOWER;
       return MIX;
   }

}
