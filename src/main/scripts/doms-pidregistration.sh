#!/bin/bash

SCRIPT_DIR=$(dirname "$(readlink -f -- ${BASH_SOURCE[0]})")

JAVA_OPTS="-Djdk.crypto.KeyAgreement.legacyKDF=true"

java "$JAVA_OPTS" -classpath "$SCRIPT_DIR/../lib/*" -Dlogback.configurationFile="$SCRIPT_DIR/../conf/logback.xml" -Ddk.kb.applicationConfig="$SCRIPT_DIR/../conf/doms-pidregistration.properties" -jar "$SCRIPT_DIR/../lib/doms-pidregistration.jar" "$@"

