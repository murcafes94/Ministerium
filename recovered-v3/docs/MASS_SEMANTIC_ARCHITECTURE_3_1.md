# Arquitectura semántica del Misal — Ministerium 3.1

## Objetivo

Migrar el compositor actual, que localiza fragmentos por nombres de archivos/anclas dentro del EPUB, hacia unidades litúrgicas estables. La experiencia visible de `Misa + Laudes/Vísperas` se mantiene continua; cambia la forma en que se resuelve cada bloque.

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
- fuente;
- opcionalidad;
- recursos de canto opcionales.

## Principios

1. **El calendario elige la celebración por ID**, no por coincidencia de nombres.
2. **El formulario se compone por unidades**, no por páginas del EPUB.
3. **Ordinary y Proper son capas distintas** que se combinan según las rúbricas.
4. **ESP/LAT comparte estructura**: cambiar de idioma cambia el contenido de una sección, no el orden de la celebración.
5. **La música es una capa opcional**; la ausencia de partitura nunca bloquea el texto.
6. **Misa integrada con Horas conserva un solo flujo** y usa las mismas unidades semánticas de la Misa sola.
7. **Textos protegidos y código se distribuyen separadamente** cuando sea necesario.

## Compatibilidad con el compositor 3.0

Durante la migración existirán dos resolutores:

- `LegacyMissalResolver`: obtiene bloques desde el EPUB existente.
- `SemanticMissalRepository`: obtiene `MassSection` desde paquetes estructurados.

La estrategia es semantic-first / legacy-fallback, igual que la migración bíblica. Así se puede publicar una build de prueba sin perder formularios todavía no convertidos.

## Leccionario

Las secciones `FIRST_READING`, `RESPONSORIAL_PSALM`, `SECOND_READING`, `GOSPEL_ACCLAMATION` y `GOSPEL` se relacionan con referencias bíblicas semánticas, pero el texto litúrgicamente proclamable se mantiene como contenido del Leccionario cuando difiere de una edición bíblica de estudio.

## Integración con Laudes/Vísperas

El compositor final recibe dos secuencias:

- `HourSection[]`
- `MassSection[]`

Y aplica explícitamente las reglas de OGLH 93–96. No concatena dos documentos completos ni obliga a navegar entre Activities.

## Canto

`ChantResource` puede asociarse a Kyrie, Gloria, Credo, Sanctus, aclamaciones, Padre nuestro, Agnus Dei y respuestas dialogadas. Formatos posibles: GABC, SVG o PDF/render prerenderizado.

Antes de incorporar una melodía se verifican de forma independiente:

- fuente;
- edición litúrgica;
- idioma;
- licencia;
- correspondencia exacta entre melodía y texto aprobado.

## Referencias externas

### Qkor/MissaleRomanumApp

Útil por su separación `calendar / ordo / proper` y caché SQLite. Es del Misal 1962 y su cliente no declara licencia, por lo que solo se reimplementan ideas.

### mmolenda/missalemeum

Motor/API MIT para el Misal 1962. Puede servir como referencia arquitectónica, nunca como fuente del calendario o formularios del Misal Romano actual de Ministerium.

### jgcrunden/missalette

Generador GPL-3.0 de misalitos del Ordo Missae posconciliar con salida inglesa/latina y GregorioTeX. Confirma la separación entre plantilla estructural, variables lingüísticas y canto. No se copia código, plantillas ni GABC; se reimplementa el patrón.

## Fases

### Fase 1

- modelos `MassCelebration`, `MassSection`, `MassSectionType`, `ChantResource`;
- pruebas de orden y presencia de bloques esenciales;
- adaptador desde fuentes actuales hacia secciones;
- mantener `CombinedMassComposer` como fallback.

### Fase 2

- paquetes semánticos de Ordinario/Propio;
- prefacios y Plegarias eucarísticas como unidades identificadas;
- validación automática por calendario anual;
- intercambio ESP/LAT por sección.

### Fase 3

- canto/partituras opcionales;
- composición para impresión/PDF si se desea;
- interoperabilidad con IDs externos (`cledrId`, `litcalKey`, `romcalKey`) sin hacerlos dependencia obligatoria.
