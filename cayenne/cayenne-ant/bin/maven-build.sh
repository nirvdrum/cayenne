#!/bin/sh

JAVA_HOME=/opt/java-1.5
CAYENNE_BASE=/home/andrus/work/cayenne-snapshots/cayenne
CAYENNE_ANT=$CAYENNE_BASE/cayenne-ant

cd $CAYENNE_BASE
cvs up

cd $CAYENNE_ANT
ant clean

cd $CAYENNE_ANT/ant/maven
ant install-all -Dm2.repo=/var/sites/objectstyle/html/maven2 -Dproject.version=1.2-SNAPSHOT


