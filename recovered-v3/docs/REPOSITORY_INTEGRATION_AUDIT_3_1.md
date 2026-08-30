# Ministerium 3.1 — auditoría de repositorios y patrones externos

Fecha de revisión: 2026-08-25

Este documento registra qué ideas externas **se usan realmente**, dónde están implementadas y qué repositorios son solo referencias. La regla general de Ministerium es: preferir una reimplementación nativa y pequeña cuando el proyecto externo no encaja tecnológicamente, litúrgicamente o por licencia. Nunca se debe afirmar que un repositorio está «integrado» si no existe una dependencia o código suyo dentro del proyecto.

Ministerium está licenciado bajo **GNU AGPL-3.0**. Los textos litúrgicos, bíblicos y documentales conservan además sus propios derechos/licencias; la licencia del código no concede derechos sobre esos textos.

## Estado de integración

| Proyecto / referencia | Decisión | Implementación real en Ministerium 3.1 | Estado |
| --- | --- | --- | --- |
| **Calibre** (`kovidgoyal/calibre`) | Usar su experiencia de lector para selección, colores, notas, persistencia, navegación y exportación. | `StudyEntry`, `StudyStore`, `UniversalSelectionMenu`, `StudyEditorActivity`, `StudyExport`. Ancla propia: unidad semántica + offsets + texto exacto + prefijo/sufijo; reanclaje contextual; UUID; colores; notas; etiquetas; JSON/Markdown. | **Reimplementado, activo** |
| **Lokus** | IDs estables, local-first, notas vinculadas, backlinks/relaciones y búsqueda transversal. | `ContentReference`, `ReaderContext.contentId`, `StudyEntry.contentId`, `StudyDeskActivity`. `sourceKey` antiguo se conserva para compatibilidad y `contentId` canónico corre en paralelo. | **Reimplementado, activo** |
| **Divinum Officium** | Inspiración para presentación y sincronización paralela LAT–ES. | Los builders ES/LAT generan `data-ministerium-align-key`; `BilingualHoursReaderActivity` sincroniza primero por unidad semántica y usa porcentaje como fallback. En teléfono no fuerza espaciadores. | **Reimplementado, activo** |
| **Missale Meum / patrón celebration→form** | Separar la identidad de la celebración del formulario y de su presentación. | `LiturgicalIdentity` y `LiturgicalDay.identity`: `celebrationId → missalFormId`; las unidades semánticas continúan separadas en Misal/Horas. | **Reimplementado, activo** |
| **CLEDR / CLBDR** | Preparar interoperabilidad, pero no fabricar identificadores. | `LiturgicalIdentity.cledrId` y `clbdrId` existen como campos opcionales vacíos hasta disponer de correspondencia verificada. | **Preparado, sin IDs inventados** |
| **LiturgicalCalendarAPI** | Referencia/validación de reglas de calendario; no hacer depender el uso diario de una API externa. | Runtime actual usa calendario local de Ecuador + cálculo interno; las descargas son explícitas desde Actualizaciones. | **Referencia, no dependencia** |
| **Breviarium Core / Liturgia+ / liturgia-horas-tui** | Contrastar estructura de Horas y tratamiento de propios/comunes; no copiar un motor externo. | `LiturgicalResolver`, `DailyHoursRepository`, `SaintOfficeRepository`, `ComplineContentRepository`, `CombinedHoursRepository`. Regla de memoria libre/obligatoria y propio/común implementada localmente. | **Patrones reimplementados** |
| **Readium / FolioReader** | Robustez EPUB y navegación estándar. | `EpubNavigation` añade EPUB 3 `container.xml → OPF → nav`; `EpubUtils` conserva NCX y usa NAV como fallback. | **Patrón reimplementado, activo** |
| **KOReader / Readest** | UX de lectura: gestos, zoom, lector limpio/local-first. | `ReaderChrome`, `ReaderPreferences`, lectores WebView: swipe cuando existe navegación, pinch de tamaño, temas, cabecera autoocultable. | **Patrón reimplementado, activo** |
| **Bitwarden Android** | Seguridad: no guardar secretos largos en texto plano ni tokens de escritura dentro del APK. | Drive usa Storage Access Framework/proveedor de Google Drive; el APK no contiene token GitHub de escritura. La firma `testStable` es deliberadamente una clave pública de pruebas, no producción. | **Patrón aplicado** |
| **PDFBox / Stirling-PDF** | PDF como fuente de construcción, no como runtime. | `tools/build_liturgiapapal_missal*.py`, `build_liturgiapapal_rituals*.py`, `build_pdf_dictionary.py`: PDF → extracción/limpieza → assets estructurados. El Misal no renderiza PDF en ejecución. | **Patrón aplicado** |
| **Penpot** | Referencia de consistencia visual/sistema de diseño. | No hay dependencia Penpot. Se refleja únicamente en componentes y estilos comunes del lector. | **Referencia visual** |
| **AppFlowy** | Referencia para organización de notas/editor local-first. | `Mi estudio` y editor son implementación Android propia; no hay SDK ni código AppFlowy. | **Referencia, no dependencia** |

