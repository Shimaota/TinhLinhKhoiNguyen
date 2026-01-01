@echo off
chcp 65001 >nul
title Tinh Linh Khoi Nguyen - Web Edition
echo ========================================================
echo   Tinh Linh Khoi Nguyen - Web Client (TeaVM HTML5)
echo ========================================================
echo.

cd /d "%~dp0.."

if not exist "web\build\dist\js\webapp\app.js" (
    echo [1/2] No web build found, compiling with TeaVM...
    call gradlew.bat :web:gdx_teavm_web_js_build
    if errorlevel 1 (
        echo [!] Build failed. Check the log above.
        pause
        exit /b 1
    )
) else (
    echo [1/2] Web build already available.
)

echo [2/2] Starting the web client server on port 8080...
start http://localhost:8080/
node "%~dp0server.js"
pause
