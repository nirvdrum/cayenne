#!/bin/sh

# Bourne shell script to start Cayenne Regression.
#
# Certain parts are modeled after Tomcat startup scrips, 
# Copyright Apache Software Foundation

MAIN_CLASS=org.objectstyle.cayenne.regression.Main


# OS specific support.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

PATH_SEPARATOR=:
if [ "$cygwin" = "true" ] ; then 
	PATH_SEPARATOR=";"
fi


if [ "$JAVA_HOME" = "" ] ; then 
    echo "Please define JAVA_HOME to point to your JSDK installation."
    exit 1
fi

# Guess from startup directory
if [ "$CAYENNE_HOME" = "" ] ; then
	# resolve links - $0 may be a softlink
	PRG="$0"

	while [ -h "$PRG" ] ; do
  		ls=`ls -ld "$PRG"`
  		link=`expr "$ls" : '.*-> \(.*\)$'`
  		if expr "$link" : '.*/.*' > /dev/null; then
    		PRG="$link"
  		else
			PRG=`dirname "$PRG"`/"$link"
		fi
	done
 
	CAYENNE_HOME=`dirname "$PRG"`
	CAYENNE_HOME=`dirname "$CAYENNE_HOME"`
fi


if [ ! -f $CAYENNE_HOME/bin/regression.sh ] ; then
    echo "Please define CAYENNE_HOME to point to your Cayenne installation."
    exit 1
fi

JAVACMD=$JAVA_HOME/bin/java
if [ ! -f $JAVACMD ] ; then
	JAVACMD=$JAVA_HOME/jre/bin/java
fi

OPTIONS="-Xms60m -Xmx60m -classpath $CAYENNE_HOME/lib/regression/cayenne-regression.jar"
if [ "$CLASSPATH" != "" ] ; then
	OPTIONS="$OPTIONS$PATH_SEPARATOR$CLASSPATH"
fi

$JAVACMD $OPTIONS $MAIN_CLASS $1 $2 $3 & 


