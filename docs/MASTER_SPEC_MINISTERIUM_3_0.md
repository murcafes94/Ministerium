# Ministerium 3.0 — Especificación maestra consolidada

Esta especificación fusiona:
1. cambios acordados en la conversación actual;
2. decisiones ya cerradas en conversaciones anteriores de Ministerium 2.1.0–2.3.2;
3. infraestructura prevista para GitHub, actualizaciones y feedback.

No contiene textos litúrgicos completos ni materiales protegidos.

---

# A. PRINCIPIOS GENERALES

## A1. Motor por bloques
Ministerium debe construir las celebraciones mediante bloques reutilizables:

`Calendario → celebración → rango → tiempo → módulo → bloques → alternativas permitidas → idioma → presentación`

No duplicar formularios completos si pueden ensamblarse dinámicamente.

## A2. Regla UX
> No preguntar al usuario aquello que el calendario, el tiempo litúrgico y las reglas del módulo pueden determinar automáticamente.

- Una opción válida → mostrar directamente.
- Varias opciones legítimas → selector compacto.
- Opción no permitida → ocultar o deshabilitar con explicación breve.

## A3. Español
- Español litúrgico latinoamericano.
- Referencia de uso: Ecuador.
- Evitar mezclar fórmulas de español de España con latinoamericano.
- No reemplazar mecánicamente palabras dentro de textos oficiales: usar la edición aprobada correspondiente.

## A4. Navegación
Navegación jerárquica por niveles.

Ejemplo Magisterio:
`Dei Verbum → Concilio Vaticano II → Magisterio → Inicio`

Ejemplo Misal:
`Formulario → categoría → Misal → Inicio`

El botón Atrás debe regresar al índice contextual inmediatamente superior, no mandar al inicio de la app.

## A5. Lectura
- Márgenes adaptables.
- Nunca pegar el texto al borde de la pantalla.
- Mantener espacio inferior suficiente para que la barra de navegación no tape contenido.
- Estética editorial consistente entre módulos.

---

# B. CALENDARIO Y MOTOR LITÚRGICO

- Resolver feria, memoria, fiesta, solemnidad y celebraciones alternativas.
- Resolver propios y comunes.
- El propio del Santoral debe prevalecer sobre el común cuando realmente existe texto propio.
- Resolver I y II Vísperas donde corresponda.
- Resolver salmodia y antífonas de acuerdo con el formulario aplicable.
- Preparar pruebas específicas para celebraciones del Santoral previamente ajustadas, incluida María Reina (22 de agosto), según la implementación acordada.
- Permitir que Misal, Liturgia de las Horas y Lecturas del día consuman la misma resolución de calendario.

---

# C. LITURGIA DE LAS HORAS

## C1. Horas disponibles
- Invitatorio.
- Oficio de Lecturas.
- Laudes.
- Tercia.
- Sexta.
- Nona.
- Vísperas.
- Completas.

## C2. Diseño común
Tomar Completas como patrón:
- encabezado fijo;
- títulos de sección;
- rúbricas diferenciadas;
- `℣.` / `℟.`;
- bloques separados;
- selectores inline;
- modo claro/oscuro;
- márgenes cómodos.

## C3. Completas
- Invocación.
- Examen de conciencia.
- 3 fórmulas penitenciales mediante selector inline.
- Himnos filtrados por tiempo/día.
- Salmodia.
- Lectura breve.
- Responsorio.
- Nunc dimittis.
- Oración.
- Conclusión.
- Antífona mariana.

Antífonas marianas:
- Salve Regina.
- Alma Redemptoris Mater.
- Ave Regina caelorum.
- Sub tuum praesidium.
- Regina caeli.

Regla:
- Regina caeli visible únicamente durante Tiempo Pascual.
- Antífonas disponibles con `ESP ⇄ LAT`.

## C4. Horas intermedias
- Tercia/Sexta/Nona.
- Eliminar enlaces manuales del tipo “Semanas I–XVII / XVIII–XXXIV”.
- Mostrar directamente el himno correspondiente a semana, tiempo y Hora.
- Preparar lógica de salmodia complementaria cuando se rece más de una Hora intermedia.

