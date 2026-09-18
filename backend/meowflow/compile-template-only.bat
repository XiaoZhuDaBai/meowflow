@echo off
setlocal
set JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19
set PATH=%JAVA_HOME%\bin;%PATH%
set MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd

cd /d "d:\Code\喵流\backend\meowflow"

echo ===== Step 1: install common + infra =====
call "%MVN%" install -pl meowflow-common,meowflow-infra -am -DskipTests > install-common-infra.log 2>&1
echo Exit code: %ERRORLEVEL%

echo ===== Step 2: compile template =====
call "%MVN%" compile -pl meowflow-template -DskipTests > compile-template.log 2>&1
echo Exit code: %ERRORLEVEL%

endlocal
