@echo off
title LAN Gamepad Windows Server
cls
echo ===================================================================
echo             LAN GAMEPAD — WINDOWS CONTROLLER SERVER                
echo ===================================================================
echo.

:: Check Python installation
where python >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Python 3 is not installed or not in PATH.
    echo Please install Python 3.10+ from https://www.python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation.
    echo.
    pause
    exit /b 1
)

echo [*] Checking dependencies...
python -m pip install -q --upgrade pip
python -m pip install -q -r requirements.txt

echo.
echo [*] Starting LAN Gamepad Companion Server...
echo [*] Web Companion UI will be available at http://localhost:42102
echo.
python main.py

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Server exited unexpectedly.
    pause
)
