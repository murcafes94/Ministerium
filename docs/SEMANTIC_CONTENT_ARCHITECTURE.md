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
formularios + bloques semánticos
        ↓
renderer de Ministerium
```

## Principios

1. **Nada de textos litúrgicos gigantes hardcodeados en Java.** Las reglas y los textos son datos distintos.
2. **Nada de búsquedas por encabezados HTML en tiempo de ejecución** (`find("HIMNO")`, `filepos...`, etc.) para construir una celebración.
3. Una remisión litúrgica debe resolverse antes de renderizar. El usuario debe recibir el texto completo que corresponde, no enlaces editoriales internos.
4. Los textos compartidos se almacenan una sola vez y se relacionan desde los formularios que los usan.
5. La **identidad litúrgica es independiente del idioma**. Español y latín comparten `unit_id` y aportan textos localizados distintos.
6. La herencia entre propio, temporal, feria y común es explícita; no se deduce buscando títulos ni usando valores mágicos.
7. Cada paquete lleva versión, esquema, procedencia, lengua/capacidades y hash. Código y contenido pueden actualizarse por separado.
8. Las fuentes con derechos de autor no se publicarán en repositorios/distribuciones públicas sin comprobar permisos.
9. La aplicación conserva un paquete anterior hasta validar el nuevo, para poder volver atrás si una actualización de contenido falla.

## Paquetes previstos

La separación lógica es:

```text
calendar-ec          calendario y precedencias para Ecuador
breviary-structure   formularios, relaciones y asignaciones de la Liturgia
breviary-es          textos españoles
breviary-la          textos latinos verificados
missal-structure     formularios, relaciones, herencia y asignaciones del Misal
missal-es            Misal Romano, español
missal-la            Missale Romanum
lectionary-es        lecturas de la Misa
bible-es             Biblia
saints               santoral/comunes auxiliares
rituals              rituales y Bendicional
dictionaries         diccionarios opcionales
commentaries         comentarios opcionales
```

Durante la primera migración, estructura y textos pueden viajar en una misma base si simplifica la implementación. El contrato semántico debe permitir separarlos después sin cambiar el renderer.

No es obligatorio que todos se instalen con el APK. El núcleo puede incluir únicamente el mínimo necesario para arrancar y descargar/instalar paquetes validados en almacenamiento privado de la app.

## Esquema semántico común

### `semantic_unit`

Identidad estable e independiente del idioma.

- `unit_id`: por ejemplo `psalm.062`, `mass.ordinary.pater`, `compline.nunc_dimittis.antiphon`.
- `kind`: `TITLE`, `RUBRIC`, `HYMN`, `ANTIPHON`, `PSALM`, `CANTICLE`, `READING`, `RESPONSORY`, `INTERCESSION`, `PRAYER`, `DIALOGUE`, `PARAGRAPH`, etc.

### `localized_text`

Texto de una `semantic_unit` en una lengua concreta.

- `unit_id`
- `language`: `es`, `la`.
- `title`
- `body`
- `reference_text`
- `source_key`: procedencia editorial verificable.

Ejemplo:

```text
unit_id = mass.ordinary.pater
  es → Padre nuestro...
  la → Pater noster...
```

Esto permite al renderer LAT–ES pedir exactamente la misma unidad en ambos idiomas, sin sincronizar dos documentos HTML por posición.

### `content_form`

Formulario litúrgico resoluble, por ejemplo:

```text
hour.compline.psalter1.sunday
hour.lauds.saint_bartholomew
mass.temporal.ordinary_21_sunday_a
mass.sanctoral.saint_bartholomew
mass.common.apostles
preface.apostles.01
eucharistic_prayer.04
```

Campos principales:

- `form_id`
- `form_type`
- `source_key`

### `form_relation`

Relaciona un formulario con las unidades que lo componen.

- `form_id`
- `role`: `hymn`, `psalmody.1`, `reading`, `collect`, `preface`, etc.
- `target_unit_id`
- `position`
- `choice_group`: identifica alternativas legítimas.
- `condition_key`: condición litúrgica si existe.
- `is_default`: alternativa seleccionada por defecto cuando hay varias válidas.

### `form_inheritance`

Permite completar un formulario específico con contenido de uno o varios padres.

- `child_form_id`
- `parent_form_id`
- `priority`

Ejemplo:

```text
mass.sanctoral.saint_bartholomew
  ↓
mass.common.apostles
  ↓