## C5. Laudes y Vísperas
- Himnos filtrados automáticamente.
- Salmodia correcta.
- Lectura breve.
- Responsorio.
- Benedictus / Magníficat.
- Preces.
- Padre nuestro.
- Oración.

Conclusión:
`Laico / individual | Ministro ordenado`

- Cambia solo el bloque final.
- No abrir pantalla adicional.
- No mostrar el selector cuando la Hora esté unida a la Misa.

## C6. Oficio de Lecturas + Laudes
Añadir opción de primer rezo del día:
`Invitatorio + Oficio de Lecturas + Laudes`

Construir por bloques y evitar duplicar elementos que las reglas indiquen omitir al unir las Horas.

## C7. Meditación opcional
Añadir ayuda opcional con Evangelio del día.

Debe quedar claro que es:
- ayuda para oración personal;
- no parte estructural de la Liturgia de las Horas.

Compatible con:
- subrayado;
- reflexión;
- comentario;
- Mesa de estudio.

---

# D. LITURGIA BILINGÜE ESPAÑOL–LATÍN

- Módulo bajo Liturgia de las Horas.
- Sincronización por fecha y Hora.
- Latín y español alineados por bloques.
- En horizontal: lado a lado.
- En vertical: diseño adaptado sin comprimir excesivamente.
- Fuente latina anual previamente prevista: Breviar.sk, verificando estrictamente que los archivos sean latinos y no eslovacos.
- No usar traductor automático como sustituto de textos litúrgicos oficiales.
- Mantener opción de traducción lingüística general separada del modo litúrgico ESP/LAT.

---

# E. MISA / MISAL

## E1. Entrada unificada
Al entrar en Misal:

`Misa | Misa + Laudes | Misa + Vísperas`

Idioma:
`Español | Latín–Español`

Celebración:
- del día;
- alternativas permitidas;
- diversas necesidades;
- comunes;
- votivas;
- difuntos;
- otras categorías previstas por el Misal cuando puedan celebrarse.

El motor filtra según calendario.

## E2. Misal LAT–ESP depurado
Un solo motor para:
- Misa español;
- Misa LAT–ESP;
- Misa+Laudes;
- Misa+Vísperas.

Bloques sincronizados.
Evitar desalineación acumulativa entre columnas.

## E3. Misa + Laudes / Vísperas
Secuencia funcional acordada para el seminario:

1. Inicio:
   - antífona de la celebración;
   - señal de la cruz.

2. Salmodia:
   - feria/memoria/solemnidad/común/propio según corresponda.

3. Misa:
   - Kyrie;
   - Gloria si corresponde;
   - colecta;
   - Liturgia de la Palabra;
   - homilía;
   - Credo cuando corresponda;
   - preces de la Hora en el lugar acordado de la oración universal.

4. Liturgia eucarística:
   selector `I | II | III | IV`.

5. Comunión.

6. Antes de la poscomunión:
   - Laudes → antífona + Benedictus + antífona.
   - Vísperas → antífona + Magníficat + antífona.

7. Poscomunión.

8. Rito de conclusión.

No mostrar conclusión independiente de la Hora.

## E4. Plegarias eucarísticas
Selector inline:
`I | II | III | IV`

El motor debe respetar restricciones de uso y prefacios.

## E5. Credo
Cuando corresponda:
`Niceno-constantinopolitano | Apostólico`

Cada bloque admite `ESP ⇄ LAT`.

## E6. Padre nuestro
En Misa española:
`ESP ⇄ LAT`

Cambiar solo el Padre nuestro, no toda la celebración.

## E7. Icono ESP/LAT
Icono propio inspirado en dos tarjetas/hojas:
- ESP.
- LAT.

No usar carácter chino.
No confundir con traductor general.

---

# F. LECTURAS DEL DÍA / LECCIONARIO

## F1. Apartado principal
Lecturas del día deben estar accesibles desde sección principal.

