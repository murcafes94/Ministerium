@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Ministerium 4.1 - Generar Misal Liturgia Papal

set "ONLINE=0"
if /I "%~1"=="/online" set "ONLINE=1"

echo.
echo ============================================================
echo   MINISTERIUM 4.1 - MISAL ROMANO / MISSALE ROMANUM
echo   Liturgia Papal - generador local
echo ============================================================
echo.
if "%ONLINE%"=="0" (
  echo MODO LOCAL: no se descargara nada de Internet.
) else (
  echo MODO ONLINE AUTORIZADO: solo se descargaran PDFs que falten.
  echo No se forzara la descarga de archivos que ya esten en cache.
)
echo.

where py >nul 2>nul
if errorlevel 1 (
  echo ERROR: No se encontro el lanzador de Python ^(py^).
  echo Ministerium necesita Python 3.11 para preparar el Misal.
  pause
  exit /b 1
)

echo [1/4] Comprobando Python 3.11...
py -3.11 --version
if errorlevel 1 (
  echo ERROR: No se encontro Python 3.11.
  pause
  exit /b 1
)

echo.
echo [2/4] Comprobando pypdf...
py -3.11 -c "import pypdf" >nul 2>nul
if errorlevel 1 (
  echo.
  echo ERROR: pypdf no esta instalado.
  echo Este BAT NO lo instalara automaticamente ni abrira una conexion.
  echo Instala la dependencia una sola vez, cuando tengas Internet, con:
  echo.
  echo   py -3.11 -m pip install "pypdf^>=5,^<6"
  echo.
  pause
  exit /b 2
)
echo pypdf: OK

echo.
echo [3/4] Comprobando paquete ya generado...
if exist "app\src\main\assets\missal\manifest.json" if exist "app\src\main\assets\missal\es\initial.txt" if exist "app\src\main\assets\missal\es\proper_ordinary.txt" if exist "app\src\main\assets\missal\la\initial.txt" if exist "app\src\main\assets\missal\la\eucharistic_prayer_4.txt" goto :already_ready

set "MISSING=0"
call :checkcache "tools\cache\missal-pdf\es\initial.pdf"
call :checkcache "tools\cache\missal-pdf\es\word.pdf"
call :checkcache "tools\cache\missal-pdf\es\eucharistic_liturgy.pdf"
call :checkcache "tools\cache\missal-pdf\es\prefaces.pdf"
call :checkcache "tools\cache\missal-pdf\es\eucharistic_prayer_1.pdf"
call :checkcache "tools\cache\missal-pdf\es\eucharistic_prayer_2.pdf"
call :checkcache "tools\cache\missal-pdf\es\eucharistic_prayer_3.pdf"
call :checkcache "tools\cache\missal-pdf\es\eucharistic_prayer_4.pdf"
call :checkcache "tools\cache\missal-pdf\es\communion.pdf"
call :checkcache "tools\cache\missal-pdf\es\conclusion.pdf"
call :checkcache "tools\cache\missal-pdf\es\proper_advent.pdf"
call :checkcache "tools\cache\missal-pdf\es\proper_christmas.pdf"
call :checkcache "tools\cache\missal-pdf\es\proper_lent.pdf"
call :checkcache "tools\cache\missal-pdf\es\proper_triduum.pdf"
call :checkcache "tools\cache\missal-pdf\es\proper_easter.pdf"
call :checkcache "tools\cache\missal-pdf\es\proper_ordinary.pdf"
call :checkcache "tools\cache\missal-pdf\la\initial.pdf"
call :checkcache "tools\cache\missal-pdf\la\word.pdf"
call :checkcache "tools\cache\missal-pdf\la\eucharistic_liturgy.pdf"
call :checkcache "tools\cache\missal-pdf\la\prefaces.pdf"
call :checkcache "tools\cache\missal-pdf\la\eucharistic_prayer_1.pdf"
call :checkcache "tools\cache\missal-pdf\la\eucharistic_prayer_2.pdf"
call :checkcache "tools\cache\missal-pdf\la\eucharistic_prayer_3.pdf"
call :checkcache "tools\cache\missal-pdf\la\eucharistic_prayer_4.pdf"
call :checkcache "tools\cache\missal-pdf\la\communion.pdf"
call :checkcache "tools\cache\missal-pdf\la\conclusion.pdf"

if "%MISSING%"=="1" if "%ONLINE%"=="0" goto :need_sources

echo.
echo [4/4] Generando el Misal 4.1...
py -3.11 tools\build_liturgiapapal_missal_31.py --language all
if errorlevel 1 (
  echo.
  echo ERROR: No se pudo generar el paquete del Misal.
  if "%ONLINE%"=="0" (
    echo No se realizo ninguna descarga desde este BAT.
  ) else (
    echo Revisa el error anterior; solo se intentaron obtener PDFs que faltaban.
  )
  pause
  exit /b 1
)

if not exist "app\src\main\assets\missal\manifest.json" goto :bad_output
if not exist "app\src\main\assets\missal\es\proper_ordinary.txt" goto :bad_output
if not exist "app\src\main\assets\missal\la\initial.txt" goto :bad_output

echo.
echo ============================================================
echo   LISTO.
echo   Espanol: app\src\main\assets\missal\es\
echo   Latin:   app\src\main\assets\missal\la\
echo   Ya puedes compilar Ministerium 4.1.
echo ============================================================
echo.
pause
exit /b 0

:already_ready
echo Paquete del Misal 4.1 ya presente: no hay nada que descargar.
echo Espanol y Latin estan disponibles localmente.
echo.
pause
exit /b 0

:need_sources
echo.
echo ============================================================
echo   FALTAN PDFs FUENTE EN LA CACHE LOCAL
echo ============================================================
echo.
echo El generador antiguo intentaba descargarlos automaticamente.
echo Esta version NO se conectara sin tu permiso.
echo.
echo Si quieres descargar SOLO los archivos que faltan, ejecuta:
echo.
echo   GENERAR-MISAL-LITURGIA-PAPAL.bat /online
echo.
echo Los PDFs que ya existan en tools\cache\missal-pdf\ se reutilizaran.
echo No se usa --force-download.
echo.
pause
exit /b 3

:bad_output
echo.
echo ERROR: El generador termino, pero faltan archivos requeridos del Misal.
echo Revisa app\src\main\assets\missal\ y el mensaje anterior.
echo.
pause
exit /b 4

:checkcache
if not exist "%~1" set "MISSING=1"
exit /b 0
