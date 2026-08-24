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
```

Tipos de sección previstos, entre otros:

- entrance_antiphon
- penitential_act
- kyrie
- gloria
- collect
- first_reading
- responsorial_psalm
- second_reading
- gospel_acclamation
- gospel
- homily_placeholder
- creed
- universal_prayer
- offertory
- prayer_over_offerings
- preface
- eucharistic_prayer
- communion_rite
- communion_antiphon
- prayer_after_communion
- blessing
- dismissal

Este modelo es compatible con la composición continua de la Misa integrada ya exigida en Ministerium 3.0.
