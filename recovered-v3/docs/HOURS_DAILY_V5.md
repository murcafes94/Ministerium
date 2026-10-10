# Ministerium 5.0.4: Horas diarias y Leccionario

La fuente diaria es https://github.com/liturgiadelashoras/liturgiadelashoras.github.io, commit 45ea5e0c73c6df1b312f75c7f79cd5889e81d4cd. La importación conserva los textos y sus saltos de línea, descarta navegación, scripts y controles de tamaño externos. Cada oficio guarda fecha, ruta original y SHA-256 de la fuente. El contenido se genera en CI; no se ejecuta HTML externo en el lector.

Este corte contiene 303 fechas de 2026 (enero–octubre) y 3165 oficios, incluidas variantes. No se promete cobertura de fechas no publicadas. Las variantes de una misma fecha se seleccionan explícitamente. Tercia, Sexta y Nona tienen archivos separados. El Invitatorio, cuando está identificado, se extrae del Oficio de lectura de la misma celebración hasta el himno.

El calendario de esta fuente permanece separado del calendario de Ecuador. La pantalla muestra su procedencia; puede elegirse la biblioteca local. Nunca se compone un oficio parcial de ambos repositorios. Fechas fuera de cobertura utilizan la biblioteca local. La fuente preferida, fecha y variante se conservan; resultados de cargas anteriores se descartan al cambiar fecha o fuente.

Se retira el Misal completo de Inicio, búsqueda y payload APK. El Leccionario y la sincronización existente permanecen independientes. Los accesos antiguos del Misal se redirigen al Leccionario conservando la fecha. No se eliminan notas, favoritos, resaltados, progreso, respaldos ni preferencias del usuario.

Completas local usa el lector nativo; el himno aparece después del acto penitencial. Lectores nativos mantienen la lectura sin cuadros y las preferencias tipográficas. Horas recupera la posición tras girar el dispositivo o continuar desde Inicio.

Validación: contratos V5, proyecto, contenido, calendario, experiencia de oración, Leccionario; prueba de todas las fechas/archivos publicados, variantes, codificación y Completas del sábado. La compilación y Android Lint se ejecutan en GitHub Actions. La comprobación visual en dispositivo se realiza instalando Ministerium Test sin desinstalar.

Se corrigieron llamadas API 24 en funciones declaradas compatibles con API 23. Lint aborta ante errores; las advertencias se incluyen en el informe del artefacto.
