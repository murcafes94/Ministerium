# Arquitectura de feedback

La APK **no** debe crear Issues de GitHub directamente con un token incrustado.

## Flujo objetivo

`Ministerium → endpoint HTTPS seguro → validación/limitación → GitHub Issues`

## Tipos

- Error → `type:bug`
- Opinión → `type:opinion`
- Calificación → `type:rating`
- Sugerencia → `type:suggestion`

## Metadatos opcionales

Con consentimiento del usuario, el envío puede incluir:

- versión de Ministerium;
- versión de Android;
- modelo de dispositivo;
- módulo;
- canal estable/pruebas;
- captura de pantalla adjunta o enlazada cuando la infraestructura lo permita.

No enviar datos personales ni contenido de notas/meditaciones salvo que el usuario los añada deliberadamente al mensaje.

## Endpoint sugerido

`POST /feedback`

Cuerpo lógico:

```json
{
  "type": "bug|opinion|rating|suggestion",
  "module": "Bible|Liturgy|Missal|...",
  "message": "...",
  "version": "3.0.0",
  "android": "...",
  "device": "...",
  "channel": "stable"
}
```

El backend valida tamaño, tipos, abuso y contenido antes de crear el Issue. El secreto de GitHub existe únicamente en el servidor.

Hasta que ese endpoint exista, `FeedbackActivity` puede continuar usando la hoja de compartir de Android.
