@echo off
REM MeowFlow API Test Suite (Simple Batch)
REM Run: run-api-tests.cmd

echo ========================================
echo   MeowFlow API Test Suite
echo   Time: %date% %time%
echo ========================================
echo.

REM Login
echo [1/12] Auth - Login...
curl -s -X POST "http://127.0.0.1:8081/api/v1/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"meow@2026\"}" > %TEMP%\login_resp.json
findstr /C:"\"code\":200" %TEMP%\login_resp.json >nul
if %errorlevel%==0 (
    echo   [PASS] Login
) else (
    echo   [FAIL] Login
)

REM Get Token from response
for /f "tokens=2 delims=:," %%a in ('findstr "accessToken" %TEMP%\login_resp.json') do set TOKEN=%%a
set TOKEN=%TOKEN:"=%
set TOKEN=%TOKEN: =%
echo Token: %TOKEN%

REM Current User
echo [2/12] User - Get Current User...
curl -s -X GET "http://127.0.0.1:8081/api/v1/auth/me" ^
  -H "Authorization: Bearer %TOKEN%" | findstr /C:"\"code\":200" >nul
if %errorlevel%==0 (
    echo   [PASS] Get Current User
) else (
    echo   [FAIL] Get Current User
)

REM Permissions
echo [3/12] Auth - Get Permissions...
curl -s -X GET "http://127.0.0.1:8081/api/v1/auth/permissions" ^
  -H "Authorization: Bearer %TOKEN%" > %TEMP%\perm_resp.json
findstr /C:"\"code\":200" %TEMP%\perm_resp.json >nul
if %errorlevel%==0 (
    for /f %%a in ('findstr /C:"permissions" %TEMP%\perm_resp.json ^| find /c ":"') do echo   [PASS] Permissions Count: %%a
) else (
    echo   [FAIL] Get Permissions
)

REM Workflow List
echo [4/12] Workflow - List...
curl -s -X GET "http://127.0.0.1:8080/workflow/api/v1/workflows" ^
  -H "Authorization: Bearer %TOKEN%" | findstr /C:"\"code\":200" >nul
if %errorlevel%==0 (
    echo   [PASS] List Workflows
) else (
    echo   [FAIL] List Workflows
)

REM Workflow Categories
echo [5/12] Workflow - Categories...
curl -s -X GET "http://127.0.0.1:8080/workflow/api/v1/categories" ^
  -H "Authorization: Bearer %TOKEN%" > %TEMP%\cat_resp.json
findstr /C:"\"code\":200" %TEMP%\cat_resp.json >nul
if %errorlevel%==0 (
    echo   [PASS] List Categories
) else (
    echo   [FAIL] List Categories
)

REM Template List
echo [6/12] Template - List...
curl -s -X GET "http://127.0.0.1:8080/template/api/v1/templates" ^
  -H "Authorization: Bearer %TOKEN%" > %TEMP%\tpl_resp.json
findstr /C:"\"code\":200" %TEMP%\tpl_resp.json >nul
if %errorlevel%==0 (
    echo   [PASS] List Templates
) else (
    echo   [FAIL] List Templates
)

REM Template Tags
echo [7/12] Template - Tags...
curl -s -X GET "http://127.0.0.1:8080/template/api/v1/tags" ^
  -H "Authorization: Bearer %TOKEN%" > %TEMP%\tag_resp.json
findstr /C:"\"code\":200" %TEMP%\tag_resp.json >nul
if %errorlevel%==0 (
    echo   [PASS] List Tags
) else (
    echo   [FAIL] List Tags
)

REM Alert Rules
echo [8/12] Alert - Rules...
curl -s -X GET "http://127.0.0.1:8080/monitor/api/v1/alerts/rules" ^
  -H "Authorization: Bearer %TOKEN%" | findstr /C:"\"code\":200" >nul
if %errorlevel%==0 (
    echo   [PASS] List Alert Rules
) else (
    echo   [FAIL] List Alert Rules
)

REM AI Models
echo [9/12] AI - Models...
curl -s -X GET "http://127.0.0.1:8080/infra/api/v1/models" ^
  -H "Authorization: Bearer %TOKEN%" > %TEMP%\model_resp.json
findstr /C:"\"code\":200" %TEMP%\model_resp.json >nul
if %errorlevel%==0 (
    echo   [PASS] List Models
) else (
    echo   [FAIL] List Models
)

REM Knowledge Base
echo [10/12] KB - List...
curl -s -X GET "http://127.0.0.1:8080/infra/api/v1/knowledge" ^
  -H "Authorization: Bearer %TOKEN%" > %TEMP%\kb_resp.json
findstr /C:"\"code\":200" %TEMP%\kb_resp.json >nul
if %errorlevel%==0 (
    echo   [PASS] List Knowledge Bases
) else (
    echo   [FAIL] List Knowledge Bases
)

REM Integration
echo [11/12] Integration - List...
curl -s -X GET "http://127.0.0.1:8080/infra/api/v1/integrations" ^
  -H "Authorization: Bearer %TOKEN%" | findstr /C:"\"code\":200" >nul
if %errorlevel%==0 (
    echo   [PASS] List Integrations
) else (
    echo   [FAIL] List Integrations
)

REM Executor Health
echo [12/12] Health - Executor...
curl -s -X GET "http://127.0.0.1:8080/executor/api/v1/health" ^
  -H "Authorization: Bearer %TOKEN%" | findstr /C:"UP" >nul
if %errorlevel%==0 (
    echo   [PASS] Executor Health
) else (
    echo   [FAIL] Executor Health
)

REM Cleanup
del %TEMP%\login_resp.json %TEMP%\perm_resp.json %TEMP%\cat_resp.json %TEMP%\tpl_resp.json %TEMP%\tag_resp.json %TEMP%\model_resp.json %TEMP%\kb_resp.json 2>nul

echo.
echo ========================================
echo   Test Complete
echo ========================================
pause
