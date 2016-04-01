# Spatial Analysis of Functional Enrichment (SAFE) for Java

## Introduction

This is the Java port of the [original implementation of the SAFE algorithm in MATLAB](https://bitbucket.org/abarysh/safe). This project also includes a [Cytoscape](http://cytoscape.org/) app featuring an interactive version of the algorithm.

### Compilation

To compile SAFE for Java from source, you'll need:

* [Java 8 SDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven 3.3](https://maven.apache.org/download.cgi)

Then run the following from the top-level directory:

	$ mvn package

That will compile the Java library into:

	core/target/safe-core-{version}.jar

...and the Cytoscape app into:

	cytoscape/target/safe-cytoscape-{version}.jar
