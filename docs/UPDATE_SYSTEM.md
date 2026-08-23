# Sistema de actualizaciones — Ministerium 3.0

## Objetivo

Permitir que Ministerium actualice el APK y los contenidos de forma independiente, controlada y verificable.

## Canales

Se contemplan dos canales iniciales:

- **stable**: versiones recomendadas para uso ordinario.
- **testing**: versiones destinadas a validación previa.

Cada canal tendrá su propio manifiesto en `distribution/manifests/`.

## Tipos de actualización

### Aplicación

Distribuye nuevas versiones del APK.

### Contenido

Distribuye paquetes independientes para calendario, breviario, leccionario, rituales u otros módulos.

## Campos mínimos por artefacto

- `id`
- `version`
- `publishedAt`
- `minAppVersion`
- `downloadUrl`
- `sha256`
- `sizeBytes`
- `notes`

## Flujo recomendado

1. Preparar el artefacto.
2. Calcular SHA-256.
3. Publicar el artefacto en el mecanismo de distribución correspondiente.
4. Actualizar primero `testing.json`.
5. Validar instalación y compatibilidad.
6. Promover los mismos metadatos a `stable.json`.

## Seguridad

La app puede realizar lecturas anónimas o autenticadas de recursos de distribución según la arquitectura elegida, pero nunca debe contener credenciales con permisos de escritura sobre GitHub.

Para feedback o creación automática de incidencias, debe existir un servicio intermedio que valide la petición y utilice credenciales guardadas del lado del servidor.

## Integridad

Antes de instalar o importar un paquete, la app debe verificar el SHA-256. Si no coincide, la actualización debe rechazarse.

## Rollback

Los manifiestos deben conservar la versión conocida como estable y permitir volver a una versión previa si una actualización introduce un problema crítico.
