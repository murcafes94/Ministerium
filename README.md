# Ministerium 3.0

**Ministerium** es una aplicación Android de apoyo litúrgico, bíblico, canónico y pastoral. Este repositorio privado contiene el código fuente y la infraestructura técnica del proyecto.

> Estado actual: preparación de la arquitectura de Ministerium 3.0 a partir de la rama 2.3.2.

## Principios del proyecto

- La aplicación y los paquetes de contenido se actualizan de forma independiente.
- Se mantienen canales separados **Estable** y **Pruebas**.
- Las distribuciones deben incluir versión, fecha, hash SHA-256 y compatibilidad mínima.
- El APK no debe contener tokens de escritura de GitHub ni credenciales privadas.
- El feedback de usuarios debe pasar por un endpoint seguro antes de crear Issues.
- Los textos litúrgicos, bíblicos o editoriales sujetos a derechos de autor no deben publicarse en distribuciones públicas sin verificar los permisos correspondientes.

## Componentes previstos

- Liturgia de las Horas
- Misal y lecturas de la Misa
- Biblia y herramientas de estudio
- Código de Derecho Canónico
- Magisterio
- Rituales y devociones
- Reflexiones, notas y oraciones personales
- Lectura en voz alta mediante TTS cuando corresponda

## Arquitectura de actualizaciones

Ministerium 3.0 separa las actualizaciones en módulos:

1. **Aplicación (APK)**
2. **Calendario litúrgico**
3. **Breviarium / Liturgia de las Horas**
4. **Leccionario**
5. **Rituales y otros contenidos**

Los manifiestos de distribución se encuentran en `distribution/manifests/`.

## Canales

- `stable.json`: versiones recomendadas para uso ordinario.
- `testing.json`: versiones de prueba antes de pasar al canal estable.

## Documentación

- `docs/ARCHITECTURE_3_0.md` — organización funcional de Ministerium 3.0.
- `docs/UPDATE_SYSTEM.md` — diseño del sistema de actualizaciones y distribución.
- `FEEDBACK.md` — esquema para consolidación de incidencias y sugerencias.

## Seguridad

Nunca deben almacenarse en el APK:

- Personal Access Tokens de GitHub.
- Secrets de Actions.
- Credenciales de escritura del repositorio.
- Claves privadas de servicios externos.

Las operaciones de escritura deben realizarse mediante un servicio intermedio autenticado y con permisos mínimos.

## Estado de desarrollo

La base funcional proviene de **Ministerium 2.3.2**. La versión 3.0 se está reorganizando para permitir mantenimiento modular, distribución controlada y actualización independiente de los distintos contenidos.
