@echo off
setlocal enabledelayedexpansion
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
set "LOG=%TEMP%\mvn-user-ctrl3.log"

call "%MVN%" test -pl meowflow-user > "%LOG%" 2>&1
echo Exit: %ERRORLEVEL%
findstr /C:"Tests run:" /C:"BUILD SUCCESS" /C:"BUILD FAILURE" "%LOG%"
