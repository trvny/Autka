@echo off
setlocal

where powershell.exe >nul 2>&1
if errorlevel 1 (
  echo PowerShell was not found.
  pause
  exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-release-keystore.ps1"
set "exit_code=%errorlevel%"

echo.
if not "%exit_code%"=="0" echo Setup failed with exit code %exit_code%.
pause
exit /b %exit_code%
