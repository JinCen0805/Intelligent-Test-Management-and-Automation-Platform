@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-local.ps1"
if errorlevel 1 (
  echo.
  echo Project startup failed. Review the message above and run scripts\setup-local.ps1 if needed.
  pause
)
endlocal
