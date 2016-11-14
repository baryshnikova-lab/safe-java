# Spatial Analysis of Functional Enrichment (SAFE) for Java

## Introduction

This is the Java implementation of the SAFE algorithm. This project also includes a SAFE app for [Cytoscape](http://cytoscape.org/) featuring an interactive version of the algorithm (see the [instructions](https://github.com/baryshnikova-lab/safe-java/wiki) on download and installation).

The original SAFE code in MATLAB is available at https://bitbucket.org/abarysh/safe.

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
