@echo off
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
"%MVN%" install -pl meowflow-common -DskipTests > "%TEMP%\mvn-install-common.log" 2>&1
echo ===== INSTALL COMMON EXIT: %ERRORLEVEL% =====
findstr /R "ERROR\|BUILD" "%TEMP%\mvn-install-common.log"
