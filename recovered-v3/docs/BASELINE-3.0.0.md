# Baseline Ministerium 3.0.0

Fecha de preparación: 2026-08-23.

El baseline fue preparado desde `Ministerium-AS-4.2.1-v3.0.0.zip`.

Validaciones ejecutadas correctamente:

```text
node tools/validate_project.mjs
node tools/validate_content.mjs
```

Resultado funcional de validación:

- 135 clases Java;
- 36 diseños;
- 19 EPUB;
- 4 diccionarios;
- 1.752 cánones bilingües;
- calendario Ecuador 2026;
- módulos de estudio, planes, TTS, Leccionario y Misal.

Durante la preparación para Git se eliminó únicamente una copia temporal oculta no referenciada: `app/src/main/assets/epubs/.Misal-Diario-Romano.epub.d1qWoX`.

No se modificó código funcional de la aplicación.

SHA-256 del archivo fuente: `f920364dbb190e50fcad6d37c1dc23a32010697e630d9d9bc5a44479437360c6`
