@echo off

if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof

:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto run_tests
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof

:run_tests
%JAVA_HOME%\bin\java -jar %CAYENNE_HOME%\lib\cayenne-performance.jar %@
cd ..


:eof
