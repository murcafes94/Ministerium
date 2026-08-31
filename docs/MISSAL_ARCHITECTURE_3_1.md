# Arquitectura del Misal — Ministerium 3.1

## Objetivo

Reorganizar el módulo de Misa para que una celebración no dependa de nombres visibles, páginas o archivos monolíticos. El núcleo queda definido por:

`CelebrationId → MissalForm → SemanticUnit`

La aplicación debe poder resolver una fecha a una celebración estable, seleccionar el formulario litúrgico correspondiente y renderizar sus unidades en español, latín o modo bilingüe sin duplicar la estructura.

## Principios

1. **ID propio estable**: `ministeriumId` es la identidad primaria.
2. **Interoperabilidad opcional**: se pueden guardar `cledrId`, `litcalKey`, `romcalKey`, `eprexKey` u otras equivalencias sin depender de ellas.
3. **Calendario separado del formulario**: el motor decide qué celebración corresponde; el Misal entrega el formulario.
4. **Unidad semántica**: colecta, antífona, prefacio, lectura, respuesta, rúbrica, etc. son objetos identificables, no fragmentos de página.
5. **Offline first**: el calendario y los formularios usados por la app deben funcionar sin red.
6. **Contenido versionable**: calendario, formularios, leccionario y cantos se actualizan independientemente del APK cuando sea posible.
7. **Texto litúrgico ≠ referencia bíblica**: las lecturas conservan referencia bíblica semántica, pero el texto proclamable puede proceder del Leccionario litúrgicamente aprobado.
8. **Laudes/Vísperas + Misa**: la composición unificada se genera a partir de unidades litúrgicas conforme a OGLH 93–96, no concatenando pantallas completas.

## CelebrationIdentifiers

Cada celebración debe tener al menos:

- `ministeriumId`
- `title`
- `rank`
- `season`
- `liturgicalColor`
- `dateRule`

Equivalencias opcionales:

- `cledrId`
- `litcalKey`
- `romcalKey`
- `eprexKey`
- `sourceEdition`

Ministerium nunca debe necesitar un servicio remoto para comprender su propio `ministeriumId`.

## MissalForm

Representa un formulario completo o parcial de una celebración.

Campos recomendados:

- `formId`
- `celebrationId`
- `editionId`
- `language`
- `formType`: proper | common | ritual | votive | various_needs | funeral
- `variant`
- `units[]`
- `source`
- `version`
- `contentHash`

Una celebración puede tener varias formas válidas, por ejemplo propio + común alternativo o distintas plegarias/opciones permitidas.

## MissalUnit

Unidad mínima renderizable.

Campos:

- `unitId`
- `type`
- `order`
- `role`
- `language`
- `text`
- `rubric`
- `bibleReference`
- `chantRef`
- `responseTo`
- `optional`
- `conditions`

### Tipos iniciales

- `ENTRANCE_ANTIPHON`
- `SIGN_OF_CROSS`
- `GREETING`
- `PENITENTIAL_ACT`
- `KYRIE`
- `GLORIA`
- `COLLECT`
- `FIRST_READING`
- `RESPONSORIAL_PSALM`
- `SECOND_READING`
- `GOSPEL_ACCLAMATION`
- `GOSPEL`
- `CREED`
- `UNIVERSAL_PRAYER`
- `OFFERTORY`
- `PRAYER_OVER_OFFERINGS`
- `PREFACE`
- `SANCTUS`
- `EUCHARISTIC_PRAYER`
- `MEMORIAL_ACCLAMATION`
- `DOXOLOGY`
- `LORDS_PRAYER`
- `SIGN_OF_PEACE`
- `AGNUS_DEI`
- `COMMUNION_ANTIPHON`
- `PRAYER_AFTER_COMMUNION`
- `BLESSING`
- `DISMISSAL`
- `RUBRIC`

La lista es extensible. El orden no debe codificarse únicamente en la UI.

## Roles

- `PRIEST`
- `DEACON`
- `READER`
- `CANTOR`
- `ASSEMBLY`
- `ALL`
- `RUBRIC`

La separación por rol permite visualización, accesibilidad, canto y generación futura de subsidios.

## Capas lingüísticas

La estructura de la Misa es independiente de la traducción.

Ejemplo conceptual:

```text
unitId: mass.dialogue.greeting
role: PRIEST
es: "El Señor esté con ustedes."
la: "Dominus vobiscum."
```

Modo español: renderiza `es`.
Modo latín: renderiza `la`.
Modo bilingüe: alinea ambas variantes por `unitId`.

Esto evita el problema actual de intentar sincronizar dos documentos completos por posición vertical.

