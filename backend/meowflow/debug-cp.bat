@echo off
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
"%MVN%" test-compile -pl meowflow-common,meowflow-user -am -X > "%TEMP%\mvn-debug.log" 2>&1
findstr /C:"classpath\|test-classes\|GlobalException" "%TEMP%\mvn-debug.log" > "%TEMP%\mvn-classpath.log"
type "%TEMP%\mvn-classpath.log"
