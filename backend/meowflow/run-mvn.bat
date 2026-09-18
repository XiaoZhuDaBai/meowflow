@echo off
cd /d "d:\Code\喵流\backend\meowflow"
set CHCP=65001
"D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" %* > mvn_out.log 2>&1
echo Exit code: %ERRORLEVEL%