## ChantRef

Inspirado en generadores de subsidios y herramientas Gregorio, una unidad puede tener una referencia opcional a canto:

- `chantId`
- `notationFormat`: gabc | svg | pdf | image | audio
- `language`
- `melodyId`
- `source`
- `licenseId`
- `contentHash`

No se incorpora material de terceros sin licencia compatible. Para repositorios GPL se reimplementa el modelo sin copiar código o archivos.

## Leccionario

Las unidades de lectura contienen una `BibleReference` estable:

```json
{
  "bookId": "JHN",
  "chapter": 1,
  "verseStart": "45",
  "verseEnd": "51"
}
```

El texto proclamable se obtiene del paquete de Leccionario cuando exista; la Biblia de estudio conserva su edición propia.

## Calendario y validación

El generador de calendario de Ministerium debe producir todos los días del año y compararlos durante CI con motores externos cuando sea posible.

Objetivo de pruebas:

- calendario local Ministerium;
- LiturgicalCalendarAPI;
- RomCal/CLEDR u otro motor interoperable cuando exista equivalencia.

Un conflicto se registra como error de validación que requiere revisión antes de publicar el paquete.

Las pruebas deben cubrir un rango amplio (por ejemplo 2020–2040) para detectar:

- años bisiestos;
- Pascua temprana/tardía;
- traslados;
- solemnidades coincidentes;
- Semana Santa y Octava;
- Adviento/Navidad/Epifanía;
- celebraciones nacionales/diocesanas.

## Misa integrada con Liturgia de las Horas

Una celebración combinada no es una pantalla que abre otra pantalla.

Se crea un `CombinedCelebrationPlan` con unidades provenientes de:

- Liturgia de las Horas;
- Misa;
- reglas de integración OGLH 93–96.

Ejemplo conceptual:

```text
Laudes
  versículo inicial
  himno
  salmodia
  ...
Transición conforme a OGLH
Misa
  liturgia de la Palabra
  liturgia eucarística
  ...
```

Las unidades omitidas o sustituidas se determinan por reglas, no por concatenación manual.

## Repositorios revisados

### Missale Meum / ecosistema relacionado

Referencia principal para el patrón `celebration id → proper/form → sections` y para la idea de IDs independientes del nombre visible.

### Qkor/MissaleRomanumApp

Cliente Flutter para el Misal de 1962 que consume la API de Missale Meum. Su modelo separa calendario, `id`, `proper/{id}` y `Ordo.sections`, y cachea JSON en SQLite. Se usa como confirmación arquitectónica, no como fuente normativa para la forma ordinaria actual ni como código copiado.

### jgcrunden/missalette

Generador GPL-3.0 de subsidios LaTeX. Aporta como referencia la separación entre orden ritual, variables lingüísticas y canto `.gabc`/GregorioTeX. Ministerium reimplementa la idea de `role + languageVariant + chantRef`; no copia código GPL ni partituras sin revisar licencia.

### latin-ocr/missaleadusumin01churgoog

OCR histórico del Misal según el uso de York. Puede servir como corpus de prueba para importadores de misales históricos y OCR, pero no como fuente normativa del Misal Romano actual. El OCR contiene errores y requiere revisión.

## Fases

### 3.1 — núcleo

- `CelebrationIdentifiers`;
- `MissalForm`;
- `MissalUnit`;
- enlace con `BibleReference`;
- soporte para variantes lingüísticas;
- `chantRef` opcional;
- resolver formularios por ID;
- mantener fallback del lector Misal existente.

### 3.2 — calendario robusto

- calendario semántico completo;
- validación anual automatizada;
- IDs CLEDR/LitCal/RomCal opcionales;
- paquetes de calendario separados.

### 3.3 — composición litúrgica

- Laudes + Misa;
- Vísperas + Misa;
- rituales y formularios alternativos;
- selección automática de comunes/propios.

### Futuro

- partituras gregorianas opcionales;
- anuncio de fiestas móviles;
- generación de subsidios/PDF desde los mismos datos semánticos;
- importadores históricos.

## Criterio de aceptación del núcleo 3.1

1. Una fecha resuelve a un `ministeriumId`.
2. El ID resuelve a un `MissalForm` sin depender de texto visible.
3. El formulario entrega unidades ordenadas.
4. Una unidad puede tener español/latín alineados por ID.
5. Las lecturas enlazan con referencias bíblicas estables.
6. La UI anterior sigue disponible como fallback durante la migración.
7. GitHub Actions compila la rama sin incorporar textos protegidos nuevos al repositorio.
