#!/bin/sh

# Bourne shell script to start Cayenne Modeler.

if [ "$JAVA_HOME" = "" ] ; then 
    echo "Please define JAVA_HOME to point to your JSDK installation."
    exit 1
fi


if [ "$CAYENNE_HOME" = "" ] ; then
    echo "Please define CAYENNE_HOME to point to your Cayenne installation."
    exit 1
fi

JAVACMD=$JAVA_HOME/bin/java
if [ ! -f $JAVACMD ] ; then
	JAVACMD=$JAVA_HOME/jre/bin/java
fi

$JAVACMD -jar $CAYENNE_HOME/lib/modeler/cayenne-modeler.jar $1 $2 $3 & 


