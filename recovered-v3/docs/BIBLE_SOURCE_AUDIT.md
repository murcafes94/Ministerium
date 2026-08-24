# Auditoría de fuentes bíblicas — Ministerium 3.1

## Regla general

Ministerium separa el **motor bíblico** de los **paquetes de texto**. Una licencia permisiva del código de una aplicación de terceros no concede derechos sobre las traducciones que esa aplicación pueda incluir.

Ningún texto bíblico protegido debe publicarse en el repositorio de código ni incorporarse a una distribución pública sin comprobar previamente los derechos correspondientes.

## Biblia de Jerusalén

Sigue siendo la edición principal prevista para el módulo de estudio ya existente. La migración 3.1 transforma su estructura técnica de EPUB/páginas a unidades libro-capítulo-versículo, pero no altera por sí misma los derechos de uso o redistribución.

El paquete `bj-es` debe permanecer separado del motor y declarar fuente, versión, aviso de copyright, licencia/permiso aplicable y SHA-256.

## bplaat/android-apps — Bible

**Uso:** referencia arquitectónica.

El código del repositorio está bajo MIT y su lector bíblico demuestra un patrón útil para Ministerium: Android offline, almacenamiento SQLite por edición, lectura por capítulo y búsqueda local sin depender de WebView.

**Decisión:** reimplementar el patrón en el código de Ministerium. No copiar ni redistribuir las traducciones obtenidas por sus herramientas de scraping.

## arron-taylor/bible-versions

**Uso:** referencia de normalización/importación.

Su modelo `versión → libro → capítulo → versículo` confirma la conveniencia de normalizar fuentes antes de consumirlas desde la app. El propio proyecto advierte que muchas traducciones recuperables desde BibleHub están protegidas y requieren permisos independientes.

**Decisión:** no usar el scraper como fuente de contenido de Ministerium.

## NewOpenBible

**Uso:** referencia de USFM y procesamiento bíblico estructurado.

**Decisión:** conservar USFM entre los formatos de entrada admitidos. NewOpenBible no sustituye la edición española principal de Ministerium.

## STEPBible Data

**Uso previsto:** candidato principal para capas léxicas y morfológicas opcionales.

El proyecto publica datasets hebreos y griegos, léxicos y datos relacionados con Strong bajo CC BY 4.0 según su documentación. Antes de empaquetar cada dataset se registrará exactamente el archivo/versión usado y la atribución correspondiente.

**Decisión:** preferido para una futura capa `BibleToken` (superficie, lema, Strong, morfología), sujeto a validación de cobertura y correspondencia con la base textual que se muestre.

## Septuaginta / LXX

Para los libros y pasajes griegos del Antiguo Testamento se necesita una fuente que cubra adecuadamente el canon católico y cuya licencia permita una distribución clara.

**Candidato preferente inicial:** el dataset de Swete mantenido en `nathans/lxx-swete`, cuyos datos se documentan bajo CC BY-SA 4.0 y cuyo código se documenta bajo MIT.

**Decisión:** no incorporar automáticamente datasets Rahlfs/CATSS con condiciones, declaraciones de usuario o procedencia ambigua hasta revisar específicamente sus términos.

## ivandustin/bible

**Uso:** referencia conceptual de almacenamiento palabra-a-palabra de hebreo/griego.

**Decisión:** no adoptarlo como fuente crítica oficial. El modelo de tokenización sí inspira la capa opcional de lenguas originales.

## Formatos admitidos

La herramienta de importación de Ministerium 3.1 admite inicialmente:

- USFM
- USX
- OSIS

EPUB se mantiene como ruta de migración para la edición ya instalada. Todos los formatos deben converger al mismo esquema SQLite semántico.

## Checklist antes de añadir un paquete

1. Identificar titular y licencia/permiso del texto.
2. Confirmar si permite copia, modificación y redistribución en el escenario concreto.
3. Verificar canon y libros incluidos.
4. Registrar versión/fecha de la fuente.
5. Generar paquete semántico.
6. Validar número de libros, capítulos y versículos.
7. Calcular SHA-256 externo del archivo final.
8. Registrar atribuciones en el manifiesto del paquete.
9. Probar lectura offline y actualización/reversión.
