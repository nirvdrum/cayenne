rem @echo off

rem -------------------------------------------------------------------
rem     Windows batch script to run Cayenne Regression
rem
rem  Certain parts are modeled after Tomcat startup scrips, 
rem  Copyright Apache Software Foundation
rem -------------------------------------------------------------------

rem -------------------------------------------------------------------
rem  Variables:
rem -------------------------------------------------------------------

set MAIN_CLASS=org.objectstyle.cayenne.regression.Main


if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof


:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto got_home
set CAYENNE_HOME=..

:got_home
if exist "%CAYENNE_HOME%\bin\regression.bat" goto check_cp
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof


:check_cp
set JAVACMD=%JAVA_HOME%\bin\java
set OPTIONS=-Xms60m -Xmx60m -classpath %CAYENNE_HOME%\lib\regression\cayenne-regression.jar
if "%CLASSPATH%" == "" goto run_regression
set OPTIONS=%OPTIONS%;%CLASSPATH%
goto run_regression

:run_regression
%JAVACMD% %OPTIONS% %MAIN_CLASS%  %*

:eof
