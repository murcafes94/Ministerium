# Arquitectura de actualizaciones

Ministerium separa las actualizaciones de la aplicación de las actualizaciones de contenido.

## Aplicación

El manifiesto de Release contiene:

- `versionName`;
- `versionCode`;
- canal (`stable` o `testing`);
- URL del APK;
- SHA-256 del APK;
- huella SHA-256 de la firma;
- versión mínima de Android;
- changelog;
- referencia al manifiesto de paquetes.

Flujo previsto:

`consultar manifiesto → comparar versión → descargar → verificar hash/firma → solicitar instalación de Android`.

No se presupone instalación silenciosa.

## Contenido

Los paquetes se versionan independientemente, por ejemplo:

- `calendar-ec`;
- `breviarium`;
- `lectionary`;
- `rituals`.

Flujo previsto:

`descargar → validar hash y esquema → instalar en staging → validar → activar → conservar anterior hasta éxito`.

Ante un fallo, el paquete anterior continúa activo.

## Canales

- `stable`: contenido probado.
- `testing`: contenido previo para pruebas.

Los manifiestos incluidos en este repositorio son plantillas. No deben considerarse endpoints públicos hasta configurar un canal de distribución real.
