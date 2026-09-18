@echo off
setlocal
set JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19
set PATH=%JAVA_HOME%\bin;%PATH%
set MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd

cd /d "d:\Code\喵流\backend\meowflow"

echo ===== Compile meowflow-template (skip-tests, skip-reactor-build) =====
call "%MVN%" compile -pl meowflow-template -DskipTests > template-compile.log 2>&1
echo Compile exit code: %ERRORLEVEL%
echo.
echo ===== Tail of template-compile.log =====
powershell -NoProfile -Command "Get-Content template-compile.log -Tail 100"

endlocal
exit /b %ERRORLEVEL%
