# Arquitectura funcional — Ministerium 3.0

## Objetivo

Ministerium 3.0 reorganiza la aplicación para separar claramente el código de la app de los paquetes de contenido actualizable. La meta es facilitar mantenimiento, pruebas, distribución y futuras ampliaciones sin obligar a recompilar el APK por cada cambio textual.

## Base de migración

La referencia funcional de partida es Ministerium 2.3.2.

## Módulos principales

### 1. Núcleo de aplicación

Incluye navegación, tema claro/oscuro, preferencias, almacenamiento local, sistema de actualizaciones, TTS y utilidades compartidas.

### 2. Liturgia de las Horas

Debe contemplar Invitatorio, Oficio de lectura, Laudes, Hora intermedia, Vísperas y Completas, con reglas litúrgicas por memoria, fiesta, solemnidad y domingo.

### 3. Misa

Incluye celebración del día, lecturas, salmo responsorial y formularios alternativos cuando correspondan.

### 4. Biblia

Incluye navegación por libros, capítulos y versículos, recientes, subrayados, marcadores, reflexiones, comentarios integrados, plan de lectura y herramientas de consulta léxica.

### 5. Derecho Canónico

Texto español/latín, búsqueda y comentarios asociados a los cánones.

### 6. Magisterio

Índice jerárquico por categorías y documentos, con navegación de retorno al índice de la sección.

### 7. Rituales y devociones

Rosario, Viacrucis, oraciones y materiales devocionales o rituales admitidos en la app.

### 8. Contenido personal

Reflexiones, notas, oraciones personales y demás contenido privado del usuario.

## Paquetes de contenido

Los paquetes actualizables deben poder versionarse de forma independiente del APK. Cada paquete debe declarar como mínimo:

- id
- versión
- fecha
- URL de descarga o recurso de distribución
- SHA-256
- versión mínima compatible de la app
- notas de cambios

## Navegación

La navegación debe preservar contexto: al salir de un documento o subsección, el botón atrás debe regresar al índice inmediatamente superior y no forzar el retorno al inicio general.

## Compatibilidad

Toda actualización de contenido debe poder indicar la versión mínima de Ministerium requerida para evitar instalar paquetes incompatibles.