Estructura:
- título de celebración;
- primera lectura;
- salmo responsorial;
- segunda lectura;
- aclamación;
- Evangelio.

## F2. Fuentes
Preferencias/acuerdos previos:
- contenido español compatible con Ecuador;
- fuente diaria actual basada en Arquidiócesis de Guadalajara;
- USCCB en español como alternativa/respaldo cuando corresponda.

## F3. Actualización mensual
- Descargar lecturas mensualmente.
- Internet solo para descargar/actualizar.
- Guardar offline.
- No reinstalar APK para actualizar lecturas.

## F4. Latín
- No traducir automáticamente el Leccionario.
- No asumir que el EPUB LAT–ESP contiene lecturas latinas completas.
- Lecturas completas en español por ahora.
- Preparar `reading.la = null` para futura fuente latina fiable.
- Verificar compatibilidad por cita, capítulo y versículos.
- Salmo responsorial: comprobar por separado referencia, versículos y respuesta.

## F5. Recordatorio Evangelio
Mantener el requerimiento previo de aviso del Evangelio del día a las 05:00, sujeto a revisión final de UX junto con los demás recordatorios.

---

# G. BIBLIA DE JERUSALÉN

## G1. Offline
Biblia disponible offline desde el EPUB incorporado/autorizado para uso previsto.

## G2. Navegación
- libros;
- capítulos;
- versículos;
- recientes;
- anterior/siguiente capítulo;
- vista vertical/horizontal;
- búsqueda por versículo cuando sea posible.

## G3. Comentarios integrados
Mostrar comentario dentro del pasaje sin obligar a abandonar la Biblia.

## G4. Diccionario/contexto
Al seleccionar una palabra:
- diccionarios bíblicos;
- diccionario teológico;
- RAE cuando esté disponible;
- comentario/contexto.

## G5. Subrayados y marcadores
Guardar:
- cita;
- texto seleccionado;
- color/estilo cuando corresponda;
- acceso desde listado de subrayados/marcadores.

## G6. Reflexiones
Seleccionar frase/versículo → escribir reflexión.

Apartado:
`Mis reflexiones`

## G7. Planes de lectura
- elegir plan;
- cancelar plan;
- progreso;
- recordatorio.
Planes previstos anteriormente:
- Biblia en 365 días.
- Evangelios en 89 días.

---

# H. TTS / LECTOR EN VOZ ALTA

## H1. Idioma
Solo español por ahora.
TTS latino descartado.

## H2. Biblia
- no pronunciar números de versículo;
- sí mantenerlos visualmente;
- respetar puntuación;
- respetar párrafos;
- no interpretar cada salto visual de línea como pausa fuerte;
- tratamiento mejorado de poesía/salmos/cánticos.

## H3. Cola semántica
Leer por bloques:
- antífona;
- salmo;
- lectura;
- responsorio;
- preces;
- oración.

## H4. Controles
- reproducir;
- pausar;
- detener;
- anterior;
- siguiente;
- velocidad;
- tono;
- volumen.

## H5. Segundo plano
Seguir leyendo con pantalla apagada/segundo plano cuando Android lo permita.
Controles en notificación.

## H6. Exclusiones
No leer:
- botones;
- navegación;
- números de versículo;
- elementos UI.
Preparar omisión de rúbricas.

---

# I. MAGISTERIO

## I1. Contenido
Mantener separados de Oración:
- Concilio Vaticano II.
- Catecismo.
- Compendio del Catecismo.
- Doctrina Social.

## I2. Navegación
Por:
- títulos;
- capítulos;
- numerales;
- documentos.

Navegación jerárquica.

Ejemplo:
`Magisterio → Concilio Vaticano II → Dei Verbum → capítulo/numeral`

## I3. Estilo
Estilo editorial semejante a Lecturas del día.

## I4. Índice contextual
Icono/botón para volver al índice inmediato del documento.

## I5. Referencias cruzadas
Vista previa emergente cuando una referencia interna sea verificable.

