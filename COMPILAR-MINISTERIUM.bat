@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo Ministerium 4.0 - compilacion local
 echo ============================================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0recovered-v3\tools\build_local_windows.ps1"
set "EXITCODE=%ERRORLEVEL%"

echo.
if "%EXITCODE%"=="0" (
    echo APK generada correctamente.
    echo Revisa la carpeta: %~dp0Ministerium-APK
) else (
    echo La compilacion fallo. Codigo: %EXITCODE%
    echo Copia el mensaje de error y envialo al chat para corregirlo.
)

echo.
pause
exit /b %EXITCODE%
