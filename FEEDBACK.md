# Feedback e incidencias — Ministerium 3.0

## Objetivo

Centralizar sugerencias, errores y observaciones recibidas durante el desarrollo y las pruebas de Ministerium.

## Tipos

- Error funcional
- Error de contenido
- Problema litúrgico
- Problema de navegación o interfaz
- Sugerencia de mejora
- Solicitud de nuevo contenido

## Datos recomendados

Cada reporte debería incluir, cuando sea posible:

- Versión de Ministerium
- Canal (`stable` o `testing`)
- Dispositivo y versión de Android
- Sección afectada
- Descripción breve
- Pasos para reproducir
- Resultado esperado
- Resultado obtenido
- Captura o evidencia opcional

## Flujo previsto

1. El usuario envía feedback desde la app.
2. Un servicio intermedio valida y normaliza la información.
3. El servicio crea o consolida una incidencia en GitHub.
4. Las incidencias duplicadas se agrupan cuando sea posible.
5. Una corrección validada pasa primero al canal de pruebas y luego al estable.

## Seguridad

La app no debe contener tokens de GitHub con permisos de escritura. La creación automática de Issues deberá usar un backend o función intermedia con secretos almacenados del lado del servidor.
