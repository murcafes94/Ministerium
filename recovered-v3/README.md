# Ministerium 3.0.0

Aplicación Android nativa, editorial y *offline first* para oración, liturgia,
Biblia, estudio y biblioteca católica. Conserva compatibilidad con **Android
Studio 4.2.1**, Android Gradle Plugin 4.2.1, Gradle 6.7.1 y SDK 30.

## Abrir el proyecto

Lee `GUIA-INICIO-ANDROID-STUDIO.md`. Abre la carpeta que contiene
`settings.gradle`, espera la primera sincronización y ejecuta en Android 6.0 o
superior. Gradle está limitado a 1536 MB y dos trabajadores.

## Ministerium 3.0

- Lectores compactos sin barras inferiores: cabecera autoocultable, gestos,
  pellizco para ajustar texto, tema rápido y menú global.
- Menú contextual explícito para copiar, compartir, resaltar, anotar, meditar,
  consultar, traducir y escuchar. Seleccionar no abre nada automáticamente.
- `Mi estudio`, autoguardado, Mesa de estudio, referencias y búsquedas precisas
  de Biblia, Canon y Catecismo.
- Continuar leyendo con módulo, documento y posición.
- Completas continuas; Horas intermedias con himno automático y salmodia
  complementaria; conclusiones legítimas.
- Oficio de lecturas + Laudes conforme OGLH 99 y recorrido Misa +
  Laudes/Vísperas conforme OGLH 93–97.
- Misal y Lecturas separados, con selectores de modo e idioma Español o
  Latín–Español.
- Liturgia bilingüe reducida a `Bilingüe | Latín`; sin TTS latino.
- Planes bíblicos personalizados con progreso real y avisos directos.
- TTS español por bloques semánticos y controles en notificación.
- Respaldo SHA-256 restaurable, con Drive opcional mediante el proveedor seguro
  de documentos de Android.
- Centro modular de versiones y comentarios sin tokens dentro de la APK.

El contenido incluye Liturgia de las Horas, Liturgia Horarum 2026, Misal
LAT–ES, Leccionario, calendario Ecuador 2026, Biblia de Jerusalén, cuatro
diccionarios, Código canónico bilingüe, Magisterio, rituales y devociones.

## Validación

```text
node tools/validate_project.mjs
node tools/validate_content.mjs
```

La compilación completa requiere SDK 30 y Gradle 6.7.1 descargados por Android
Studio. Genera la APK desde `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## Distribución responsable

No publiques material protegido sin comprobar permisos. `distribution` aporta
solo plantillas: cada Release debe completar URLs, firma y hashes reales.

## Flujo del repositorio

- `main`: versión estable.
- `develop`: integración de la siguiente versión.
- Releases normales: Estable.
- Prereleases: Pruebas.

Consulta `docs/GITHUB-STRUCTURE.md`, `docs/RELEASE-PROCESS.md` y
`docs/CONTENT-RIGHTS.md` antes de publicar artefactos o cambiar la visibilidad
del repositorio.
