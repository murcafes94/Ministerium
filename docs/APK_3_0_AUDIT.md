# Auditoría estática del APK Ministerium 3.0

APK revisado: `app-debug(1).apk` (subido por el usuario el 23-08-2026).

## Datos generales
- Tamaño aproximado: 54 MB.
- Versión declarada en recursos: `3.0.0`.
- Android Gradle Plugin: `4.2.1`.
- Incluye assets de Biblia, Liturgia de las Horas, Liturgia latina 2026, Misal LAT–ES, Magisterio, Derecho Canónico, diccionarios, Vía Crucis, rituales y calendario Ecuador 2026.

## Funciones con evidencia clara dentro del APK
- `ComplineEnhancer`: mejora de Completas con invocación, fórmulas penitenciales, selector de himno, selector de antífona mariana y conclusión.
- `IntermediateHourResolver`: resolución de Horas intermedias y himno automático.
- `LiturgyConclusionEnhancer`: selector `Laico / individual | Ministro ordenado` y uso de español latinoamericano (`El Señor esté con ustedes`).
- `CombinedHoursActivity` / `CombinedHoursRepository`: unión de Oficio de Lecturas + Laudes.
- `CombinedMassActivity`: existe una pantalla específica para Misa unida a una Hora.
- `ReaderTtsController` + `TtsPlaybackService`: lector TTS y reproducción en segundo plano.
- El código de extracción TTS elimina `sup` y `.verse-number`, por lo que existe soporte para no pronunciar números de versículo.
- `UpdateCenterActivity`: centro de actualizaciones con SHA-256 y módulos.
- `FeedbackActivity`: pantalla de feedback.
- Biblia, planes, reflexiones, Mesa de estudio, oraciones personales, Magisterio, Derecho Canónico y Liturgia LAT–ES tienen clases/recursos específicos.

## Diferencias importantes respecto a lo acordado

### 1. Misa + Laudes / Vísperas — desviación principal
La pantalla `activity_combined_mass.xml` fue construida con un flujo de OGLH 93 que ofrece dos comienzos:
- `Versículo + himno de la Hora`.
- `Entrada + procesión + saludo de la Misa`.

Además muestra el aviso: `Celebración comunitaria — OGLH 93`.

Esto NO coincide con el flujo concreto pedido para el seminario:
1. Antífona de la celebración del día + señal de la cruz.
2. Salmodia de la Hora.
3. Kyrie, Gloria cuando corresponda, colecta y Liturgia de la Palabra.
4. Homilía, Credo cuando corresponda y preces de la Hora.
5. Liturgia eucarística.
6. Comunión.
7. Benedictus/Magníficat antes de la poscomunión.
8. Poscomunión y conclusión de la Misa.

**Prioridad: P0.** Debe rehacerse el compositor de esta celebración conforme al flujo acordado para la app.

### Requisito obligatorio de UX: una sola celebración continua
Este punto queda fijado como criterio de aceptación no negociable para la recuperación de 3.0:

> **Misa + Laudes** y **Misa + Vísperas** deben mostrarse completas en una única pantalla/flujo continuo, sin que el usuario tenga que salir de la celebración, abrir Liturgia de las Horas, volver al Misal o entrar aparte en Lecturas.

La pantalla combinada debe ensamblar y presentar, en este orden, los bloques tomados de los módulos existentes:

`Calendario → Misal → Liturgia de las Horas → Leccionario → Misal → Liturgia de las Horas → Misal`

En concreto:
- el **Calendario** resuelve la celebración y los textos aplicables;
- la **Liturgia de las Horas** aporta salmodia, preces y Benedictus/Magníficat;
- el **Leccionario** aporta primera lectura, salmo responsorial, segunda lectura cuando exista, aclamación y Evangelio;
- el **Misal** aporta antífona de entrada, Kyrie/Gloria cuando corresponda, colecta, Credo, liturgia eucarística, plegaria elegida, rito de comunión, poscomunión y conclusión.

El usuario debe poder hacer **scroll de principio a fin** y celebrar todo desde el mismo flujo. Los módulos fuente siguen separados internamente para mantenimiento y actualizaciones, pero su separación **no debe ser visible como navegación obligatoria durante la celebración**.

No se acepta como implementación completa:
- un botón que abra Lecturas en otra Activity;
- un enlace que mande al Misal a mitad de la Hora;
- volver atrás para recuperar las preces;
- abrir Benedictus/Magníficat en otra pantalla;
- pegar tres páginas web completas una detrás de otra sin composición por bloques.

