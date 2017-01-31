#!/bin/bash
#
# Bash script to run the application under Linux.
#
EXE_HOME=$(dirname `realpath $0`)
$JAVA_HOME/bin/java -Dconfig=${EXE_HOME}/Application.properties \
  -Djava.util.logging.config.file=${EXE_HOME}/logging.properties \
  -Dkeystore.file=${EXE_HOME}/keystore.jks \
  -jar ${EXE_HOME}/lib/ftinterface-server-${project.version}.jar $@
