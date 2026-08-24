# Ministerium — Arquitectura semántica del Misal ES–LA

## Objetivo

Sustituir el uso runtime del antiguo EPUB `Misal-Diario-Romano.epub` por un modelo de datos compacto, trazable y bilingüe.

Las fuentes editoriales se usan únicamente durante la preparación del contenido:

- Español: Misal Romano, **VERSIÓN DE ESPAÑA**, publicado por Liturgia Papal.
- Latín: *Missale Romanum* publicado por Liturgia Papal.

Los PDFs/HTML fuente no se muestran dentro de Ministerium ni se incluyen como motor de resolución. El preprocesado elimina número de página, encabezados, pies y demás artefactos editoriales y conserva únicamente el texto litúrgico y las rúbricas necesarias.

Las lecturas completas permanecen en `lectionary-es` y no se duplican en el Misal.

## Patrones de referencia adoptados

### Breviarium Core

Se adopta el patrón conceptual:

```text
fecha
  ↓
calendario
  ↓
celebración canónica
  ↓
buscar formulario propio
  ↓
completar huecos desde feria/temporal/común
  ↓
mapper
  ↓
objeto semántico estable
```

Breviarium mantiene almacenamiento compacto y después usa `mappers` para convertir IDs/keys internos en un contrato semántico legible. También conserva las lecturas de Misa como consulta separada (`getLectures`), lo que coincide con la separación `missal` / `lectionary` de Ministerium.

Ministerium no copiará el mecanismo de valores centinela (`-1`) de Breviarium. La herencia será explícita mediante `form_inheritance`.

### liturgia-horas-tui

Se adopta el patrón de **preprocesado una sola vez**:

```text
fuente externa
  ↓
extractor
  ↓
normalizador
  ↓
JSON/SQLite generado
  ↓
runtime offline
```

Ese proyecto demuestra que el contenido de un ciclo estructurado puede reducirse a un conjunto pequeño de datos y cargarse sin scraping ni red durante la oración. Ministerium no copiará su calendario simplificado ni sus textos CEE como fuente definitiva; se usa solo como referencia de arquitectura y pruebas.

## Identidad bilingüe

La identidad de un elemento litúrgico no contiene el idioma.

Ejemplo:

```text
unit_id: mass.ordinary.pater

localized_text:
  es → Padre nuestro...
  la → Pater noster...
```

Lo mismo se aplica a:

```text
mass.ordinary.sign_of_cross
mass.ordinary.kyrie
mass.ordinary.gloria
mass.ordinary.creed.nicene
mass.ordinary.creed.apostles
mass.ordinary.pater
mass.ordinary.agnus_dei
mass.conclusion.blessing
```

Y a textos propios:

```text
mass.proper.saint_bartholomew.entrance
mass.proper.saint_bartholomew.collect
mass.proper.saint_bartholomew.offerings
mass.proper.saint_bartholomew.communion
mass.proper.saint_bartholomew.postcommunion
```

El renderer solicita el mismo `unit_id` a `es` y `la`. Si ambos existen, puede presentarlos sincronizados. Si falta latín verificado, se muestra español y el latín queda explícitamente no disponible; nunca se traduce automáticamente.

## Formularios

Un `content_form` representa un conjunto litúrgico resoluble.

Tipos previstos:

```text
MASS_PROPER
MASS_ORDINARY
PREFACE
EUCHARISTIC_PRAYER
MASS_COMMON
HOUR
RITUAL
```

Ejemplos:

```text
mass.temporal.ordinary_21_sunday_a
mass.sanctoral.saint_bartholomew
mass.common.apostles
mass.ordinary.roman
preface.apostles.01
eucharistic_prayer.01
eucharistic_prayer.02
eucharistic_prayer.03
eucharistic_prayer.04
```

## Roles del formulario de Misa

Roles semánticos mínimos:

```text
entrance_antiphon
collect
prayer_over_offerings
preface
communion_antiphon
postcommunion
```

El Ordinario añade:

```text
sign_of_cross
greeting
penitential_act
kyrie
gloria
word_liturgy_transition
creed
prayer_of_faithful_transition
offertory_dialogues
preface_dialogue
sanctus
eucharistic_prayer
pater
embolism
doxology
sign_of_peace
agnus_dei
communion_dialogue
conclusion
```

En `Misa + Laudes/Vísperas`, las preces de la Hora ocupan el lugar previsto por el modo de seminario definido para Ministerium; el compositor no inserta una segunda oración universal.

## Herencia de formularios

Un propio no necesita repetir contenido compartido.

Ejemplo conceptual:

