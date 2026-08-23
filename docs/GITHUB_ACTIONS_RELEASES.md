# Compilación y distribución con GitHub Actions

Ministerium 3.0 utiliza GitHub Actions para compilar el proyecto Android y preparar APK de distribución.

## Flujos incluidos

### `android-build.yml`

Se ejecuta al modificar el proyecto Android en `main`, en Pull Requests o manualmente. Compila `assembleDebug` y conserva el APK como artefacto temporal de GitHub Actions.

### `android-release.yml`

Se ejecuta al crear un tag con formato `vX.Y.Z`. Compila `assembleRelease`, firma el APK usando secrets del repositorio, calcula SHA-256 y publica una GitHub Release con:

- `Ministerium-X.Y.Z.apk`
- `Ministerium-X.Y.Z.apk.sha256`
- `update.json`

## Firma de producción

La keystore de producción nunca debe almacenarse en el repositorio.

Añadir en GitHub Repository Settings > Secrets and variables > Actions los siguientes secrets:

- `KEYSTORE_BASE64`: keystore JKS codificada en Base64.
- `KEYSTORE_PASSWORD`: contraseña de la keystore.
- `KEY_ALIAS`: alias de la clave.
- `KEY_PASSWORD`: contraseña de la clave.

El proyecto Android debe leer estas variables de entorno para configurar `signingConfigs.release`:

- `MINISTERIUM_KEYSTORE_FILE`
- `MINISTERIUM_KEYSTORE_PASSWORD`
- `MINISTERIUM_KEY_ALIAS`
- `MINISTERIUM_KEY_PASSWORD`

## Crear una versión

Ejemplo:

```bash
git tag v3.0.0
git push origin v3.0.0
```

GitHub Actions compilará y publicará la Release automáticamente si el proyecto Android y los secrets de firma están correctamente configurados.

## Distribución de actualizaciones

Para actualización directa desde la aplicación, se recomienda mantener el código fuente privado y usar un canal de distribución accesible por la aplicación sin incluir tokens de escritura en el APK. Puede ser un repositorio público separado dedicado únicamente a APK, manifiestos y paquetes que puedan distribuirse legalmente.

Mientras `murcafes94/Ministerium` permanezca privado, no debe incrustarse un token personal de GitHub dentro de la app para descargar Releases privadas.
