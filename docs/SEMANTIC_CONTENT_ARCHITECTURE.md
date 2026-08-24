# Ministerium — Arquitectura de contenido semántico

## Decisión

Ministerium deja de considerar EPUB/PDF como formato de ejecución para Liturgia de las Horas, Misal, Leccionario, Biblia y demás módulos estructurados.

Los libros fuente pueden seguir utilizándose **fuera de la aplicación** durante la preparación del contenido, pero el APK final no debe depender de abrir, buscar ni modificar EPUB/PDF durante la oración o la lectura.

Flujo objetivo:

```text
fuente editorial (EPUB/PDF/HTML/etc.)
        ↓  solo en herramientas de preparación
extractor específico de la fuente
        ↓
normalizador semántico
        ↓
paquete SQLite compacto + manifiesto
        ↓
resolución litúrgica por fecha/celebración
        ↓
bloques semánticos
        ↓
renderer de Ministerium
```

## Principios

1. **Nada de textos litúrgicos gigantes hardcodeados en Java.** Las reglas y los textos son datos distintos.
2. **Nada de búsquedas por encabezados HTML en tiempo de ejecución** (`find("HIMNO")`, `filepos...`, etc.) para construir una celebración.
3. Una remisión litúrgica debe resolverse antes de renderizar. El usuario debe recibir el texto completo que corresponde, no enlaces editoriales internos.
4. Los textos compartidos se almacenan una sola vez y se relacionan desde los formularios que los usan.
5. Cada paquete lleva versión, esquema, procedencia, lengua y hash. Código y contenido pueden actualizarse por separado.
6. Las fuentes con derechos de autor no se publicarán en repositorios/distribuciones públicas sin comprobar permisos.
7. La aplicación conserva un paquete anterior hasta validar el nuevo, para poder volver atrás si una actualización de contenido falla.

## Paquetes previstos

```text
calendar-ec        calendario y precedencias para Ecuador
breviary-es        Liturgia de las Horas en español
breviary-la        Liturgia Horarum en latín verificado
missal-es          Misal Romano, español
missal-la          Missale Romanum
lectionary-es      lecturas de la Misa
bible-es           Biblia
saints             santoral/comunes
rituals            rituales y Bendicional
dictionaries       diccionarios opcionales
commentaries       comentarios opcionales
```

No es obligatorio que todos se instalen con el APK. El núcleo puede incluir únicamente el mínimo necesario para arrancar y descargar/instalar paquetes validados en almacenamiento privado de la app.

## Esquema semántico común

### `content_block`
Unidad de texto reutilizable.

- `id`: identificador estable, por ejemplo `psalm.es.062`.
- `kind`: `TITLE`, `RUBRIC`, `HYMN`, `ANTIPHON`, `PSALM`, `CANTICLE`, `READING`, `RESPONSORY`, `INTERCESSION`, `PRAYER`, `DIALOGUE`, `PARAGRAPH`, etc.
- `title`
- `text`
- `reference`
- `language`: `es`, `la`.
- `source_key`: procedencia editorial verificable.

### `content_relation`
Relaciona un formulario con los bloques que lo componen.

- `owner_id`
- `role`: `hymn`, `psalmody.1`, `reading`, `gospel_canticle`, `prayer`, etc.
- `target_id`
- `position`
- `choice_group`: identifica alternativas legítimas.
- `condition_key`: condición litúrgica si existe.

### `liturgical_assignment`
Asocia fecha lógica/celebración/hora con un formulario o bloque.

- `celebration_key`
- `season`
- `rank`
- `hour`
- `weekday`
- `cycle`
- `role`
- `target_id`
- `priority`

Las reglas de precedencia del calendario deciden primero **qué celebración corresponde**; estas asignaciones deciden después **qué contenido corresponde**.

## Primer módulo piloto: Completas

Completas será el primer módulo que deje de depender del EPUB. El formulario resuelto debe entregar, como mínimo:

```text
invocación
examen de conciencia
acto penitencial / alternativas legítimas
himno (opción por defecto + alternativas permitidas por la fuente)
salmodia completa
lectura breve
responsorio breve
Nunc dimittis + antífona
oración conclusiva
bendición propia de Completas
antífona mariana permitida
```