```text
mass.sanctoral.saint_bartholomew
  ↓ hereda
mass.common.apostles
  ↓ hereda
mass.ordinary.roman
```

O un domingo:

```text
mass.temporal.ordinary_21_sunday_a
  ↓ hereda
mass.ordinary.roman
```

`form_inheritance.priority` define el orden. Para cada `role`, el resolvedor toma la primera relación válida encontrada desde el formulario más específico hacia sus padres.

Esto sustituye la búsqueda por nombre de archivo, fragmento HTML y coincidencia de texto que usa actualmente `MissalProperRepository`.

## Alternativas

Las alternativas legítimas se modelan con `choice_group` y `condition_key`, no con HTML escondido.

Ejemplos:

```text
choice_group = creed
  - Niceno-constantinopolitano (default)
  - Apostólico

choice_group = eucharistic_prayer
  - I
  - II (default general configurable por contexto)
  - III
  - IV
```

La Plegaria IV lleva una condición semántica, por ejemplo:

```text
condition_key = mass.ep4.allowed
```

El motor la deshabilita cuando la celebración exige un prefacio incompatible con su prefacio propio e invariable.

## Prefacios

Los prefacios son formularios independientes para poder reutilizarlos.

Ejemplo:

```text
preface.apostles.01
  ├── title
  ├── rubric
  └── text
```

Una Misa puede relacionar uno o varios prefacios mediante `choice_group = preface`. Si existe un único prefacio obligatorio, se muestra directamente y no aparece selector.

## Resolución por fecha

```text
Calendar EC
  ↓
celebration_key + rank + season + cycle + weekday
  ↓
liturgical_assignment
  ↓
content_form
  ↓
form_inheritance
  ↓
form_relation
  ↓
semantic_unit
  ↓
localized_text(es/la)
```

El calendario decide primero qué celebración corresponde. El Misal no intenta deducir la celebración buscando palabras en títulos editoriales.

## Misa unida

El compositor final recibe objetos ya resueltos:

```text
BreviaryForm
  salmodia
  preces
  Benedictus/Magníficat

MissalForm
  entrada
  colecta
  ofertorio
  prefacio
  plegaria
  comunión
  poscomunión
  conclusión

LectionaryForm
  primera lectura
  salmo
  segunda lectura si corresponde
  aclamación
  evangelio
```

Y genera un único flujo:

```text
antífona de entrada + señal de la cruz
salmodia
Kyrie
Gloria si corresponde
colecta
lecturas
homilía
Credo si corresponde
preces de la Hora
liturgia eucarística
comunión
Benedictus/Magníficat
poscomunión
conclusión
```

No se concatenan páginas ni se abren Activities intermedias.

## Emparejamiento ES–LA durante el preprocesado

El extractor genera primero bloques provisionales de cada fuente. Un normalizador los asigna a los mismos IDs semánticos.

Ejemplo:

```text
staging/es/collect_0041 → mass.proper.x.collect [es]
staging/la/collect_0041 → mass.proper.x.collect [la]
```

Antes de publicar un paquete bilingüe se valida:

- que cada formulario español tenga las partes obligatorias;
- que cada texto latino esté emparejado con el mismo rol litúrgico;
- que no queden encabezados/pies/números de página;
- que no se hayan mezclado lecturas del Leccionario dentro del Misal;
- que las rúbricas se conserven como `RUBRIC` y no como texto proclamado;
- que las alternativas mantengan su relación y restricciones.

## Distribución de paquetes

Arquitectura recomendada:

```text
missal-structure   formularios, relaciones, herencia, asignaciones
missal-es          localized_text español
missal-la          localized_text latín
lectionary-es      lecturas de la Misa
```

`missal-la` puede ser opcional. `missal-structure` evita duplicar la misma lógica en español y latín y garantiza que ambos idiomas compartan exactamente los mismos IDs.

Si durante la primera migración resulta más sencillo distribuir una sola base `missal.db`, el esquema seguirá siendo el mismo y permitirá separar los idiomas posteriormente sin cambiar el contrato del renderer.

## Criterio de sustitución del EPUB antiguo

`Misal-Diario-Romano.epub` solo se elimina del APK cuando:

1. Ordinario de la Misa está disponible desde el paquete semántico.
2. Propios necesarios están resueltos por `celebration_key`.
3. Prefacios y Plegarias I–IV funcionan con restricciones.
4. Misa simple funciona sin EPUB.
5. Misa + Laudes/Vísperas funciona en un único scroll sin EPUB.
6. ES y LA se emparejan por `unit_id`, no por posición HTML.
7. Las pruebas por domingo, solemnidad, fiesta, memoria y feria pasan en dispositivo.
