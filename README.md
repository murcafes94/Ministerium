# Ministerium 4.0

Repositorio privado de **Ministerium**, aplicación Android *offline first* para
oración, Liturgia de las Horas, Misa, Biblia, Magisterio, derecho canónico y
estudio personal.

El proyecto Android editable está en `recovered-v3/`. La rama de la versión es
`feature/ministerium-4.0`.

## Compilación local

En Windows, ejecuta `COMPILAR-MINISTERIUM.bat` desde esta carpeta. El proceso:

- detecta JDK 11 y Android SDK;
- genera los contenidos limpios y los índices locales;
- valida calendario, Leccionario OLM, Magisterio, oración y lectores;
- compila la APK de prueba;
- deja la APK y su SHA-256 en `Ministerium-APK/`.

Consulta `recovered-v3/README.md` para los requisitos y el detalle de la
versión 4.0.

## Principios

- La aplicación y los paquetes de contenido se mantienen de forma modular.
- Los textos litúrgicos se validan contra sus fuentes; no se inventan perícopas
  ni rúbricas.
- Intenciones, oraciones, notas y estudios permanecen locales salvo exportación
  expresa del usuario.
- TTS se ofrece en español; los textos latinos se leen visualmente.
- El APK no contiene tokens de escritura ni claves privadas de producción.
- Antes de distribuir material protegido deben verificarse los permisos.
