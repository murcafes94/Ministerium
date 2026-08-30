@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo Ministerium 4.1 - compilacion local OFFLINE
echo ============================================================
echo.
echo No se regenerara contenido, no se ejecutara pip y no se
echo consultaran calendarios ni fuentes por Internet.
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0recovered-v3\tools\build_local_offline_41.ps1" -Offline
set "EXITCODE=%ERRORLEVEL%"

echo.
if "%EXITCODE%"=="0" (
    echo APK generada correctamente.
    echo Revisa la carpeta: %~dp0Ministerium-APK
) else (
    echo La compilacion fallo. Codigo: %EXITCODE%
    echo.
    echo Si el mensaje dice que falta una dependencia de Gradle en cache,
    echo usa una sola vez COMPILAR-MINISTERIUM-CON-RED.bat.
    echo Para cualquier otro error, copia el mensaje y envialo al chat.
)

echo.
pause
exit /b %EXITCODE%
