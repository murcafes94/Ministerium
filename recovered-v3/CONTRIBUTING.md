# Contribuir a Ministerium

Ministerium usa un flujo simple y conservador:

- `main`: versión estable y reproducible.
- `develop`: integración de cambios ya revisados.
- ramas de trabajo: `feature/<tema>`, `fix/<tema>` o `content/<paquete>`.

No se debe hacer desarrollo cotidiano directamente sobre `main`.

## Antes de proponer cambios

Ejecuta:

```text
node tools/validate_project.mjs
node tools/validate_content.mjs
```

Cuando el entorno Android esté disponible, compila además el proyecto antes de integrar cambios.

## Contenidos y derechos

El repositorio privado contiene materiales de terceros necesarios para el uso actual de la aplicación. No conviertas el repositorio ni sus Releases en públicos sin revisar primero `docs/CONTENT-RIGHTS.md` y los permisos de cada obra.

## Seguridad

Nunca confirmes en Git:

- tokens de GitHub;
- claves API;
- `local.properties`;
- claves de firma, keystores o contraseñas;
- credenciales de Google Drive u otros servicios.

Los APK/AAB se publican como artefactos de Release, no como archivos fuente versionados.
