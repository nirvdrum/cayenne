@echo off

rem -------------------------------------------------------------------
rem     Windows batch script to run Cayenne Modeler
rem -------------------------------------------------------------------


if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof

:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto run_modeler
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof

:run_modeler
start %JAVA_HOME%\bin\javaw -jar %CAYENNE_HOME%\lib\modeler\cayenne-modeler.jar %*

:eof
