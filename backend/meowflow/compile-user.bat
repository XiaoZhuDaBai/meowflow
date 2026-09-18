@echo off
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
"%MVN%" -pl meowflow-common,meowflow-user -am test-compile -DskipTests -q 2> "%TEMP%\mvn-user-tc.log"
echo ===== TEST COMPILE EXIT: %ERRORLEVEL% =====
type "%TEMP%\mvn-user-tc.log" | findstr /R "ERROR" | head -200
