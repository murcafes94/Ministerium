# Seguridad

## Credenciales

Ministerium no debe contener tokens de escritura de GitHub ni secretos de servicios remotos dentro del APK o del repositorio.

Las claves de firma, keystores, contraseñas, credenciales OAuth y secretos de endpoints deben permanecer fuera de Git y fuera del APK cuando permitan operaciones privilegiadas.

## Reportes sensibles

No publiques como Issue información que revele credenciales, datos personales de terceros o material que permita explotar una vulnerabilidad. Los problemas de seguridad deben comunicarse de forma privada al mantenedor del repositorio.

## Actualizaciones

Toda actualización descargada por la app debe validar, como mínimo:

1. esquema y versión del manifiesto;
2. SHA-256 del artefacto;
3. firma esperada del APK cuando corresponda;
4. compatibilidad de versión y canal.

La versión anterior debe conservarse hasta completar correctamente la instalación o actualización de contenido.
