#!/bin/sh

PROJECT_VERSION=1.2-SNAPSHOT
JAVA_HOME=/opt/java-1.5
CAYENNE_BASE=/home/andrus/work/cayenne-snapshots/cayenne
CAYENNE_ANT=$CAYENNE_BASE/cayenne-ant

cd $CAYENNE_BASE
cvs up

cd $CAYENNE_ANT
ant clean

cd $CAYENNE_ANT/ant/maven
ant -Dproject.version=$PROJECT_VERSION

$CAYENNE_ANT/bin/maven-deploy-bundle.sh
