#!/bin/sh

if [ "$JAVA_HOME" = "" ] ; then
    echo "Please define JAVA_HOME to point to your JSDK installation."
    exit
fi

if [ "$CAYENNE_HOME" = "" ] ; then
    echo "Please define CAYENNE_HOME to point to your Cayenne installation."
    exit
fi

if [ -d testrun ] ; then
    if [ -d "testrun.bak" ] ; then
        rm -rf "testrun.bak" 
    fi
    mv testrun testrun.bak
fi

mkdir testrun
cd testrun
$JAVA_HOME/bin/java -jar $CAYENNE_HOME/lib/cayenne_tests.jar $1 $2 $3
cd ..
