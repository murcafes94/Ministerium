# Guía para Android Studio 4.2.1

Esta edición de Ministerium está preparada específicamente para una laptop con
**Windows 10 de 64 bits, 8 GB de RAM, procesador Intel i3 de 5.ª generación,
disco HDD y Android Studio 4.2.1**.

No uses con Android Studio 4.2.1 el paquete anterior configurado para Ladybug.
Utiliza solamente el ZIP cuyo nombre termina en `AS-4.2.1`.

## 1. Instalar el SDK compatible

En la pantalla inicial de Android Studio 4.2.1:

1. Abre **Configure > SDK Manager**.
2. En **SDK Platforms**, marca **Android 11.0 (R)**, cuyo nivel es **API 30**.
3. Abre la pestaña **SDK Tools**.
4. Marca **Show Package Details**.
5. Dentro de **Android SDK Build-Tools**, marca la versión **30.0.2**.
6. Verifica que también estén marcados **Android SDK Platform-Tools** y
   **Android SDK Tools** o **Android SDK Command-line Tools** si aparecen.
7. No instales Android Emulator ni imágenes de teléfonos virtuales: en esta
   laptop consumirían demasiada memoria y trabajarían lentamente sobre el HDD.
8. Pulsa **Apply**, acepta las licencias y espera a que termine.

Que también esté instalado Android 36 no causa ningún problema; el proyecto usa
API 30 porque es la versión compatible con Android Studio 4.2.1.

## 2. Colocar y abrir el proyecto

1. Crea la carpeta `C:\Android`.
2. Descomprime el ZIP dentro de ella.
3. Debe existir el archivo:

   `C:\Android\Ministerium\settings.gradle`

4. En Android Studio pulsa **Open an Existing Project**.
5. Selecciona exactamente `C:\Android\Ministerium`.
6. Si pregunta si confías en el proyecto, acepta.
7. Espera sin cerrar el programa. La primera sincronización descargará Gradle
   6.7.1 y el complemento de Android 4.2.1; en un HDD puede tardar varios minutos.

## 3. Configuración para 8 GB de RAM

El proyecto ya aplica estas limitaciones:

- Máximo de 1536 MB para Gradle.
- Recolector de memoria recomendado para el JDK 11 de Android Studio 4.2.1.
- Compilación paralela desactivada.
- Máximo de dos trabajadores.

Mientras compile, cierra el navegador y otros programas pesados. No ejecutes
Android Studio como administrador durante el uso cotidiano; abre normalmente
`studio64.exe`.

## 4. Preparar el teléfono

1. En el teléfono abre **Ajustes > Acerca del teléfono**.
2. Toca siete veces **Número de compilación** o **Versión del sistema operativo**.
3. Entra en **Opciones de desarrollador** y activa **Depuración USB**.
4. Conecta el teléfono con un cable que permita datos.
5. Selecciona **Transferencia de archivos**.
6. Acepta el mensaje **Permitir depuración USB**.

No utilizaremos emulador. Android Studio puede instalar la app directamente en
el teléfono; si no tienes cable, también puedes generar el APK y transferirlo
después por otro medio.

## 5. Ejecutar Ministerium

1. Espera a que el teléfono aparezca en la barra superior de Android Studio.
2. Selecciónalo.
3. Pulsa **Run ▶**.
4. La primera compilación será la más lenta.

La aplicación de ApkCreator y la nativa pueden coexistir. Cuando confirmes que
la nativa funciona, puedes desinstalar la anterior para evitar dos iconos con el
mismo nombre.

## 6. Generar el APK

1. Abre **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. Espera el mensaje de compilación correcta.
3. El APK de prueba quedará en:

   `app\build\outputs\apk\debug\app-debug.apk`

## 7. Modificar el diseño

Sí puedes cambiar el diseño desde Android Studio. En el panel **Project**, abre
`app > src > main > res > layout`. Los archivos principales son:

- `activity_main.xml`: portada y tarjetas de acceso.
- `activity_hours_today.xml`: pantalla diaria de la Liturgia de las Horas.
- `activity_hours_reader.xml`: lector de cada Hora.
- `activity_settings.xml`: selector de tema y siete recordatorios independientes.
- `activity_bilingual_hours.xml`: selección por fecha de la edición bilingüe.
- `activity_bilingual_reader.xml`: columnas española y latina de cada Hora.
- `activity_latin_hours.xml`: edición latina organizada por fecha y Hora.
- `activity_mass_readings.xml`: Misal y actualización mensual de lecturas.
- `activity_mass_reading_reader.xml`: lector limpio de las lecturas guardadas.
- `activity_liturgical_calendar.xml`: calendario mensual de Ecuador 2026.

Puedes usar las pestañas **Design** y **Code** del editor. Los colores están en
`res/values/colors.xml` y los del modo oscuro en
`res/values-night/colors.xml`. Conserva los identificadores que comienzan por
`@+id/`, porque las clases Java los usan para abrir pantallas y responder a los
botones. Después de cada cambio ejecuta **Build > Make Project**.

## 8. Soluciones rápidas

### Error `failed to find target android-30`

Instala Android 11 (API 30) desde **Configure > SDK Manager**.

### Error relacionado con Java o Gradle

Abre **File > Project Structure > SDK Location** y selecciona **Use embedded
JDK**. Android Studio 4.2.1 incluye JDK 11.

### El teléfono no aparece

Cambia el modo USB a Transferencia de archivos, acepta nuevamente la autorización
de depuración y prueba otro cable o puerto USB.

No aceptes propuestas automáticas para actualizar Gradle o el complemento de
Android. Las versiones 4.2.1 y 6.7.1 están emparejadas para este proyecto.
