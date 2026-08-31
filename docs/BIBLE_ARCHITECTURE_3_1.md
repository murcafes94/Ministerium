# Arquitectura Biblia — Ministerium 3.1

## Objetivo

Migrar el módulo Biblia desde un lector centrado en archivos/páginas hacia un motor bíblico semántico, offline y modular. El EPUB deja de ser la estructura interna definitiva: pasa a ser una de varias fuentes importables.

La edición principal prevista para el usuario sigue siendo la Biblia de Jerusalén que ya usa Ministerium. El repositorio de código **no debe publicar ni redistribuir** textos bíblicos protegidos; el motor y los paquetes textuales se mantienen separados.

## Principios

1. **Offline first**: la lectura, búsqueda, navegación, subrayados, notas y referencias deben funcionar sin Internet.
2. **Motor separado del contenido**: código Android y paquetes bíblicos tienen ciclo de vida y licencia independientes.
3. **IDs estables**: la navegación litúrgica y bíblica no depende del nombre visible de un libro ni de la paginación de un EPUB.
4. **Unidades semánticas**: edición → libro → capítulo → versículo → token opcional.
5. **Interoperabilidad**: importadores para EPUB, USFM, USX y OSIS normalizan a un mismo modelo interno.
6. **Leccionario enlazado, no duplicado**: una lectura litúrgica referencia rangos bíblicos por ID y edición.
7. **Datos personales separados**: subrayados, marcadores, reflexiones y notas no modifican el paquete bíblico de origen.
8. **Lenguas originales opcionales**: hebreo, arameo y griego se incorporan como capas enlazadas, no como requisito para la lectura normal.

## Modelo canónico

### BibleEdition

- `editionId`: ID estable de Ministerium, p. ej. `bj-es`
- `name`
- `abbreviation`
- `language`
- `canon`
- `sourceFormat`
- `version`
- `copyrightNotice`
- `licenseId`
- `contentHash`
- `minAppVersion`

### BibleBook

- `bookId`: ID canónico estable, preferentemente abreviatura interoperable (`GEN`, `EXO`, `MAT`, `JHN`, etc.)
- `editionId`
- `name`
- `shortName`
- `testament`
- `canonicalOrder`
- `chapterCount`

Los libros deuterocanónicos deben estar representados explícitamente; el modelo no puede asumir un canon protestante de 66 libros.

### BibleChapter

- `chapterId`: `${editionId}:${bookId}:${chapter}`
- `bookId`
- `number`

### BibleVerse

- `verseId`: `${editionId}:${bookId}:${chapter}:${verse}`
- `chapterId`
- `verseLabel`
- `verseOrder`
- `text`
- `isHeading`
- `paragraphStart`

`verseLabel` debe admitir numeración no trivial (`3a`, `3b`, rangos editoriales, etc.) sin convertir todo a entero.

### BibleFootnote

- `footnoteId`
- `verseId`
- `marker`
- `text`
- `type`: textual | editorial | cross_reference | study

### BibleCrossReference

- `referenceId`
- `sourceVerseId`
- `targetBookId`
- `targetChapter`
- `targetVerseStart`
- `targetVerseEnd`

### BibleToken (opcional)

- `tokenId`
- `verseId`
- `position`
- `surface`
- `language`
- `lemma`
- `strongId`
- `morphology`
- `sourceDataset`

Esta tabla permite añadir después vista griego/hebreo, Strong, lema y morfología sin alterar el modelo básico de versículos.

## Referencias compartidas con Liturgia

El Leccionario debe guardar referencias semánticas y no depender de una copia del texto bíblico.

Ejemplo conceptual:

```json
{
  "readingId": "mass.2026-08-24.gospel",
  "type": "GOSPEL",
  "bibleReference": {
    "bookId": "JHN",
    "chapter": 1,
    "verseStart": "45",
    "verseEnd": "51"
  },
  "preferredEditionId": "bj-es"
}
```

La capa de presentación puede aplicar el texto litúrgicamente aprobado cuando el Leccionario requiera una forma distinta a la edición bíblica de estudio. Por ello el modelo distingue **referencia bíblica** de **texto litúrgico proclamable**.

## Almacenamiento

### Paquete bíblico de solo lectura

Formato recomendado para Android 3.1: base SQLite versionada por edición. Debe incluir tablas de metadatos, libros, capítulos, versículos, notas y referencias. Puede distribuirse comprimida y verificarse por SHA-256 antes de activarse.

Ventajas:

- búsqueda local rápida;
- carga parcial por capítulo;
- múltiples ediciones sin duplicar código;
- paquete reemplazable independientemente del APK;
- no requiere WebView;
- adecuado para índices FTS futuros.

### Base personal del usuario

Separada del paquete bíblico:

