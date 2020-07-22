#!/bin/bash

SCRIPT_DIR=$(dirname $(readlink -f $0))

java -classpath "$SCRIPT_DIR/../lib/*" -Dlogback.configurationFile="$SCRIPT_DIR/../conf/logback.xml" -Ddk.kb.applicationConfig="$SCRIPT_DIR/../conf/doms-pidregistration.properties" -jar "$SCRIPT_DIR/../lib/doms-pidregistration.jar" "$@"

