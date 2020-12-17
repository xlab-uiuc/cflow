# cFlow: A Flow-based Configuration Analysis Framework

cFlow is a flow-, field-, and context-sensitive static taint analysis framework for Java bytecode based clooud systems that tracks how configuration option values flow through a program from their loading points to the user-specified sink points where the values are used (e.g. an external API call), and can output the taint propagation path from the source to the sink. It could also be used as a generic static taint anaysis tool by providing your own definition of sources and sinks.

## Use cFlow from the command line

S1. Download the software you want to analyze and put it under `/app`. Currently supported cloud systems include: hdfs, mapreduce, yarn, hadoop_common, hadoop_tools, hbase, spark.

S2. Compile cFlow

```sh
mvn compile
```

S3. Run the analysis.

```sh
./run.sh -a hadoop_common [-i] [-s]
```

If the `-i` flag is enabled, only intra-procedural analysis in performed, used for testing only.

If the `-s` flag is enabled, the SPARK call graph toolkit is used to compute a more accurate call graph at the cost of longer running time and higher memory consumption.

S4. Inspect the result.

A `tmp.txt` file will be generated, which contains all the discovered taint propagation paths from sources to sinks.

## Use cFlow as a library

The following code piece illustrates how to use cFlow as a library. You may also want to refer to `Main.java`.

```java
// The configuration for the analyzing the software, which is predefined in Config.java
String[] cfg = ...;


// srcPaths is a list of string paths to the jars file of the core part of the software (usu. excluding library code)
List<String> srcPaths = Config.getSourcePaths(cfg));

// classPaths is a list of string paths to the jars file of the software (including library code)
List<String> classPaths = Config.getClassPaths(cfg));

// Create an instance of ConfigInterace (specifies how to identify configuration loading/setting points)
ConfigInterface configInterface = Config.getInterface(cfg);

// Create an instance of SourceSinkManager (specifes the taint sources and sinks)
ISourceSinkManager sourceSinkManager = new SourceSinkManager(configInterface);

// Create an instance of TaintWrapper (used for library modeling)
ITaintWrapper taintWrapper = TaintWrapper.getDefault();


/* Run analysis */
TaintAnalysisDriver driver = new TaintAnalysisDriver(sourceSinkManager, taintWrapper);

// Run intra-procedural analysis
IntraAnalysisTransformer intraTransformer = 
    driver.runIntraTaintAnalysis(srcPaths, classPaths);

// Run inter-procedural analysis
// if use_spark is set to true, use the SPARK call graph toolkit for computing the call graph
InterAnalysisTransformer interTransformer = 
    driver.runInterTaintAnalysis(srcPaths, classPaths, use_spark);

// Get the results of the inter-procedural analysis: 
//  The key of the map is source taint.
//  List<Taint> represents a taint propagation path.
//  The value of the map is a list of taint propagation paths between the source and a sink.
Map<Taint, List<List<Taint>>> results = interTransformer.getPathsMap();
```

## The software I want to analyze is not supported

S1. Download the software and put it under `\app` as usual.

S2. Extend `taintAnalysis/utility/Config.java` to specify where to load the source code of the analyzed software. Two path needs to specified:

* Source Path: The path to the core of the analyzed software, excluding library code (unless you want cFlow to look into the library codebase for full-scale analysis).

* Class Path: The path to both the core of the software and its dependencies.

S3. (Optional) Implement the `ConfigInterface` interface in the `configInterface` package to specify how to identify the configuration loading/setting points.

S4. (Optional) Implement the `ISourceSinkManager` interface in the `taintAnalysis/sourceSinkManager` subpackage to specify the sources and sinks used for the analysis.

Now, you should be good to go.

## Code structure

The architecture of cFlow is as follows:

![142](doc/cflow.jpg)

The source code of cFlow is placed under the `src/main/java` directory.

It contains the following main modules:

* `configInterface` package contains the configuration interface used to identify configuration loading/setting points; implementations for several cloud systems have already been provided.
* `taintAnalysis` package contains the core module for static taint analysis:
  * `sourceSinkManger` subpackage contains the interface for defining sources/sinks and a default implementation of it;
  * `taintWrapper` subpackage contains the interface for defining library modeling rules and a default implementation using herustic-based rules.
  * `Taint.java` contains the implementation of the field-sensitive taint abstraction.
  * `TaintFlowAnalysis.java` contains the implementaition for intra-procedural taint analysis, which extends the `ForwardFlowAnalysis` class in Soot.
  * `InterTaintAnalysis.java` contains the implmentation for inter-procedural taint analysis.
  * `PathVisitor.java` contains the implementation of a recursive realizable path visitor that traverses all the realizable taint propagation paths starting from a source taint. Mainly used for validation, and not enabled by default.
  * `SourceSinkConnectionVisitor.java` containts the the implementation of a recursive realizable path reconstructor that reconstructs the realizable taint propagation paths between any source sink pair.
* `utility` package contains `Config.java`, which specifies where to load the source code of the analyzed software. If the analyzed software is not supported by default, you should extend this file.

## Documents

For more details of the design and implementation of cFlow:

* [Report](doc/cflow_report.pdf)

* [Design Slide](https://docs.google.com/presentation/d/1XluXB7bBepI0bVzGl3IhC9ecMd1SiP1sxXrHQZax10o/edit?usp=sharing)

## Resources

* [Soot Tutorial](http://www.iro.umontreal.ca/~dufour/cours/ift6315/docs/soot-tutorial.pdf)
