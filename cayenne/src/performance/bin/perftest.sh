#!/bin/sh

if [ "$JAVA_HOME" = "" ] ; then
    echo "Please define JAVA_HOME to point to your JSDK installation."
    exit
fi

if [ "$CAYENNE_HOME" = "" ] ; then
    echo "Please define CAYENNE_HOME to point to your Cayenne installation."
    exit
fi

$JAVA_HOME/bin/java -jar $CAYENNE_HOME/lib/cayenne-performance.jar $@
cd ..
