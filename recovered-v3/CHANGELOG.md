# Changelog

Este archivo resume las versiones de Ministerium. Los detalles históricos se conservan en `NOVEDADES-<versión>.md`.

## [4.1.0] - en desarrollo

### Estudio e interoperabilidad
- Mi estudio puede exportarse como Markdown normal, JSON portable o Markdown preparado para Obsidian.
- La exportación Obsidian conserva IDs de contenido de Ministerium, referencias, citas, etiquetas y anclas semánticas.

### Lectores y calendario
- Esta rama queda preparada para continuar con modo de lectura desplazamiento/página y validación secundaria del calendario litúrgico.

### Alcance
- La versión 4.1 no incorpora IA ni dictado/transcripción por voz.
- Se mantienen el funcionamiento local-first y las reglas de privacidad de 4.0.

## [4.0.0] - 2026-08-27

### Oración y privacidad
- Las oraciones personales se abren en un lector de oración; editar y eliminar son acciones separadas.
- Las intenciones pueden presentarse juntas y siguen integradas en Laudes y Vísperas.
- Los datos espirituales privados quedan fuera de las copias automáticas de Android.

### Magisterio
- Biblioteca jerárquica por materias y búsqueda local de texto completo con relevancia.
- Apertura directa del fragmento encontrado en Vaticano II, Catecismo, Compendio y Doctrina Social.
- Acceso visible a la OGLH y a la Ordenación de las Lecturas de la Misa.

### Liturgia y lectores
- Ciclos dominicales A/B/C y feriales I/II corregidos según OLM 65 y 69.
- Reglas de domingos, solemnidades, fiestas, memorias y ferias trazables a OLM 79 y 82–89.
- Acceso de la Liturgia de las Horas a las lecturas de la Misa conservando la fecha.
- Cabeceras autoocultables estables y ancho editorial adaptable a teléfono y tablet.

### Compilación
- Versión 4.0.0 (código 40).
- Se conservan Gradle 6.7.1, AGP 4.2.1, JDK 11 y SDK 30.
- El compilador local genera y valida los índices bíblico y magisterial antes de Gradle.

## [3.0.0] - 2026-08-23

### Añadido
- Lectores editoriales compactos con navegación por gestos y ajuste tipográfico por pellizco.
- Menú contextual de lectura, `Mi estudio`, Mesa de estudio y referencias estructuradas.
- Continuar leyendo con conservación de módulo, documento y posición.
- Planes bíblicos personalizados con progreso real.
- TTS español por bloques semánticos.
- Centro modular de actualizaciones y feedback sin credenciales de GitHub dentro de la APK.

### Liturgia
- Oficio de lecturas + Laudes conforme OGLH 99.
- Misa + Laudes/Vísperas conforme OGLH 93–97.
- Completas y Horas intermedias revisadas.
- Liturgia bilingüe simplificada a `Bilingüe | Latín`.

### Misa y Leccionario
- Misal y Lecturas del día como módulos independientes y enlazados.
- Selector de Misa / Misa + Laudes / Misa + Vísperas.
- Modos Español y Latín–Español.

### Compatibilidad
- Conserva datos personales procedentes de 2.3.2.
- Android 6.0 (API 23) o superior.
- Proyecto compatible con Android Studio 4.2.1 / AGP 4.2.1 / Gradle 6.7.1 / SDK 30.

Consulta `NOVEDADES-3.0.0.md` para el detalle funcional.
