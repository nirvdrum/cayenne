@echo off

rem -------------------------------------------------------------------
rem     Windows batch script to run Cayenne Modeler
rem
rem  Certain parts are modeled after Tomcat startup scrips, 
rem  Copyright Apache Software Foundation
rem -------------------------------------------------------------------


if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof


:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto got_home
set CAYENNE_HOME=..

:got_home
if exist "%CAYENNE_HOME%\bin\modeler.bat" goto run_modeler
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof

:run_modeler
start %JAVA_HOME%\bin\javaw -jar %CAYENNE_HOME%\lib\modeler\cayenne-modeler.jar %*

:eof