Sí se acepta:
- una Activity/Screen de celebración combinada;
- bloques cargados dinámicamente desde repositorios separados;
- selectores inline que cambian solo el bloque afectado (Plegaria, Credo, ESP/LAT, etc.);
- scroll continuo y posición preservada al cambiar una alternativa.

### 2. Plegarias Eucarísticas I–IV
El APK contiene referencias generales a `Plegarias Eucarísticas`, pero en la inspección estática no aparece evidencia clara de un selector inline `I | II | III | IV` dentro de la celebración combinada.

**Estado: incompleto/no verificado.** Debe comprobarse en ejecución y, si no existe, implementarse.

### 3. Credos + ESP/LAT
Existen assets de `credo_niceno.txt` y `credo_apostolico.txt`, y el Misal contiene material LAT–ES; sin embargo no hay evidencia clara en la UI inspeccionada de un selector integrado:
- `Niceno-constantinopolitano | Apostólico`.
- con cambio de idioma por bloque `ESP ⇄ LAT`.

**Estado: incompleto/no verificado.** Debe verificarse en ejecución y completar según especificación.

### 4. Padre nuestro ESP/LAT
No aparece evidencia clara de que el Padre nuestro de la Misa en español tenga el control por bloque `ESP ⇄ LAT` acordado.

**Estado: incompleto/no verificado.**

### 5. Antífonas marianas de Completas
Hay evidencia de textos/opciones en español equivalentes a:
- Salve Regina.
- Alma Redemptoris Mater (`Madre del Redentor`).
- Ave Regina caelorum (`Salve, Reina de los cielos`).
- Sub tuum praesidium (`Bajo tu amparo`).
- Regina caeli.

También existen marcadores de `easterSeason`, lo que sugiere lógica pascual. Debe confirmarse que `Regina caeli` solo se muestre durante Tiempo Pascual.

No se encontró evidencia clara de un control `ESP ⇄ LAT` específico para cada antífona mariana dentro de Completas.

**Estado: parcialmente implementado.**

### 6. Feedback
La pantalla de feedback dice que el usuario elige la aplicación con la que se envía y que Ministerium no publica sin confirmación. Esto corresponde a un flujo de compartir/enviar manualmente.

No coincide con la arquitectura acordada:
`Ministerium → endpoint HTTPS seguro → validación/rate limit → GitHub Issue`.

No hay URL de endpoint segura visible en las cadenas del APK.

**Prioridad: P1.** La pantalla puede mantenerse, pero el backend de feedback acordado todavía no está implementado.

### 7. Actualizaciones
Existe `UpdateCenterActivity`, SHA-256 y `package-manifest.json`, pero el manifest embebido de 3.0.0 contiene URLs vacías para la app y solo registra algunos paquetes (calendario, breviario y misal).

La UI menciona también Leccionario y Rituales, pero la infraestructura de actualización remota completa no puede considerarse terminada con el manifest actual.

**Prioridad: P1.** Conectar a manifests remotos Stable/Testing y completar paquetes independientes.

## Funciones que parecen bien encaminadas
- Himnos automáticos de Horas intermedias.
- Conclusión dinámica Laudes/Vísperas para laico/individual o ministro ordenado.
- Oficio de Lecturas + Laudes.
- TTS en español con eliminación de números de versículo y bloques semánticos.
- Liturgia bilingüe con mecanismos de alineación por bloques.
- Biblioteca bíblica, subrayados/reflexiones y Mesa de estudio.

## Orden de recuperación recomendado

### P0 — Celebración y liturgia
1. Rehacer Misa + Laudes/Vísperas como **una celebración única y continua**, sin navegación entre módulos.
2. Verificar y completar Plegarias Eucarísticas I–IV.
3. Verificar y completar Credo Niceno/Apostólico + ESP/LAT.
4. Añadir Padre nuestro ESP/LAT.
5. Completar antífonas marianas de Completas con ESP/LAT y Regina caeli solo en Pascua.

### P1 — Infraestructura
6. Completar manifests remotos Stable/Testing.
7. Separar paquetes reales: calendario, Breviarium, Leccionario, Rituales, etc.
8. Implementar endpoint seguro de feedback que cree Issues sin token dentro del APK.

### P2 — QA
9. Probar en dispositivo cada Hora y celebración combinada.
10. Comparar comportamiento real contra la especificación maestra, no solo presencia de clases.
11. Verificar expresamente que toda Misa+Hora puede recorrerse de inicio a fin con un solo scroll y sin cambiar de pantalla.

## Nota metodológica
Esta es una auditoría **estática** del APK: confirma clases, recursos, textos, assets y flujos declarados, pero no sustituye una prueba funcional en un dispositivo Android. Todo elemento marcado como `no verificado` debe probarse en ejecución antes de considerarlo ausente o correcto.
