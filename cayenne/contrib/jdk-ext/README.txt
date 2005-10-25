This directory contains Cayenne extensions that need to be compiled with specific JDK versions
(usually a newer version of JDK than the oldest currently supported by Cayenne)
As this implies different project settings in Eclipse, it has to be checked out as a separate 
project from CVS.


1. Compiling an extension subproject checked out as a separate Eclipse project in the 
same workspace as Cayenne

# cd subproject
# ant -Dcayenne.base="../cayenne"


2. Compiling subproject from Cayenne source tree

# cd cayenne/contrib/jdk-ext/subproject
# ant