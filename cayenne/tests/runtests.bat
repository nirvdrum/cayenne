@echo off

if not "%JAVA_HOME%" == "" goto check_cayenne_home
echo Please define JAVA_HOME to point to your JSDK installation.
goto eof

:check_cayenne_home
if not "%CAYENNE_HOME%" == "" goto prepare_dirs
echo Please define CAYENNE_HOME to point to your Cayenne installation.
goto eof

:prepare_dirs
if exist "testrun" goto arch_old_tests
goto run_tests

:arch_old_tests
if exist "testrun.bak" rmdir "testrun.bak" /s /q
move "testrun" "testrun.bak"
if not exist "testrun" goto run_tests
echo Can not delete old tests directory 'testrun'
goto eof


:run_tests
mkdir "testrun"
cd "testrun"
%JAVA_HOME%\bin\java -jar %CAYENNE_HOME%\lib\cayenne_tests.jar %@
cd ..


:eof
