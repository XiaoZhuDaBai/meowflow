@echo off
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"

echo ===== Starting meowflow-executor on port 8083 =====
cd /d "%~dp0"
"%MVN%" -pl meowflow-executor spring-boot:run -Dmaven.test.skip=true
