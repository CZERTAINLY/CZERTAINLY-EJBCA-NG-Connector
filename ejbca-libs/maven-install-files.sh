#!/usr/bin/env bash

# Find directory for this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink -f "$SOURCE")"; done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# Install KFC dependencies not yet in Central
XCU_VERSION=3.2.0
mvn ${MVN_OPTS} install:install-file -Dfile="${DIR}/x509-common-util-${XCU_VERSION}.jar" -DgroupId=com.keyfactor -DartifactId=x509-common-util -Dversion=${XCU_VERSION} -Dpackaging=jar
