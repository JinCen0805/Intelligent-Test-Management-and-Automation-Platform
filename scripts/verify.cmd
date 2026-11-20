@echo off
rem TestPilot verification entry for Windows. CI calls scripts/verify.ps1 with pwsh.
setlocal
cd /d "%~dp0.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0verify.ps1" %*
exit /b %errorlevel%
