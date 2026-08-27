# Ministerium 4.0

Aplicación Android nativa, editorial y *offline first* para oración, liturgia,
Biblia, Magisterio y estudio. Conserva compatibilidad con **Android Studio
4.2.1**, Android Gradle Plugin 4.2.1, Gradle 6.7.1, JDK 11 y SDK 30.

## Compilar en Windows

1. Descarga o actualiza el repositorio y cambia a `feature/ministerium-4.0`.
2. Instala JDK 11, Android SDK 30 y Python 3.11.
3. Ejecuta `COMPILAR-MINISTERIUM.bat` desde la raíz.
4. El script genera los paquetes limpios y los índices, ejecuta los contratos
   4.0 y construye la APK de prueba.
5. La salida queda en `Ministerium-APK/Ministerium-4.0.0-prueba.apk`, junto a
   su SHA-256.

También puede abrirse `recovered-v3` en Android Studio. La compilación completa
de distribución debe ejecutar antes el preprocesamiento, porque el índice del
Magisterio se genera desde los EPUB locales.

## Novedades 4.0

- Leccionario guiado por la Ordenación de las Lecturas de la Misa: ciclos A/B/C,
  ferial I/II por año civil y reglas diferenciadas para domingos, solemnidades,
  fiestas, memorias y ferias.
- Centro diario enlazado: Liturgia de las Horas y lecturas de la Misa conservan
  la misma fecha seleccionada.
- Magisterio jerárquico y búsqueda local dentro del texto completo, con apertura
  directa del fragmento encontrado.
- Oraciones personales con modo de oración independiente del editor.
- Intenciones privadas integradas en Laudes/Vísperas y en una sesión propia.
- Datos espirituales excluidos de las copias automáticas de Android; respaldo
  únicamente por acción expresa del usuario.
- Lectores con cabecera autoocultable estable, gestos, TTS español y columna
  editorial adaptable a teléfono y tablet.

## Contenido y fuentes

Ministerium no inventa perícopas ni rúbricas. La OLM orienta la selección y
validación, mientras las lecturas en español proceden del paquete sincronizado.
Los documentos del Magisterio incluidos se consultan sin conexión. Los PDF
normativos visibles en la biblioteca requieren Internet.

## Validación

La compilación local ejecuta:

```text
node tools/validate_stabilization_31.mjs
node tools/validate_calendar_31.mjs
node tools/validate_lectionary_40.mjs
node tools/validate_magisterium_40.mjs
node tools/validate_prayer_experience_40.mjs
```

## Distribución responsable

No publiques material protegido sin comprobar permisos. El APK no debe contener
tokens, credenciales de escritura ni una clave privada de producción. La firma
de prueba incluida es pública y solo sirve para actualizar instalaciones de
Ministerium Test.
