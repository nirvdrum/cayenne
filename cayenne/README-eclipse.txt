How to check out Cayenne to Eclipse
===================================
    
1. What you'll need: 
   * JDK 1.5 or newer
   * Eclipse 3.1.1 or newer
   
   
2. Repository Structure.

Cayenne CVSROOT for pserver anonymous users is
   :pserver:anonymous@cvs.sourceforge.net:/cvsroot/cayenne
and 
   :ext:developername@cvs.sourceforge.net:/cvsroot/cayenne
for committers. "cayenne" folder under CVSROOT contains 4 Eclipse projects that should 
be checked out in the same workspace. You may check out all or some of them depending on 
your needs.

NOTE THAT PRIOR TO VERSION 1.2M7 EVERYTHING WAS UNDER A SINGLE ECLISPE PROJECT, 
SO IF YOU ARE CHECKING OUT AN OLDER TAG OR BRANCH, JUST CHECK OUT "cayenne" 
FOLDER AND IGNORE THE INSTRUCTIONS BELOW.


1. /README-eclipse.txt    
     
           This file explaining how to setup Eclipse.

2. /cayenne-java

           Eclipse project containing main Cayenne source and library folders. Source 
           and binary compatibility must be set to JDK 1.4.

3. /cayenne-java-1.5    

           Eclipse project containing Cayenne JDK 1.5 specific code. Requires "cayenne-java"
           project to be present in workspace. Source and binary compatibility must be set to 
           JDK 1.5.

4. /cayenne-other

           Optional Eclipse project that contains Cayenne documentation, contrib folders and such.

5. /cayenne-ant         

           Eclipse project with Ant build files to build the entire Cayenne workspace. 
           Needed if you are planning to build Cayenne with Ant outside of Eclipse
           (i.e. to create JAR files or run a test suite).



