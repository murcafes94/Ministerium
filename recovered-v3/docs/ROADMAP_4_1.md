# Ministerium 4.1 — hoja de ruta

Estado: en desarrollo

## Alcance confirmado

Ministerium 4.1 continúa sobre la base funcional de 4.0 y mantiene el enfoque local-first. Esta rama no incorpora inteligencia artificial ni dictado/transcripción por voz.

## Lotes de trabajo

### 1. Interoperabilidad de Mi estudio — implementado, pendiente de prueba en dispositivo
- Exportación Markdown normal.
- Exportación JSON portable.
- Exportación Obsidian con frontmatter, etiquetas, referencias, citas, `contentId` y anclas de Ministerium.
- Mantener compatibilidad con anotaciones existentes.

### 2. Lector desplazamiento / página — primera cobertura implementada
- Preferencia global `Desplazamiento` / `Página` desde el menú del lector.
- Modo Página basado en columnas del ancho del viewport y gestos laterales.
- Al llegar al borde del documento se conserva la navegación al capítulo/entrada anterior o siguiente.
- La página actual se guarda por documento y se restaura al volver a abrirlo; DOM storage se habilita explícitamente para compatibilidad entre versiones de Android WebView.
- Tablet: usa los márgenes editoriales configurados por `ReaderPreferences`.
- Primera cobertura: Biblia, Magisterio y libros basados en WebView.
- La Liturgia de las Horas paralela ES/LAT queda retirada de la experiencia de la app; la segunda entrada litúrgica abre únicamente `Liturgia Horarum` en latín.
- Las cabeceras de los lectores quedan estáticas: no desaparecen ni se desplazan al hacer scroll.
- Pendiente antes de cerrar el lote: probar reflujo con zoom, selección/subrayados y restauración real de página en teléfono/tablet.

### 3. Validación secundaria del calendario — diagnóstico estructurado implementado
- El motor local de Ministerium sigue siendo la fuente runtime y funciona sin red.
- `check_secondary_liturgy_sources.py` consulta de forma informativa LiturgicalCalendarAPI cuando hay conexión.
- La comprobación compara fechas universales reconocibles con el calendario local de Ecuador y reporta diferencias sin sustituir silenciosamente las reglas locales.
- La indisponibilidad de la API o una discrepancia no bloquean la compilación.
- Pendiente: ampliar los casos comparables después de verificar calendarios particulares de Ecuador disponibles en la fuente secundaria.

### 4. Sistema visual compartido — migración en curso
- Añadidos tokens semánticos de superficie, texto, acento y divisor para claro/oscuro.
- Añadidos tokens comunes de espaciado, radios y geometría de tarjetas con ajustes `sw600dp` para tablet.
- `HomeCard`, tarjetas de Horas y `bg_card` ya consumen los nuevos tokens sin cambiar la identidad visual existente.
- `ReaderVisualPalette` centraliza la paleta CSS de los lectores WebView y define explícitamente claro, oscuro y sepia; `ReaderPreferences` ya consume esta paleta común.
- Las tipografías globales se normalizan con pilas de fuentes fiables en Android/WebView y se fuerzan también en descendientes HTML del Misal y de Liturgia Horarum.
- Los lectores TextView de oraciones usan un fallback serif correcto cuando se selecciona Palatino, evitando que Android caiga en una fuente visual distinta.
- Pendiente: comprobar visualmente claro/oscuro/sepia y las cuatro familias de lectura en Oraciones, Misal y Liturgia Horarum en teléfono y tablet.

### 5. Modo oración y anotaciones bíblicas — implementado, pendiente de prueba en dispositivo
- Se restaura el ciclo real de `No molestar`: al entrar en oración, Liturgia, Misa, Ritual o una sesión de plan bíblico se activa el filtro configurado y al salir se restaura el estado anterior.
- La autorización de acceso a No molestar sigue gestionándose desde Preferencias de lectura.
- En la Biblia, tocar un subrayado abre directamente acciones de `Nota`, `Marcador` o `Eliminar subrayado`, sin mostrar primero una ficha del fragmento.
- Tocar un marcador desde la propia Biblia permite añadir una nota o eliminar el marcador sin salir del capítulo.
- Las acciones actualizan `StudyStore` y la anotación visible en el mismo lector.

## Fuera de 4.1

- IA/RAG/asistente conversacional.
- Dictado o transcripción Whisper/voz.
- Grabación continua.
- Liturgia de las Horas bilingüe ES/LAT.

Estas capacidades se estudiarán en otra versión y no deben introducirse accidentalmente en la rama 4.1.
