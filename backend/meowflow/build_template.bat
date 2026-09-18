@echo off
cd /d "d:\Code\喵流\backend\meowflow"
call "D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" compile -pl meowflow-template -DskipTests > build.log 2>&1
echo Exit code: %ERRORLEVEL%
