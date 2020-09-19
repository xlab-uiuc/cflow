# Case sensitivity check

## Methodology

1. Only string type parameters are considered.
2. `equals` and `equalsIgnoreCase` should not both appear in the usages of the same parameter.
3. The arguments of `equals` for all usages of the same parameter should be all uppercase or all lowercase or all mixture. Otherwise the following inconsistency may happen: 

![img](caseSensitivityCheckPic/1.png)

## Current problem

1. The value of arguments may not be directly presented as strings. Sometimes the arguments are tokens in soot such as r2, $r3. We cannot figure out the concrete value and analyze its case type.

   ![](caseSensitivityCheckPic/4.png)

   

   After discussion in meeting, we assume that for some cases, the value of token may be able to be achieved. For example, a token $r1 is here in path[2]:

   ![](caseSensitivityCheckPic/2.png)

   

   The corresponding code in project is

   ```
   "murmur".equalsIgnoreCase(name)
   ```

   So we currently think that if we force to change the type of token or try to do some hacks, it is possible to get the value.

   

   However, under most cases the value cannot be determined that simply. It may not be deterministic so it is impossible to get the concrete value.

   

2. Some usages of `equals` or `equalsIgnoreCase` are not related to the parameter, but they are present in the flow and taken into consideration. Such usages may lead to unnecessary inconsistency warnings. 

   Here is an example. In a flow whose source is `get("hadoop.ssl.enabled.protocol")`, an `equalsIgnoreCase`  appears in the flow after a very very long process.

   ![](caseSensitivityCheckPic/5.png)

   The corresponding code in project is 

   ```
   if ("https".equalsIgnoreCase(kmsUrl.getProtocol())) 
   ```

   So here the method `equalsIgnoreCase()` is used to compare the value with "http". However, the default value of parameter `"hadoop.ssl.enabled.protocol"` is `"TLSv1,SSLv2Hello,TLSv1.1,TLSv1.2"`, which is obviously irrelevant with "http" . Therefore, the usage of `equalsIgnoreCase()` here has nothing to do with case sensitivity of the parameter. But it is present in the flow, and I currently cannot think out a way to distinguish it from those `equalsIgnoreCase()` which are actually related to the parameter. 

   

3. The result changes when I run the program, which may undermine the correctness of the analysis. In other words, a flow is possible to be present in the outcome, and it is also possible to be not there. Some flows I encountered differ from the flows recorded in  `/flows` of `ccc` repository as well.

   Personally, I suspect the phenomenon is due to the balance between correctness and accuracy in soot, and may be related to the set of soot configuration. But it needs more actions and tries to be proved.
