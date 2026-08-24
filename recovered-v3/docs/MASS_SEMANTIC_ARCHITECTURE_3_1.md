# Arquitectura semántica del Misal — Ministerium 3.1

## Objetivo

Migrar el compositor actual hacia unidades litúrgicas estables y eliminar la dependencia del antiguo Misal EPUB como fuente de la celebración. La experiencia visible de `Misa + Laudes/Vísperas` se mantiene continua; cambia la forma en que se obtiene y resuelve cada bloque.

## Fuente litúrgica acordada

Para el texto español del Misal, Ministerium 3.1 usa como fuente de trabajo los PDF de **Liturgia Papal — versión de México**, por ser la línea elegida para Ecuador/Latinoamérica.

Los PDF **no son el formato final de lectura en la app**. El proceso correcto es:

```text
PDF Liturgia Papal (México)
  -> extracción y revisión
  -> normalización por bloques litúrgicos
  -> paquete semántico offline
  -> MassSection[]
  -> lector nativo de Ministerium
```

El antiguo Misal EPUB no es `fallback` ni fuente secundaria de la Misa. Si una sección todavía no ha sido migrada desde Liturgia Papal, debe quedar marcada como contenido pendiente de importar/verificar, en lugar de recuperarse silenciosamente desde el EPUB.

Referencias de trabajo principales:

- Ordinario de la Misa — versión de México.
- Ritos iniciales.
- Liturgia de la Palabra del Ordinario.
- Preparación y presentación de los dones.
- Prefacios.
- Plegarias Eucarísticas I–IV.
- Rito de la Comunión.
- Rito de conclusión.
- Propio del Tiempo: Adviento, Navidad, Cuaresma, Triduo Pascual, Pascua y Tiempo Ordinario.
- Otros rituales/libros de Liturgia Papal cuando correspondan al módulo de Rituales/Bendicional.

## Modelo

```text
ministeriumId / celebrationId
  -> MassCelebration
      -> MassSection[]
          -> ChantResource[] (opcional)
```

Cada `MassSection` conserva:

- ID estable;
- tipo semántico (`COLLECT`, `GOSPEL`, `PREFACE`, etc.);
- etiqueta;
- cuerpo;
- idioma;
- fuente documental;
- edición/región;
- opcionalidad;
- recursos de canto opcionales.

## Principios

1. **El calendario elige la celebración por ID**, no por coincidencia de nombres.
2. **El formulario se compone por unidades**, no por páginas de EPUB ni por navegación dentro de PDF.
3. **Ordinario y Propio son capas distintas** que se combinan según las rúbricas.
4. **El español litúrgico principal es México/Latinoamérica** para esta app.
5. **ESP/LAT comparte estructura**: cambiar de idioma cambia el contenido de una sección, no el orden de la celebración. El latín debe proceder de una fuente litúrgica propia verificada, no de la columna del antiguo EPUB.
6. **La música es una capa opcional**; la ausencia de partitura nunca bloquea el texto.
7. **Misa integrada con Horas conserva un solo flujo** y usa las mismas unidades semánticas de la Misa sola.
8. **Textos protegidos y código se distribuyen separadamente** cuando sea necesario.
9. **No se vuelve al Misal EPUB** para completar huecos de forma automática.

## Repositorios de contenido

- `SemanticMissalRepository`: obtiene `MassSection` desde paquetes estructurados derivados y verificados a partir de Liturgia Papal.
- `MassSourceManifest`: conserva procedencia, documento, versión/región, fecha de verificación y hash del paquete.

No existe `LegacyMissalResolver` para el EPUB en la arquitectura final.

## Leccionario

El Leccionario sigue siendo independiente del Misal. Las secciones `FIRST_READING`, `RESPONSORIAL_PSALM`, `SECOND_READING`, `GOSPEL_ACCLAMATION` y `GOSPEL` se relacionan con referencias bíblicas semánticas, pero el texto litúrgicamente proclamable se mantiene como contenido del Leccionario cuando difiere de una edición bíblica de estudio.

Por tanto, la celebración integrada se compone como:

```text
Liturgia de las Horas + Misal (Liturgia Papal MX) + Leccionario + reglas OGLH
```

## Integración con Laudes/Vísperas

El compositor final recibe dos secuencias:

- `HourSection[]`
- `MassSection[]`

Y aplica explícitamente las reglas de OGLH 93–96. No concatena dos documentos completos ni obliga a navegar entre Activities.

La Liturgia de las Horas aporta sus bloques propios —salmodia, preces, Benedictus/Magníficat y elementos correspondientes— y el Misal aporta los bloques eucológicos y el Ordinario. El Leccionario aporta las lecturas completas.

## Canto

`ChantResource` puede asociarse a Kyrie, Gloria, Credo, Sanctus, aclamaciones, Padre nuestro, Agnus Dei y respuestas dialogadas. Formatos posibles: GABC, SVG o PDF/render prerenderizado.

Antes de incorporar una melodía se verifican de forma independiente:

- fuente;
- edición litúrgica;
- idioma;
- licencia;
- correspondencia exacta entre melodía y texto aprobado.

## Referencias externas de arquitectura

### Qkor/MissaleRomanumApp

Útil por su separación `calendar / ordo / proper` y caché SQLite. Es del Misal 1962 y su cliente no declara licencia, por lo que solo se reimplementan ideas.

### mmolenda/missalemeum

Motor/API MIT para el Misal 1962. Puede servir como referencia arquitectónica, nunca como fuente del calendario o formularios del Misal Romano actual de Ministerium.

### jgcrunden/missalette

Generador GPL-3.0 de misalitos del Ordo Missae posconciliar con salida inglesa/latina y GregorioTeX. Confirma la separación entre plantilla estructural, variables lingüísticas y canto. No se copia código, plantillas ni GABC; se reimplementa el patrón.

## Fases

### Fase 1

- modelos `MassCelebration`, `MassSection`, `MassSectionType`, `ChantResource`;
- manifiesto de procedencia de Liturgia Papal México;
- pruebas de orden y presencia de bloques esenciales;
- retirar cualquier adaptador/fallback automático al Misal EPUB.

### Fase 2

- importar y normalizar Ordinario de la Misa México;
- paquetes semánticos de Ordinario/Propio;
- prefacios y Plegarias eucarísticas como unidades identificadas;
- validación automática por calendario anual;
- intercambio ESP/LAT por sección con fuentes independientes verificadas.

### Fase 3

- completar rituales y Bendicional desde sus fuentes de Liturgia Papal, extrayendo texto para render nativo;
- canto/partituras opcionales;
- composición para impresión/PDF si se desea;
- interoperabilidad con IDs externos (`cledrId`, `litcalKey`, `romcalKey`) sin hacerlos dependencia obligatoria.
