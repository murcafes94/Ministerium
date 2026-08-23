# Configuración de firma para GitHub Actions

El módulo Android debe configurar la firma `release` utilizando exclusivamente variables de entorno inyectadas por GitHub Actions.

## Ejemplo para `app/build.gradle` (Groovy)

```groovy
android {
    signingConfigs {
        release {
            def ks = System.getenv("MINISTERIUM_KEYSTORE_FILE")
            if (ks != null && !ks.isEmpty()) {
                storeFile file(ks)
                storePassword System.getenv("MINISTERIUM_KEYSTORE_PASSWORD")
                keyAlias System.getenv("MINISTERIUM_KEY_ALIAS")
                keyPassword System.getenv("MINISTERIUM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
        }
    }
}
```

## Ejemplo para `app/build.gradle.kts` (Kotlin DSL)

```kotlin
android {
    signingConfigs {
        create("release") {
            val ks = System.getenv("MINISTERIUM_KEYSTORE_FILE")
            if (!ks.isNullOrBlank()) {
                storeFile = file(ks)
                storePassword = System.getenv("MINISTERIUM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MINISTERIUM_KEY_ALIAS")
                keyPassword = System.getenv("MINISTERIUM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}
```

No copiar contraseñas, archivos `.jks` ni tokens dentro del repositorio.
