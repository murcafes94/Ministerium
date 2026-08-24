# Revisión de fuentes bíblicas — Ministerium 3.1

## Objetivo

Seleccionar fuentes técnicamente útiles y con licencias claras para la futura capa de griego, hebreo, Strong, lema y morfología. Esta revisión **no autoriza por sí misma a redistribuir** cada corpus: antes de incorporar datos se debe verificar la licencia exacta del archivo/edición concreta.

## Decisiones

### Hebreo — candidato principal: Open Scriptures Hebrew Bible (OSHB)

- Repositorio/dataset basado en Westminster Leningrad Codex.
- Formato OSIS disponible.
- Licencia indicada por el radar revisado: CC BY 4.0.
- Aporta una base adecuada para enlazar texto hebreo con el modelo `BibleToken`.

**Estado Ministerium:** APROBADO PARA PROTOTIPO, sujeto a conservar atribución y verificar la licencia del snapshot importado.

### Hebreo/morfología — candidato complementario: MACULA Hebrew

- Sintaxis, morfología y anotaciones lingüísticas.
- Basado también en WLC.
- Licencia indicada: CC BY 4.0.

**Estado:** CANDIDATO COMPLEMENTARIO. No debe reemplazar la identidad bíblica de Ministerium; se enlaza como `sourceDataset`.

### Hebreo + griego — STEPBible Data

- Datos etiquetados en TSV.
- Incluye múltiples datasets hebreos y griegos.
- Licencia indicada: CC BY 4.0.

**Estado:** CANDIDATO MUY ÚTIL para Strong, lema y equivalencias, especialmente durante la fase de enriquecimiento de tokens.

### Strong — Open Scriptures Strong's

- Diccionarios Strong hebreo/griego en XML.
- Marcado como dominio público en el radar.

**Estado:** APROBADO PARA PROTOTIPO. Debe verificarse el repositorio exacto y registrar el hash del snapshot.

### Strong griego Unicode — morphgnt/strongs-dictionary-xml

- Diccionario Strong con griego Unicode, transliteración y correcciones.
- Licencia indicada: CC0-1.0.

**Estado:** PREFERIDO para la representación Unicode del diccionario griego si la revisión del snapshot confirma CC0.

### Léxico griego — Abbott-Smith

- Léxico del NT en TEI XML.
- Marcado como dominio público.

**Estado:** CANDIDATO PRINCIPAL para una primera capa léxica griega más rica que Strong.

### Léxico griego — Dodson

- CSV/TEI XML.
- Marcado como dominio público.

**Estado:** CANDIDATO SECUNDARIO/COMPLEMENTARIO.

### Léxico hebreo — BDB

- Versiones de dominio público y versión enriquecida CC BY 4.0.

**Estado:** CANDIDATO para una fase posterior. Preferir datasets estructurados cuya licencia y procedencia estén explícitas.

## Nuevo Testamento griego

### Opción A — Berean Greek Bible

- Texto crítico basado en Nestle 1904.
- Marcado como dominio público.
- Disponible en USFM y otros formatos.

**Ventaja:** licencia simple para prototipos y distribución.

**Límite:** no debe presentarse como NA28 ni como edición crítica católica oficial.

**Estado:** APROBADO PARA PROTOTIPO TÉCNICO.

### Opción B — SBLGNT

- Edición crítica moderna disponible en XML/OSIS/plaintext.
- El radar indica CC BY 4.0, pero los proyectos morfológicos derivados pueden tener condiciones adicionales y la edición debe revisarse en su licencia oficial.

**Estado:** CANDIDATO ACADÉMICO; REQUIERE REVISIÓN DE LICENCIA DEL SNAPSHOT antes de empaquetar.

### MorphGNT

- Aporta morfología para textos griegos compatibles.
- Las licencias pueden diferir entre etiquetado y texto base.

**Estado:** CANDIDATO DE ANOTACIÓN, no fuente textual automática.

## Septuaginta / deuterocanónicos

Este es el punto que exige mayor cuidado para Ministerium porque el modelo debe cubrir el canon católico y no solamente Tanaj + NT.

### CATSS

- Corpus y análisis morfológico LXX históricamente muy utilizados.
- Existen herramientas de terceros para transformar los datos en SQLite.

**Estado:** INVESTIGAR LICENCIA/CONDICIONES EXACTAS antes de incorporar.

### Open Greek & Latin / First1KGreek / Swete

- Existen versiones digitalizadas y corregidas del texto de Swete.
- Útiles para cubrir LXX y libros deuterocanónicos.
- La licencia puede depender del corpus/archivo concreto.

**Estado:** CANDIDATO PRINCIPAL PARA INVESTIGACIÓN, pero NO se empaqueta aún hasta cerrar licencia, cobertura y calidad.

### Rahlfs 1935 datasets

- Existen repositorios enriquecidos con morfología.
- El radar advierte restricciones aplicables al texto base y algunas versiones usan CC BY-NC-SA.

**Estado:** NO usar como primera opción de distribución.

## Estrategia adoptada

La capa de lenguas originales no estará atada a un corpus único. `BibleToken.sourceDataset` permite combinar:

- texto base;
- lema;
- morfología;
- Strong;
- léxico;
- equivalencias.

Ejemplo:

```text
verseId: grc:JHN:1:1
token: λόγος
lemma: λόγος
strongId: G3056
morphology: N-NSM
sourceDataset: stepbible / morphgnt / otro aprobado
```

## Orden de implementación

1. Strong Unicode / diccionarios abiertos.
2. Prototipo NT griego con corpus de licencia simple.
3. Hebreo OSHB + morfología MACULA/STEPBible.
4. Cerrar una fuente LXX que cubra deuterocanónicos y tenga licencia adecuada.
5. Alinear todos los corpora con IDs bíblicos canónicos de Ministerium.
6. Añadir vista de palabra original solo cuando el dataset de ese versículo exista.

## Restricciones

- No presentar una edición como NA28, BHS/BHQ u otra edición crítica protegida si el dataset realmente procede de otra fuente.
- No mezclar silenciosamente corpora distintos: toda palabra conserva `sourceDataset`.
- No asumir que la licencia del código de un repositorio cubre sus textos/datasets.
- No forzar una numeración protestante cuando la LXX/deuterocanónicos tengan diferencias de libro, capítulo o versículo; se necesitará una tabla de versificación/mapeo.
