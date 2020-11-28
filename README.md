# cflow

Flow-based configuration analysis

## Project Doc

* [Project Proposal](https://docs.google.com/document/d/1nyOCS7g5iyMzjjQasGd7yhnjIIlhQ9X2ivUStXT_N-k/edit?usp=sharing)

### Taint Tracking Infra

* [Soot Tutorial](http://www.iro.umontreal.ca/~dufour/cours/ift6315/docs/soot-tutorial.pdf)
* [Design Slide](https://docs.google.com/presentation/d/1XluXB7bBepI0bVzGl3IhC9ecMd1SiP1sxXrHQZax10o/edit?usp=sharing)

## How to Run

### Compile

```
$ mvn compile
```

### Test

```
$ mvn test
```

### Run

To run on the test jar:

```
$ ./run.sh -a test [-i]
```

To run on Hadoop Common:

```
$ ./run.sh -a hadoop_common [-i]
```

If the -i flag is enabled, only intra-procedural analysis in performed.
