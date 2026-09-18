@echo off
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"

echo ===== Building meowflow-common first =====
"%MVN%" -pl meowflow-common install -DskipTests
if %ERRORLEVEL% neq 0 (
    echo FAILED: meowflow-common build failed
    exit /b 1
)

echo ===== Building all modules =====
"%MVN%" install -DskipTests
if %ERRORLEVEL% neq 0 (
    echo FAILED: Full build failed
    exit /b 1
)

echo ===== BUILD SUCCESSFUL =====
exit /b 0
