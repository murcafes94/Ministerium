# Ministerium 4.0 — compilación local con Android Studio (Windows)

Este flujo sustituye GitHub Actions cuando solo se necesita generar una APK de prueba. GitHub continúa como repositorio privado y Android Studio/Windows realiza la compilación.

## Requisitos una sola vez

- Android Studio con Android SDK Platform 30 instalado.
- JDK 11. El proyecto usa Gradle 6.7.1 y Android Gradle Plugin 4.2.1.
- Python 3.11.
- Internet en la primera compilación para dependencias de Gradle/Python y las fuentes que usan los preprocesadores.
- Node.js es recomendable para ejecutar todos los contratos de validación 4.0; Gradle puede compilar sin Node.

La instalación portable de Android Studio usada durante las pruebas también está soportada. El compilador reconoce `C:\portapps\android-studio-portable\data\sdk` cuando allí se encuentra el SDK.

## Abrir el proyecto

1. Trabajar en la rama `feature/ministerium-4.0`.
2. En Android Studio elegir **Open** y seleccionar únicamente la carpeta `recovered-v3`.
3. Configurar **JDK 11** para Gradle. Con Android Studio antiguo puede aparecer en **File > Project Structure > SDK Location > JDK location** en vez de existir un selector "Gradle JDK".
4. Esperar a que termine la sincronización inicial. La primera descarga de Gradle 6.7.1 puede tardar.

## Compilar la versión completa

Desde la carpeta raíz del repositorio ejecuta:

`COMPILAR-MINISTERIUM.bat`

El lanzador:

- localiza Python 3.11, JDK 11 y Android SDK y comprueba que Gradle use JVM 11;
- instala `pypdf` y `beautifulsoup4` si faltan;
- genera los paquetes limpios de Liturgia de las Horas ES/LAT;
- genera Misal, Rituales y catálogo estructural del Misal;
- genera los índices de búsqueda bíblica y de Magisterio;
- ejecuta las validaciones base y los contratos 4.0 si Node.js está disponible;
- reconstruye la clave estable de **Ministerium Test**;
- retira temporalmente del APK los EPUB fuente de Liturgia y los restaura incluso si el build falla;
- ejecuta `gradlew.bat --no-daemon assembleDebug`;
- calcula SHA-256;
- copia el resultado a `Ministerium-APK/Ministerium-4.0.0-prueba.apk`.

## Prueba rápida de Java y Gradle

Desde `recovered-v3`:

```bat
gradlew.bat --version
```

La salida correcta debe indicar **Gradle 6.7.1** y **JVM 11.x**.

## Si falla

No borres el proyecto ni regeneres archivos al azar. Conserva el bloque final desde la primera línea `ERROR`, `FAILURE` o `Exception`. La corrección debe hacerse sobre `feature/ministerium-4.0`; después basta actualizar la rama y volver a ejecutar `COMPILAR-MINISTERIUM.bat`.

## Pruebas directas en tablet/teléfono

Una vez sincronizado el proyecto, puede conectarse un dispositivo Android por USB con **Depuración USB** habilitada y usarse **Run ▶** para iteraciones rápidas. Para una APK completa, con preprocesadores, índices y controles de integridad, usa siempre `COMPILAR-MINISTERIUM.bat`.
