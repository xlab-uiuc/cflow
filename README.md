# ccc

Configuration Consistency Checker

## Project Docs

* [Project Proposal](https://docs.google.com/document/d/1nyOCS7g5iyMzjjQasGd7yhnjIIlhQ9X2ivUStXT_N-k/edit?usp=sharing)
* First [issue](https://github.com/xlab-uiuc/ccc/issues/1) in this repo on Project Scope and Approach

### Consistency Checking

* [Common Properties Doc](https://docs.google.com/document/d/1d-FnKT3N6oEbi8nm-2HJEtfbhUiXxWYeiq36_nnrXCg/edit?usp=sharing)
* [Inconsistencies found](https://docs.google.com/document/d/1fQHlm-B35eHaKczrnIS9FRXRKOyOKJvCCVDcuunWh-k/edit)

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

One example running command is as follows:

```
$ ./run.sh -a hadoop_common
```
