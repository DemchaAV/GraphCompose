@echo off
REM Закрыть Adobe Reader/Acrobat и вспомогательный процесс
taskkill /IM AcroRd32.exe /F /T >nul 2>&1
taskkill /IM Acrobat.exe  /F /T >nul 2>&1
taskkill /IM RdrCEF.exe   /F /T >nul 2>&1
exit /b 0