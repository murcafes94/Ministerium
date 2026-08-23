# Auditoría de fuentes litúrgicas — Misal español

## Hallazgo

El asset actual `Misal-Diario-Romano.epub` contiene un Ordinario bilingüe LAT–ES, pero la columna española usa fórmulas propias de una edición española/antigua en varios lugares.

Ejemplos detectados en la fuente incorporada:
- `El Señor esté con vosotros`.
- `Orad, hermanos` / `mío y vuestro`.
- `La paz del Señor esté siempre con vosotros`.
- `Podéis ir en paz`.
- En las Plegarias Eucarísticas aparecen formas como `Tomad y comed`, `por vosotros`, `Haced esto...` y una redacción antigua de la fórmula sobre el cáliz.

## Decisión Ministerium 3.0

La interfaz y los textos propios añadidos por Ministerium deben usar español latinoamericano, con referencia de uso Ecuador.

No se debe convertir automáticamente una Plegaria Eucarística española a variante latinoamericana mediante simples sustituciones de pronombres o verbos, porque produciría un texto litúrgico híbrido y potencialmente no aprobado.

## Tratamiento provisional

En la celebración integrada se aplican únicamente sustituciones exactas y seguras de fórmulas breves del Ordinario que ya fueron definidas para la interfaz, por ejemplo:
- `El Señor esté con ustedes`.
- `La paz del Señor esté siempre con ustedes`.
- `Pueden ir en paz`.

Las Plegarias Eucarísticas I–IV se integran técnicamente mediante selector inline, pero su columna española debe considerarse **pendiente de sustitución por una fuente latinoamericana/Ecuador fiable y autorizada** antes de declarar finalizado el módulo.

La columna latina se conserva según la fuente latina ya incorporada.

## Criterio de aceptación

No marcar como finalizada la localización latinoamericana del Misal hasta que:
1. se disponga de una fuente aprobada/fiable para las Plegarias y Ordinario en español latinoamericano;
2. se sustituya la columna española sin alterar la latina;
3. se compare por bloques contra la edición de referencia;
4. se comprueben especialmente las palabras de la consagración, aclamaciones, embolismo, paz y conclusión.
