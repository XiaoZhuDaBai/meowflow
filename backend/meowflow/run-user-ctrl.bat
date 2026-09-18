@echo off
set "JAVA_HOME=C:\Users\11057\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MVN=D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
"%MVN%" -pl meowflow-user test -Dtest='UserControllerTest+AuthControllerTest+LoginLogControllerTest+PermissionControllerTest+RoleControllerTest+CaptchaControllerTest+OrgControllerTest+AuditLogControllerTest' -Dsurefire.failIfNoSpecifiedTests=false > "%TEMP%\mvn-user-ctrl.log" 2>&1
echo ===== TEST EXIT: %ERRORLEVEL% =====
findstr /R "Tests run:\|Caused by:\|BUILD" "%TEMP%\mvn-user-ctrl.log"
