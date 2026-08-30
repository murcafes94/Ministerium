# Distribución

Código y artefactos se mantienen separados. `main` es estable;
`develop`, desarrollo; prerelease, pruebas; Release normal, estable.

Antes de publicar sustituye `REEMPLAZAR`, calcula SHA-256, comprueba la firma y
valida cada paquete. Se instala primero en staging, se conserva la versión
anterior hasta confirmar y se hace rollback si falla. Android siempre debe
pedir confirmación para instalar la APK.

No publiques textos protegidos sin revisar sus permisos.
