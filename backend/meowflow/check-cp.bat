@echo off
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" dependency:build-classpath -pl meowflow-user -Dmdep.outputFile="C:\Users\11057\AppData\Local\Temp\cp.txt" > "C:\Users\11057\AppData\Local\Temp\mvn-dep.log" 2>&1
echo EXIT: %ERRORLEVEL%
findstr /i "common" "C:\Users\11057\AppData\Local\Temp\cp.txt"