## Repositorios antiguos de Misal y Biblia

| Repositorio | Verificación y uso |
| --- | --- |
| **Qkor/MissaleRomanumApp** | Su README indica que muestra el Misal Romano de **1962** y obtiene textos/calendario de missalemeum.com. No se usa su contenido para el Misal actual de Ministerium. Solo sirve para contrastar el patrón fecha/celebración→formulario. No se encontró un `LICENSE` en la ruta estándar durante esta auditoría, por lo que no se copia código. |
| **jgcrunden/missalette** | Genera una missalette mediante LaTeX y permite respuestas/oraciones en latín. GPL-3.0. La idea de **salida imprimible** queda registrada, pero no se introduce LaTeX en Android ni se usa como fuente litúrgica. |
| **latin-ocr/missaleadusumin01churgoog** | Es un corpus OCR/hOCR de un Misal histórico. No es fuente normativa para el Misal actual y no se importa al APK. Puede servir únicamente para investigación/OCR histórica. |
| **arron-taylor/bible-versions** | El propio repositorio no distribuye traducciones y advierte sobre copyright; proporciona scrapers/JSON. Ministerium **no** scrapea ni redistribuye traducciones protegidas. Se conserva solo el patrón `versión → libro → capítulo → versículo`. |
| **ivandustin/bible** | Datos griego/hebreo por palabra en CSV, procedentes de fuentes externas. No reemplaza la Biblia de Jerusalén. Puede servir en el futuro para herramientas de lengua original solo después de verificar licencia/procedencia por corpus. |
| **NewOpenBible/NewOpenBible** | Aporta USFM y una arquitectura abierta de referencias; sus textos se publican bajo CC BY-SA y scripts bajo BSD según su README. No sustituye la Biblia elegida por Ministerium. El concepto útil es interoperabilidad libro/capítulo/versículo/USFM. |
| **biblenerd/awesome-bible-developer-resources** | Índice de recursos para desarrolladores. Es una fuente de descubrimiento, no una dependencia de la aplicación. |

## Fuente normativa del Misal

La decisión vigente prevalece sobre todos los repositorios de Misal anteriores:

1. **Misal actual:** PDF de **Liturgia Papal**, versión de México para español y Missale Romanum para latín.
2. Los PDF se descargan/validan en build y se convierten a texto/estructura semántica.
3. El antiguo Misal EPUB no se usa como fallback.
4. Un repositorio de Misal histórico (1962, OCR antiguo, etc.) no puede resolver un formulario actual.
5. Las referencias estructurales externas pueden ayudar a validar la arquitectura, pero el texto mostrado debe conservar trazabilidad a la fuente litúrgica elegida.

## Fuente y arquitectura de la Biblia

La Biblia principal de Ministerium se mantiene separada de los repositorios de traducciones externas. Los repositorios anteriores se usan únicamente para patrones técnicos de estructura, índices e interoperabilidad. No se incorporan automáticamente textos descargados/scrapeados de terceros.

La identificación canónica nueva utiliza `ContentReference` y no sustituye las claves históricas, para que subrayados/notas existentes sigan restaurándose.

## Calibre: implementación concreta

Calibre es GPL-3.0 y Ministerium AGPL-3.0, pero en 3.1 se decidió **no introducir el runtime de Calibre** ni copiar sus módulos Python/JavaScript. Se reimplementaron las capacidades necesarias en Android/Java para evitar una dependencia enorme y conservar el modelo propio de contenido:

- UUID por anotación;
- colores de subrayado;
- selección contextual explícita;
- nota/reflexión vinculada;
- etiquetas;
- ancla semántica con offsets;
- copia exacta del texto seleccionado;
- contexto anterior/posterior para reanclaje;
- restauración resistente a cambios menores;
- exportación JSON y Markdown;
- inclusión automática en la copia completa de Ministerium/Drive mediante `ministerium-study-v3.json`.

Ministerium no usa EPUB CFI de Calibre porque sus propios documentos semánticos (Biblia, Liturgia, Misal, Magisterio, Derecho, Rituales) necesitan un ancla común que también funcione fuera de EPUB.

## Exclusiones de esta auditoría

Por petición expresa, **no forman parte de este lote** y no se han incorporado aquí las ideas del grupo más reciente:

- Turbo Editor
- All-In-One-Python-Projects
- auth-flow-kit
- LeafPic
- LizardFS
- TeknoMW3
- SELinuxModeChanger

Se revisarán únicamente después de cerrar y verificar este bloque anterior.

## Contratos que deben mantenerse

- No introducir tokens GitHub de escritura en el APK.
- No activar descargas de calendario/Leccionario simplemente por abrir un lector.
- No volver al Misal EPUB como fallback.
- No sustituir la Biblia configurada por una traducción scrapeada.
- No inventar IDs CLEDR/CLBDR.
- No activar TTS en texto latino.
- Conservar `sourceKey` mientras existan anotaciones antiguas; añadir capacidades nuevas sobre `contentId`.
- Cualquier copia directa futura de código externo debe revisar primero licencia, avisos y compatibilidad; “inspirado en” no significa “código importado”.
