@echo off

rem -------------------------------------------------------------------
rem     A little script to run EOModel to DataMap conversion. 
rem     Writes DataMap XML file to STDOUT, that can be redirected
rem     to a file.
rem    
rem -------------------------------------------------------------------

set CP=

if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof

:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto run_converter
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof

:run_converter
%JAVA_HOME%\bin\java -classpath %CAYENNE_HOME%\lib\cayenne.jar org.objectstyle.cayenne.tools.EOModelConverter %1 %2 %3

:eof
