# Estructura GitHub de Ministerium

## Repositorio de código

`murcafes94/Ministerium` debe permanecer **privado** mientras incluya contenidos cuya redistribución pública no haya sido verificada.

### Ramas

- `main`: estable. Cada punto publicado debe corresponder a una versión probada.
- `develop`: integración de la siguiente versión.
- `feature/*`: funciones nuevas.
- `fix/*`: correcciones.
- `content/*`: cambios de paquetes de contenido.

### Tags

Usar SemVer: `v3.0.0`, `v3.0.1`, `v3.1.0`, etc.

### Releases

- Release normal: canal Estable.
- Prerelease: canal Pruebas.
- Nunca sobrescribir un artefacto ya publicado bajo el mismo tag.

## Código y distribución

El repositorio de código y el canal de distribución son conceptos separados. Un repositorio privado de GitHub no ofrece por sí solo una URL anónima adecuada para que una APK instalada descargue Releases sin autenticación.

Por tanto, el manifiesto remoto y los APK/paquetes deberán vivir en un canal de distribución accesible por la app y autorizado para esos contenidos. No se debe introducir un token de GitHub en la aplicación para resolver esta limitación.
