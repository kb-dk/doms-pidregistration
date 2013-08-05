#!/bin/sh
pushd  $(dirname $0)/.. > /dev/null
java -Dlog4j.configuration=file://$HOME/pidregistration.log4j.xml -classpath lib/\* dk.statsbiblioteket.pidregistration.PIDRegistrationsCommandLineInterface "$@"
popd > /dev/null