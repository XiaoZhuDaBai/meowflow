@echo off
setlocal enabledelayedexpansion
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
set "LOG=%TEMP%\mvn-ctrl-compile.log"

echo [1] Installing meowflow-common...
call "%MVN%" install -pl meowflow-common -DskipTests >nul 2>&1
echo [1] Exit: %ERRORLEVEL%

echo [2] Compiling meowflow-user test sources...
call "%MVN%" test-compile -pl meowflow-user > "%LOG%" 2>&1
echo [2] Exit: %ERRORLEVEL%
findstr /C:"Compiling\|ERROR\|BUILD" "%LOG%" | findstr /V "downloading"
