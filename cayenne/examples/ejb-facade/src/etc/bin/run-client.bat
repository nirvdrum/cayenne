@echo off

set LCP=.
REM Default locations of jars we depend on 
for %%i in (@jboss.home@\client\*.jar) do call lcp.bat %%i
for %%i in (..\..\lib\*.jar) do call lcp.bat %%i
for %%i in (..\client\*.jar) do call lcp.bat %%i

REM This automatically adds system classes to CLASSPATH
if exist @java.home@\lib\tools.jar   set LCP=%LCP%;@java.home@\lib\tools.jar

echo @java.home@\bin\java.exe -classpath "%LCP%" org.objectstyle.cayenne.examples.ejbfacade.Main

@java.home@\bin\java.exe -classpath "%LCP%" org.objectstyle.cayenne.examples.ejbfacade.Main