Los himnos no se decidirán con cadenas Java del tipo `if (lent) ...`. Se almacenan como bloques y las relaciones litúrgicas determinan cuál corresponde. Las fuentes externas de referencia se usan para comprobar el resultado por fecha; el texto definitivo debe proceder de la fuente litúrgica adoptada para Ministerium.

### Pruebas mínimas de Completas

Se validarán fechas representativas de:

- Tiempo Ordinario (varios días de una misma semana y semanas distintas).
- Adviento.
- Navidad.
- Cuaresma (sin Aleluya en los lugares correspondientes).
- Pascua.
- solemnidades/fiestas que puedan afectar la Hora.

La comparación debe verificar himno, salmodia, lectura, responsorio, antífona del Nunc dimittis, oración y antífona mariana.

## Misal

El antiguo EPUB LAT–ES deja de ser fuente de contenido cuando termine la migración. Solo se conserva como referencia del esquema visual/lógico ya aprobado.

Fuentes adoptadas para preparar los nuevos datos:

- Español: Misal Romano, sección **VERSIÓN DE ESPAÑA** de Liturgia Papal.
- Latín: *Missale Romanum* de Liturgia Papal.

Los PDFs/archivos se procesan fuera del APK. Se eliminan números de página, encabezados, pies, separadores y artefactos editoriales. Se conservan el texto de la Misa, rúbricas necesarias y alternativas legítimas.

Las lecturas completas continúan viniendo de `lectionary-es`, no se duplican desde el Misal.

## Misa + Laudes / Misa + Vísperas

El compositor no concatena páginas. Solicita bloques a tres fuentes semánticas:

```text
breviary → salmodia, preces, Benedictus/Magníficat
missal    → propias, ordinario y liturgia eucarística
lectionary→ lecturas del día
```

El resultado se renderiza en **una única celebración continua y un único scroll**.

## Biblia y anotaciones

La Biblia también migrará de EPUB a entidades por libro/capítulo/versículo. Esto permite localizadores estables como `JN.1.14` para resaltados, notas, reflexiones, comentarios y planes de lectura, evitando depender de posiciones HTML variables.

## Almacenamiento Android

La primera implementación será SQLite nativo (`SQLiteOpenHelper`) para mantener compatibilidad con el proyecto Java/Gradle actual. Room puede evaluarse cuando se modernice el proyecto; no es requisito para adoptar ahora el modelo semántico.

Paquetes instalados:

```text
filesDir/content/<package-id>/<version>/content.db
filesDir/content/<package-id>/<version>/manifest.json
```

Un puntero local identifica la versión activa. La instalación nueva se valida antes de activar y la versión previa no se borra inmediatamente.

## Migración incremental

1. Añadir contrato SQLite y gestor de paquetes sin cambiar el lector actual.
2. Construir paquete semántico de Completas.
3. Renderizar Completas desde el paquete; mantener EPUB solo como fallback temporal.
4. Validar en dispositivo y contra fuente física/de referencia.
5. Migrar Laudes y Vísperas.
6. Migrar Oficio de Lecturas y Hora intermedia.
7. Migrar Misal ES/LA y conectar Misa continua.
8. Migrar Biblia y anotaciones a localizadores semánticos.
9. Migrar Ritual/Bendicional y documentos estructurados restantes.
10. Eliminar del APK cada EPUB/PDF únicamente cuando su módulo ya no tenga dependencia runtime.
11. Cambiar el Centro de actualizaciones para verificar paquetes instalados fuera del APK.

## Criterio de salida de la migración

La migración se considera completa cuando:

- ningún módulo litúrgico necesita abrir un EPUB/PDF para resolver la celebración;
- no quedan remisiones `filepos...` o búsquedas de encabezados HTML en el camino principal;
- el APK no contiene los grandes libros fuente;
- cada bloque mostrado puede rastrearse a `package → source_key → id`;
- el contenido puede actualizarse independientemente del binario de la app;
- Completas, Laudes, Vísperas y Misa unida pasan pruebas por fechas representativas.
