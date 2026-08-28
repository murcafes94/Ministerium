# Ministerium 4.1 — hoja de ruta

Estado: en desarrollo

## Alcance confirmado

Ministerium 4.1 continúa sobre la base funcional de 4.0 y mantiene el enfoque local-first. Esta rama no incorpora inteligencia artificial ni dictado/transcripción por voz.

## Lotes de trabajo

### 1. Interoperabilidad de Mi estudio — iniciado
- Exportación Markdown normal.
- Exportación JSON portable.
- Exportación Obsidian con frontmatter, etiquetas, referencias, citas, `contentId` y anclas de Ministerium.
- Mantener compatibilidad con anotaciones existentes.

### 2. Lector desplazamiento / página — pendiente
- Preferencia global `Desplazamiento` / `Página`.
- Mantener selección, subrayados, notas, zoom y continuar leyendo en ambos modos.
- Tablet: respetar ancho editorial y márgenes actuales.
- Biblia, Magisterio y libros como primera cobertura; Liturgia bilingüe se mantiene inicialmente en desplazamiento sincronizado.

### 3. Validación secundaria del calendario — pendiente
- El motor local de Ministerium sigue siendo la fuente runtime y debe funcionar sin red.
- Añadir una comprobación de build/diagnóstico contra una fuente litúrgica secundaria estructurada.
- Las discrepancias deben reportarse; nunca sustituir silenciosamente las reglas locales ni convertir una API externa en dependencia de uso diario.

### 4. Sistema visual compartido — pendiente
- Consolidar tokens de color, espaciado, radios, tipografía y anchos de lector.
- Reducir valores repetidos entre pantallas y lectores.
- Mantener modo claro, oscuro y sepia.

## Fuera de 4.1

- IA/RAG/asistente conversacional.
- Dictado o transcripción Whisper/voz.
- Grabación continua.

Estas capacidades se estudiarán en otra versión y no deben introducirse accidentalmente en la rama 4.1.
