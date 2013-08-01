#!/bin/sh
java -Dlog4j.configuration=file://$(readlink -f $(dirname $0))/../config/log4j.xml -classpath lib/\* dk.statsbiblioteket.pidregistration.HandleRegistrationsTool "$@"