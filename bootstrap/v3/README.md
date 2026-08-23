# Bootstrap de Ministerium 3.0.0

Esta carpeta permite transportar temporalmente el proyecto Android completo cuando el cliente que edita el repositorio no admite subir directamente binarios grandes.

El proyecto fuente se divide en tres partes:

- `ministerium-v3.zip.part-00` — 20 MiB
- `ministerium-v3.zip.part-01` — 20 MiB
- `ministerium-v3.zip.part-02` — resto del archivo (~13 MiB)

El workflow `.github/workflows/android-build.yml` concatena las partes en orden, verifica el SHA-256 registrado en `ministerium-v3.zip.sha256`, extrae el ZIP y compila Ministerium 3.0.0 con Android SDK 30, Gradle 6.7.1, Android Gradle Plugin 4.2.1 y JDK 11.

Una vez que el proyecto completo quede importado como archivos normales del repositorio, esta carpeta bootstrap puede eliminarse.
