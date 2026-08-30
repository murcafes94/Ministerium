# Proceso de Release

## 1. Preparación

1. Integrar cambios en `develop`.
2. Ejecutar los validadores de proyecto y contenido.
3. Compilar e instalar en un dispositivo real.
4. Probar actualización desde la versión estable anterior y comprobar que se preservan datos personales.
5. Integrar en `main` únicamente después de superar QA.

## 2. Versionado

Actualizar de manera coherente:

- `versionName`;
- `versionCode`;
- `NOVEDADES-<versión>.md`;
- `CHANGELOG.md`;
- manifiesto de Release.

## 3. APK

Generar APK firmado fuera del repositorio. Calcular:

```text
sha256sum Ministerium-<versión>.apk
```

Registrar también la huella SHA-256 de la firma que la app espera.

## 4. Publicación

- Tag: `vX.Y.Z`.
- Estable: GitHub Release normal o canal de distribución estable.
- Pruebas: GitHub Prerelease o canal `testing`.
- Adjuntar APK y changelog solo cuando los derechos de distribución lo permitan.

## 5. Manifiesto

Partir de `distribution/release-manifest.template.json` y sustituir todos los marcadores `REEMPLAZAR`. No publicar un manifiesto con hashes, URL o firma ficticios.

## 6. Restaurar una versión estable

Android no debe recibir un downgrade de `versionCode`. Si una versión debe revertirse, publicar una nueva compilación con `versionCode` superior cuyo código/contenido vuelva al comportamiento estable anterior.
