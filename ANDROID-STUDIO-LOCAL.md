# Ministerium — compilación local con Android Studio (Windows)

Este flujo sustituye GitHub Actions para las APK de prueba. GitHub sigue siendo el repositorio privado y Android Studio/Windows realiza la compilación.

## Requisitos una sola vez

- Android Studio con Android SDK Platform 30 instalado.
- JDK 11. El proyecto usa Gradle 6.7.1 y Android Gradle Plugin 4.2.1.
- Python 3.11.
- Internet en la primera compilación para dependencias de Gradle/Python y fuentes que usan los preprocesadores.
- Node.js es recomendable para ejecutar las validaciones `.mjs`, pero no es obligatorio para que Gradle compile la APK.

## Abrir el proyecto

1. Trabajar en la rama `feature/ministerium-3.1.1-final-fixes`.
2. En Android Studio elegir **Open** y seleccionar la carpeta `recovered-v3`.
3. En **Settings > Build, Execution, Deployment > Build Tools > Gradle**, usar un **Gradle JDK 11**.
4. Esperar a que Android Studio termine la sincronización inicial.

## Compilar con un doble clic

Desde la carpeta raíz del repositorio ejecutar:

`COMPILAR-MINISTERIUM.bat`

El lanzador:

- localiza Python 3.11, JDK 11 y Android SDK;
- genera los paquetes limpios de Liturgia de las Horas ES/LAT;
- genera Misal y Rituales;
- actualiza el catálogo estructural del Misal;
- genera el índice de búsqueda bíblica;
- ejecuta las validaciones Node si Node.js está disponible;
- reconstruye la clave estable de **Ministerium Test**;
- retira temporalmente del empaquetado los EPUB fuente de Liturgia y los restaura incluso si el build falla;
- ejecuta `gradlew.bat --no-daemon assembleDebug`;
- calcula SHA-256;
- copia el resultado a `Ministerium-APK/Ministerium-3.1.1-prueba.apk`.

## Si falla

No borres ni muevas archivos para intentar corregirlo. Copia desde la consola el bloque final del error y envíalo al chat. La corrección se hará sobre la misma rama del repositorio y después bastará con hacer **Pull** y volver a ejecutar `COMPILAR-MINISTERIUM.bat`.

## Pruebas directas en tablet/teléfono

Una vez que el proyecto sincronice correctamente, también se puede conectar un dispositivo Android por USB con **Depuración USB** habilitada y usar **Run ▶** desde Android Studio para iteraciones rápidas. Para una APK completa con todos los preprocesadores y controles anteriores, usar el lanzador `COMPILAR-MINISTERIUM.bat`.
