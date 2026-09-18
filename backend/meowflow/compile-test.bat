@echo off
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
"%MVN%" test-compile -pl meowflow-common,meowflow-user -am > "%TEMP%\mvn-testcompile.log" 2>&1
echo EXIT: %ERRORLEVEL%
findstr /R "ERROR\|Compiling\|BUILD\|common\|classpath" "%TEMP%\mvn-testcompile.log" | findstr /V "downloading\|Downloading" | head -50
