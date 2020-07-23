# DOMS pidregistration

Project to handle registration of objects in DOMS on handle.net infrastructure so that persistent identifiers is available and resolves. 

The tool is a commandline application that stores objects in a postgresql database and writes back PID/handle information to the objects in DOMS.

## Requirements
The project requires Java 8 be build and run. Known to build with OpenJDK 1.8, other JDKs may work

## Building
Use maven to build the project i.e. `mvn clean package`

## Test 
Besides the local unit tests, the setup requires access to a DOMS, and credentials to handle.net's infrastructure. 