- `highlight`
- `bookmark`
- `reflection`
- `note`
- `reading_history`
- `reading_plan_progress`

Las claves apuntan a `editionId + bookId + chapter + verseLabel` para sobrevivir a una actualización del paquete.

## Importadores

Los importadores son herramientas de construcción; no son necesariamente parte del APK.

### EPUB

Uso principal: migrar la Biblia actualmente disponible en Ministerium. El parser debe extraer estructura, referencias y notas sin conservar la paginación como identidad.

### USFM

Formato prioritario para interoperabilidad con ecosistemas bíblicos. Debe mapear libros, capítulos, versículos, títulos, párrafos, notas y referencias cuando estén disponibles.

### USX

Importador XML equivalente a USFM para fuentes que distribuyan USX.

### OSIS

Importador XML para módulos o corpora que utilicen Open Scripture Information Standard.

Todos producen el mismo paquete SQLite Ministerium.

## API interna del motor

La UI no debe consultar SQLite directamente. Se define una interfaz conceptual `BibleRepository`:

- `listEditions()`
- `listBooks(editionId)`
- `listChapters(editionId, bookId)`
- `getChapter(editionId, bookId, chapter)`
- `getVerseRange(reference)`
- `search(editionId, query, limit)`
- `getFootnotes(verseId)`
- `getCrossReferences(verseId)`
- `getTokens(verseId)`

La implementación inicial puede usar `SQLiteDatabase` para mantener compatibilidad con el stack Android existente; Room puede evaluarse en una migración posterior sin cambiar el contrato del repositorio.

## Experiencia de lectura objetivo

Selección de texto/versículo:

- Subrayar
- Nota
- Reflexión
- Comparar
- Griego/Hebreo (cuando exista capa original)
- Diccionario
- Comentario
- Copiar

`Comparar` alinea ediciones por referencia bíblica, no por posición de pantalla.

## Fuentes externas revisadas

### bplaat/android-apps — Bible

Código MIT. Aporta como referencia un lector Android offline, sin WebView, con paquetes bíblicos locales SQLite, carga por capítulo, búsqueda y separación entre modelos/servicio/vistas. Ministerium reimplementa el patrón arquitectónico y no incorpora sus traducciones ni scrapers.

### arron-taylor/bible-versions

Útil como referencia de normalización `versión → libro → capítulo → versículo`; no se utilizará como fuente de textos ni como dependencia. Las traducciones obtenibles por scraping tienen licencias independientes.

### NewOpenBible

Confirma la utilidad de USFM y la posibilidad de enlazar texto con identificadores léxicos. Sus textos no sustituyen la edición española principal de Ministerium.

### ivandustin/bible

Referencia para una futura capa palabra-a-palabra griego/hebreo. No se adopta como fuente crítica oficial y no cubre por sí sola las necesidades del canon católico.

### awesome-bible-developer-resources

Se usa como radar para formatos, corpora, léxicos, morfología y herramientas de conversión. Cada recurso deberá aprobar revisión de licencia y cobertura canónica antes de incorporarse.

## Reglas de licencia

1. Código de terceros solo se copia cuando la licencia lo permite y se conserva atribución requerida.
2. Una licencia de código no concede derechos sobre textos bíblicos incluidos por una app.
3. Los textos protegidos no se incorporan al repositorio público ni a paquetes redistribuibles sin permiso.
4. Para GPL y licencias copyleft incompatibles con la distribución prevista, preferir reimplementación limpia de ideas/algoritmos documentados.
5. Cada paquete bíblico declara licencia, fuente y hash.

## Fases de migración

### Fase 1 — 3.1

- introducir modelos semánticos e IDs;
- crear `BibleRepository` y backend SQLite;
- preservar el lector existente como fallback;
- crear importador de migración para la edición actual;
- enlazar referencias del Leccionario al nuevo modelo;
- mantener compilación con AGP 4.2.1 / Gradle 6.7.1 / JDK 11.

### Fase 2

- búsqueda indexada FTS;
- subrayados/notas/reflexiones por versículo;
- comparación de ediciones;
- paquetes descargables con hash y versión.

### Fase 3

- griego/hebreo/LXX;
- Strong/lemas/morfología;
- diccionarios y comentarios enlazados;
- sincronización opcional de datos personales.

## Criterio de aceptación de Fase 1

Una build de pruebas debe poder:

1. instalar y abrir sin red;
2. listar una edición bíblica instalada;
3. navegar libro → capítulo;
4. mostrar versículos desde SQLite;
5. resolver un `BibleReference` desde el Leccionario;
6. conservar el comportamiento anterior cuando no exista aún paquete semántico;
7. pasar GitHub Actions sin publicar ningún texto protegido en el repositorio.
