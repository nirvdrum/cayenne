@echo off

rem -------------------------------------------------------------------
rem     Windows batch script to run DbGeneratorTool
rem -------------------------------------------------------------------


if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof

:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto run_modeler
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof

:run_modeler
%JAVA_HOME%\bin\java -classpath %CAYENNE_HOME%\lib\cayenne.jar org.objectstyle.cayenne.tools.DbGeneratorTool %*

:eof
