# Referencias externas para el Misal — Ministerium 3.1

## Qkor/MissaleRomanumApp

Repositorio Flutter/Dart para consultar el Misal Romano de 1962. Su README indica que los textos y el calendario se obtienen desde la API pública de MissaleMeum.

### Patrones arquitectónicos útiles

- separar calendario, ordinario (`ordo`) y propios (`proper`);
- consumir contenido estructurado por API y cachearlo localmente;
- representar una celebración como una lista ordenada de secciones con identificador, etiqueta y cuerpo;
- permitir lectura offline después de haber descargado/cacheado los datos.

### Restricción

El repositorio cliente no declara una licencia explícita en GitHub. Por tanto, Ministerium no copia su código. Las ideas arquitectónicas se reimplementan de forma independiente.

## mmolenda/missalemeum

Repositorio actual del servicio MissaleMeum. Su código se publica bajo licencia MIT y ofrece una API documentada para calendario, propios, ordinario y otros recursos.

### Alcance litúrgico

MissaleMeum está diseñado para el Misal Romano de 1962 / Misa Tradicional Latina. No debe utilizarse como fuente del calendario, formularios o rúbricas del Misal Romano actual que usa Ministerium.

### Decisión para Ministerium

1. No sustituir el motor litúrgico actual por el calendario de 1962.
2. Adoptar, mediante implementación propia, el patrón semántico `CalendarDay -> Ordo -> Proper -> Section`.
3. Mantener cada sección con un ID estable para poder componer Misa sola, Misa + Laudes y Misa + Vísperas sin depender de páginas del EPUB.
4. Mantener caché/offline y paquetes actualizables, pero usando fuentes propias y aprobadas para el rito romano actual/Ecuador.
5. Si en el futuro se añade una opción de Misal 1962, tratarla como módulo/edición litúrgica separada, nunca mezclada con el calendario ordinario actual.

## jgcrunden/missalette

Generador de hojas/misalitos en LaTeX para el Ordo Missae posconciliar. Su plantilla principal organiza la celebración en Ritos iniciales, Liturgia de la Palabra, Liturgia eucarística, Rito de la Comunión y Ritos conclusivos. Permite una salida en inglés o en latín y usa GregorioTeX/GABC para respuestas y cantos del Ordinario.

### Lo que aporta a Ministerium

- confirma que el Ordinario debe tratarse como bloques semánticos independientes del formulario propio del día;
- muestra una separación útil entre **texto/variables** y **plantilla de composición**;
- confirma que la misma celebración puede renderizarse en otra lengua sin cambiar su estructura;
- aporta un catálogo práctico de elementos cantables: Kyrie, Gloria, Credo, Sanctus, aclamación del misterio de la fe, Padre nuestro, Agnus Dei y respuestas dialogadas;
- sugiere tratar la notación gregoriana como un recurso opcional asociado a una sección, no incrustada en el texto principal.

### Restricción

El repositorio está bajo GPL-3.0. Ministerium no copiará su código, plantillas ni archivos GABC. Se reimplementará el modelo funcional y, si se añaden melodías, se verificarán por separado la fuente musical, su licencia y la correspondencia con el texto litúrgico español/latino aprobado.

El texto inglés del Misal tampoco se reutiliza como fuente: las traducciones litúrgicas modernas pueden tener derechos editoriales independientes de la licencia del repositorio.

## Modelo recomendado para Ministerium

```text
MassEdition
  -> LiturgicalCalendarDay
      -> MassCelebration
          -> MassSection[]
              id
              type
              label
              body
              language
              source
              optionality
              chantResource?
```

Tipos de sección previstos, entre otros:

- entrance_antiphon
- sign_of_cross
- greeting
- penitential_act
- kyrie
- gloria
- collect
- first_reading
- responsorial_psalm
- second_reading
- gospel_acclamation
- gospel_dialogue
- gospel
- homily_placeholder
- creed
- universal_prayer
- preparation_of_gifts
- prayer_over_offerings
- preface_dialogue
- preface
- sanctus
- eucharistic_prayer
- mystery_of_faith
- final_doxology
- lord_prayer
- embolism
- sign_of_peace
- agnus_dei
- invitation_to_communion
- communion_antiphon
- prayer_after_communion
- blessing
- dismissal

## Capa de canto

La partitura debe mantenerse separada del texto y asociada por ID:

```text
ChantResource
  chantId
  sectionType
  language
  notationFormat   // gabc, svg, pdf, etc.
  source
  license
  edition
  variant
```

Esto permite mostrar el texto ordinario aunque no exista partitura y añadir más adelante notación gregoriana sin alterar el motor de la Misa.

Este modelo es compatible con la composición continua de la Misa integrada ya exigida en Ministerium 3.0 y con la futura visualización ESP/LAT por bloque.