## I6. Mesa de estudio
Agrupar resultados por fuente y relaciones verificables:
- Biblia;
- Catecismo;
- Concilio;
- Magisterio;
- Padres/comentarios cuando existan.

---

# J. CÓDIGO DE DERECHO CANÓNICO

## J1. Separación
Código y comentarios son módulos lógicamente separados.

## J2. Código
- búsqueda por canon/numeral;
- navegación jerárquica:
  Libro → Parte → Sección → Título → Capítulo → Artículo → Canon.

## J3. LAT–ESP
- vertical: Español sobre Latín.
- horizontal: lado a lado.
- búsqueda textual principalmente en español.

## J4. Comentarios
Si existe comentario para el canon:
- mostrar icono de pergamino junto a “Canon X”;
- abrir comentario integrado/emergente.
Si no existe, no mostrar el icono.

---

# K. ORACIONES PERSONALES

Apartado:
`Mis oraciones`

CRUD:
- crear;
- editar;
- eliminar;
- título;
- texto.

Offline.

---

# L. VÍA CRUCIS DE BENEDICTO XVI / JOSEPH RATZINGER

- Disponible offline.
- 14 estaciones.
- Navegación ordenada.
- Incluir bendición/final según el recurso incorporado.
- Mantenerlo dentro del módulo devocional/oración, separado de Biblia, Magisterio y Derecho.

---

# M. NOTIFICACIONES Y APARIENCIA

## M1. Notificaciones independientes
- Laudes: ejemplo acordado 06:00.
- Vísperas: ejemplo acordado 18:40.
- Invitatorio: sin recordatorio independiente.
- Evangelio del día: requerimiento previo 05:00.
- Planes de lectura: recordatorio propio.

Los horarios deben quedar configurables.

## M2. Apariencia
- Botón de modo claro/oscuro separado de notificaciones.
- Ajuste del color/contraste en oscuro.
- Márgenes adaptables.

---

# N. GITHUB / ACTUALIZACIONES

## N1. Repositorio
Preferiblemente privado al inicio.

## N2. Canales
- Stable.
- Testing.

## N3. Releases
Separar:
- APK;
- paquetes de contenido.

## N4. Paquetes de contenido
Actualizables independientemente:
- Calendar.
- Breviarium.
- Lectionary.
- Rituals.
- Magisterium, si se autoriza su distribución.
- Canon-law/comments, si se autoriza su distribución.
- otros módulos futuros.

## N5. Manifest
Cada artefacto:
- versión;
- URL;
- SHA-256;
- tamaño;
- mínimo de app compatible;
- fecha;
- canal.

## N6. Seguridad
- Nunca token GitHub de escritura dentro del APK.
- HTTPS.
- hashes.
- secretos solo en backend/Actions.
- no publicar contenido protegido sin verificar permisos.

---

# O. FEEDBACK

Tipos:
- error;
- opinión;
- calificación;
- sugerencia.

Flujo deseado:
`App → endpoint seguro → GitHub Issue`

Metadatos mínimos:
- versión app;
- módulo;
- canal;
- Android;
- tipo;
- descripción;
- pasos para reproducir si es error.

Sin datos personales innecesarios.

Etiquetas:
- type:bug
- type:opinion
- type:rating
- type:suggestion
- module:breviarium
- module:missal
- module:lectionary
- module:bible
- module:calendar
- module:magisterium
- module:canon-law
- channel:stable
- channel:testing

---

# P. CRITERIOS DE ACEPTACIÓN

- Compila partiendo de v2.3.2.
- Mantiene navegación jerárquica.
- Márgenes correctos.
- No duplica textos entre modos.
- No mezcla español europeo/latinoamericano.
- No inventa traducciones litúrgicas.
- LAT/ESP alineado por bloques.
- Lecturas actualizables sin APK.
- TTS omite números de versículo.
- Misa+LdH ensamblada dinámicamente.
- Himnos automáticos donde el calendario puede determinarlos.
- GitHub sin secretos dentro de la app.