mass.ordinary.roman
```

Para cada `role`, el resolvedor busca primero en el formulario más específico y después en sus padres por prioridad. Esto sustituye la lógica de “buscar el enlace correcto dentro del EPUB”.

### `liturgical_assignment`

Asocia la celebración ya resuelta por el calendario con un formulario.

- `celebration_key`
- `season`
- `rank_key`
- `hour`
- `weekday`
- `cycle_key`
- `form_id`
- `priority`

Las reglas de precedencia del calendario deciden primero **qué celebración corresponde**; las asignaciones deciden después **qué formulario corresponde**.

## Referencias arquitectónicas

### Breviarium Core

Se toma como referencia el patrón:

```text
fecha → calendario → opciones propias/feriales → combinación → mapper → objeto semántico
```

Su separación entre almacenamiento compacto y mappers públicos confirma que Ministerium puede guardar IDs internamente y exponer al renderer un contrato estable. También mantiene las lecturas de Misa separadas del Oficio, igual que `lectionary-es` en Ministerium.

Ministerium mejora ese patrón usando `form_inheritance` explícita en lugar de valores centinela para indicar “tomar de la feria/común”.

### liturgia-horas-tui

Se toma como referencia el patrón de **preprocesar una sola vez** y ejecutar offline desde datos estructurados. No se adopta su calendario simplificado ni sus textos como autoridad litúrgica de Ministerium.

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

Los himnos no se decidirán con cadenas Java del tipo `if (lent) ...`. Se almacenan como unidades y las relaciones/asignaciones litúrgicas determinan cuál corresponde. Las fuentes externas de referencia se usan para comprobar el resultado por fecha; el texto definitivo debe proceder de la fuente litúrgica adoptada para Ministerium.

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

El antiguo EPUB LAT–ES deja de ser fuente de contenido cuando termine la migración. Solo se conserva temporalmente como referencia del esquema visual/lógico ya aprobado.

Fuentes adoptadas para preparar los nuevos datos:

- Español: Misal Romano, sección **VERSIÓN DE ESPAÑA** de Liturgia Papal.
- Latín: *Missale Romanum* de Liturgia Papal.

Los PDFs/archivos se procesan fuera del APK. Se eliminan números de página, encabezados, pies, separadores y artefactos editoriales. Se conservan el texto de la Misa, rúbricas necesarias y alternativas legítimas.

Las lecturas completas continúan viniendo de `lectionary-es`, no se duplican desde el Misal.

El contrato detallado se documenta en `docs/MISSAL_SEMANTIC_ARCHITECTURE.md`.

## Misa + Laudes / Misa + Vísperas

El compositor no concatena páginas. Solicita formularios/bloques a tres fuentes semánticas:

```text
breviary → salmodia, preces, Benedictus/Magníficat
missal    → propias, ordinario y liturgia eucarística
lectionary→ lecturas del día
```

El resultado se renderiza en **una única celebración continua y un único scroll**.

## Biblia y anotaciones

La Biblia también migrará de EPUB a entidades por libro/capítulo/versículo. Esto permite localizadores estables como `JN.1.14` para resaltados, notas, reflexiones, comentarios y planes de lectura, evitando depender de posiciones HTML variables.

## Almacenamiento Android

La primera implementación usa SQLite nativo para mantener compatibilidad con el proyecto Java/Gradle actual. Room puede evaluarse cuando se modernice el proyecto; no es requisito para adoptar ahora el modelo semántico.

Los paquetes de producción deben generarse **fuera de la aplicación** y abrirse preferentemente como bases validadas de contenido. `SemanticContentDbHelper` define el contrato de esquema durante esta fase de migración; no implica que el dispositivo deba construir toda la base desde cero.

Paquetes instalados:

```text
filesDir/content/<package-id>/<version>/content.db
filesDir/content/<package-id>/<version>/manifest.json
```

Un puntero local identifica la versión activa. La instalación nueva se valida antes de activar y la versión previa no se borra inmediatamente.

## Migración incremental

1. Definir contrato SQLite y gestor de paquetes sin cambiar el lector actual.
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
- cada bloque mostrado puede rastrearse a `package → source_key → unit_id`;
- ES y LA se emparejan por `unit_id`, no por posición visual;
- el contenido puede actualizarse independientemente del binario de la app;
- Completas, Laudes, Vísperas y Misa unida pasan pruebas por fechas representativas.
