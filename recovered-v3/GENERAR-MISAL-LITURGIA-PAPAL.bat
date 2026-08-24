@echo off
setlocal
cd /d "%~dp0"
title Ministerium - Generar Misal Liturgia Papal Mexico

echo.
echo ============================================================
echo   MINISTERIUM - MISAL ROMANO DESDE LITURGIA PAPAL
echo   Primera etapa: VERSION DE MEXICO ^(espanol Latinoamerica^)
echo ============================================================
echo.

where py >nul 2>nul
if errorlevel 1 (
  echo ERROR: No se encontro el lanzador de Python ^(py^).
  echo Instala Python 3.11 o ejecuta el script manualmente.
  pause
  exit /b 1
)

echo [1/3] Comprobando Python 3.11...
py -3.11 --version
if errorlevel 1 (
  echo ERROR: No se encontro Python 3.11.
  pause
  exit /b 1
)

echo.
echo [2/3] Comprobando pypdf...
py -3.11 -c "import pypdf" >nul 2>nul
if errorlevel 1 (
  echo pypdf no esta instalado. Se instalara ahora para este usuario...
  py -3.11 -m pip install --user "pypdf>=5,<6"
  if errorlevel 1 (
    echo ERROR: No se pudo instalar pypdf.
    pause
    exit /b 1
  )
)

echo.
echo [3/3] Descargando y normalizando la version de Mexico...
py -3.11 tools\build_liturgiapapal_missal.py --language es --force-download
if errorlevel 1 (
  echo.
  echo ERROR: No se pudo generar el paquete del Misal.
  echo Revisa el mensaje anterior y la conexion a Internet.
  pause
  exit /b 1
)

if not exist "app\src\main\assets\missal\manifest.json" (
  echo ERROR: El generador termino sin crear manifest.json.
  pause
  exit /b 1
)
if not exist "app\src\main\assets\missal\es\proper_ordinary.txt" (
  echo ERROR: Falta el Propio del Tiempo Ordinario de Mexico.
  pause
  exit /b 1
)

echo.
echo ============================================================
echo   LISTO.
echo   El paquete mexicano esta en app\src\main\assets\missal\es\
echo   Ahora vuelve a Android Studio y usa Build ^> Make Project.
echo ============================================================
echo.
pause
endlocal
