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
- Liturgia bilingüe permanece en desplazamiento sincronizado.
- Pendiente antes de cerrar el lote: probar reflujo con zoom, selección/subrayados y restauración real de página en teléfono/tablet.

### 3. Validación secundaria del calendario — diagnóstico estructurado implementado
- El motor local de Ministerium sigue siendo la fuente runtime y funciona sin red.
- `check_secondary_liturgy_sources.py` consulta de forma informativa LiturgicalCalendarAPI cuando hay conexión.
- La comprobación compara fechas universales reconocibles con el calendario local de Ecuador y reporta diferencias sin sustituir silenciosamente las reglas locales.
- La indisponibilidad de la API o una discrepancia no bloquean la compilación.
- Pendiente: ampliar los casos comparables después de verificar calendarios particulares de Ecuador disponibles en la fuente secundaria.

### 4. Sistema visual compartido — migración iniciada
- Añadidos tokens semánticos de superficie, texto, acento y divisor para claro/oscuro.
- Añadidos tokens comunes de espaciado, radios y geometría de tarjetas con ajustes `sw600dp` para tablet.
- `HomeCard`, tarjetas de Horas y `bg_card` ya consumen los nuevos tokens sin cambiar la identidad visual existente.
- Pendiente: trasladar progresivamente lectores y pantallas restantes a los tokens comunes y revisar sepia de forma explícita.

## Fuera de 4.1

- IA/RAG/asistente conversacional.
- Dictado o transcripción Whisper/voz.
- Grabación continua.

Estas capacidades se estudiarán en otra versión y no deben introducirse accidentalmente en la rama 4.1.
