param(
    [switch]$SkipContent,
    [switch]$SkipValidation
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$RepoRoot = Split-Path -Parent $ProjectRoot
Set-Location $ProjectRoot

function Write-Section([string]$Text) {
    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Text
    Write-Host "============================================================"
}

function Assert-Exit([string]$Label) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Label fallo con codigo $LASTEXITCODE"
    }
}

$script:UsePyLauncher = $false
if (Get-Command py -ErrorAction SilentlyContinue) {
    & py -3.11 --version *> $null
    if ($LASTEXITCODE -eq 0) {
        $script:UsePyLauncher = $true
    }
}
if (-not $script:UsePyLauncher -and -not (Get-Command python -ErrorAction SilentlyContinue)) {
    throw 'No se encontro Python. Instala Python 3.11 y vuelve a ejecutar.'
}

function Invoke-Py([string[]]$PyArgs, [string]$Label = 'Python') {
    if ($script:UsePyLauncher) {
        & py -3.11 @PyArgs
    } else {
        & python @PyArgs
    }
    Assert-Exit $Label
}

function Test-Java11([string]$JavaExe) {
    $previousErrorPreference = $ErrorActionPreference
    try {
        # java -version writes to stderr on several Windows distributions.
        # Relax the preference only while reading that diagnostic output.
        $ErrorActionPreference = 'Continue'
        $versionText = (& $JavaExe -version 2>&1 | Out-String)
        return ($versionText -match '(?im)(?:java|openjdk) version\s+"?11(?:\.|\s|")')
    } catch {
        return $false
    } finally {
        $ErrorActionPreference = $previousErrorPreference
    }
}

function Configure-Java11 {
    $candidates = New-Object System.Collections.Generic.List[string]
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand -and $javaCommand.Source) {
        $candidates.Add($javaCommand.Source)
    }

    $patterns = @(
        "$env:ProgramFiles\Eclipse Adoptium\jdk-11*\bin\java.exe",
        "$env:ProgramFiles\Java\jdk-11*\bin\java.exe",
        "$env:ProgramFiles\Microsoft\jdk-11*\bin\java.exe",
        "$env:ProgramFiles\Amazon Corretto\jdk11*\bin\java.exe",
        "$env:USERPROFILE\.jdks\*11*\bin\java.exe",
        "$env:ProgramFiles\Android\Android Studio\jre\bin\java.exe"
    )
    foreach ($pattern in $patterns) {
        Get-Item $pattern -ErrorAction SilentlyContinue | ForEach-Object { $candidates.Add($_.FullName) }
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ((Test-Path $candidate) -and (Test-Java11 $candidate)) {
            $bin = Split-Path -Parent $candidate
            $jdkHome = Split-Path -Parent $bin
            $env:JAVA_HOME = $jdkHome
            $env:Path = "$bin;$env:Path"
            Write-Host "JDK 11: $jdkHome"
            return
        }
    }

    throw @'
No se encontro JDK 11. Ministerium usa Gradle 6.7.1 / Android Gradle Plugin 4.2.1 y esta compilacion local requiere Java 11.
Instala un JDK 11 (por ejemplo Temurin 11) o configuralo en Android Studio y vuelve a ejecutar.
'@
}

function Configure-AndroidSdk {
    $localProperties = Join-Path $ProjectRoot 'local.properties'
    if (Test-Path $localProperties) {
        return
    }

    $sdkCandidates = @(
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
    ) | Where-Object { $_ -and (Test-Path $_) }

    if (-not $sdkCandidates -or $sdkCandidates.Count -eq 0) {
        throw 'No se encontro Android SDK. Abre recovered-v3 en Android Studio una vez o instala/configura el SDK de Android.'
    }

    $sdk = ($sdkCandidates | Select-Object -First 1).Replace('\','/')
    "sdk.dir=$sdk" | Set-Content -Path $localProperties -Encoding ASCII
    Write-Host "local.properties creado para: $sdk"
}

Write-Section 'Ministerium 4.0 - compilacion local Windows'
Configure-Java11
Configure-AndroidSdk

if (-not $SkipContent) {
    Write-Section 'Dependencias de preprocesamiento'
    if ($script:UsePyLauncher) {
        & py -3.11 -c "import pypdf, bs4" *> $null
    } else {
        & python -c "import pypdf, bs4" *> $null
    }
    if ($LASTEXITCODE -ne 0) {
        Invoke-Py -PyArgs @('-m','pip','install','--disable-pip-version-check','pypdf','beautifulsoup4') -Label 'Instalacion de dependencias Python'
    } else {
        Write-Host 'pypdf y beautifulsoup4: OK'
    }

    Write-Section 'Liturgia de las Horas - paquetes limpios'
    Invoke-Py -PyArgs @('tools/build_clean_hours_31.py','--volume','all') -Label 'Horas ES'
    Invoke-Py -PyArgs @('tools/build_clean_latin_hours_31.py') -Label 'Horas LAT'

    Write-Section 'Misal y Rituales'
    Invoke-Py -PyArgs @('tools/build_liturgiapapal_missal_31.py','--language','all') -Label 'Misal Liturgia Papal'
    Invoke-Py -PyArgs @('tools/build_liturgiapapal_rituals_31.py') -Label 'Rituales Liturgia Papal'
    Invoke-Py -PyArgs @('tools/build_missal_reference_catalog.py') -Label 'Catalogo estructural del Misal'

    Write-Section 'Comprobacion secundaria de fuentes'
    if ($script:UsePyLauncher) {
        & py -3.11 tools/check_secondary_liturgy_sources.py
    } else {
        & python tools/check_secondary_liturgy_sources.py
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Warning 'La comprobacion secundaria fallo, pero no bloquea la compilacion.'
    }

    Write-Section 'Indices de busqueda local'
    Invoke-Py -PyArgs @('tools/build_bible_search_index.py') -Label 'Indice biblico'
    Invoke-Py -PyArgs @('tools/build_magisterium_index_40.py') -Label 'Indice completo del Magisterio'
}

if (-not $SkipValidation) {
    Write-Section 'Validaciones base y contrato 4.0'
    if (Get-Command node -ErrorAction SilentlyContinue) {
        & node tools/validate_stabilization_31.mjs
        Assert-Exit 'Validacion base 3.1.1'
        & node tools/validate_calendar_31.mjs
        Assert-Exit 'Validacion de calendario'
        & node tools/validate_lectionary_40.mjs
        Assert-Exit 'Validacion OLM del Leccionario 4.0'
        & node tools/validate_magisterium_40.mjs
        Assert-Exit 'Validacion de Magisterio 4.0'
        & node tools/validate_prayer_experience_40.mjs
        Assert-Exit 'Validacion de oracion, privacidad y lectores 4.0'
    } else {
        Write-Warning 'Node.js no esta instalado. Se omiten las validaciones .mjs; Gradle seguira compilando.'
    }
}

$requiredFiles = @(
    'app/src/main/assets/hours-clean/manifest.json',
    'app/src/main/assets/hours-clean/latin/2026/manifest.json',
    'app/src/main/assets/missal/es/initial.txt',
    'app/src/main/assets/missal/la/initial.txt',
    'app/src/main/assets/rituals/liturgiapapal/manifest.json',
    'app/src/main/assets/bible-search-index.tsv',
    'app/src/main/assets/magisterium-index.tsv'
)
foreach ($required in $requiredFiles) {
    if (-not (Test-Path (Join-Path $ProjectRoot $required))) {
        throw "Falta archivo generado requerido: $required"
    }
}
$magisteriumIndex = Join-Path $ProjectRoot 'app/src/main/assets/magisterium-index.tsv'
$magisteriumRows = @(
    Get-Content $magisteriumIndex | Where-Object { $_ -and -not $_.StartsWith('#') }
).Count
if ($magisteriumRows -lt 20) {
    throw "El indice del Magisterio no fue generado correctamente ($magisteriumRows filas)."
}
Write-Host "Indice del Magisterio: $magisteriumRows fragmentos"

Write-Section 'Preparando firma estable de pruebas'
$b64Key = Join-Path $ProjectRoot 'test-signing\ministerium-test.keystore.b64'
$keyFile = Join-Path $ProjectRoot 'test-signing\ministerium-test.keystore'
if (-not (Test-Path $b64Key)) {
    throw "No existe la clave codificada: $b64Key"
}
$keyText = (Get-Content $b64Key -Raw) -replace '\s',''
[IO.File]::WriteAllBytes($keyFile, [Convert]::FromBase64String($keyText))
if ((Get-Item $keyFile).Length -le 0) {
    throw 'La clave de firma generada esta vacia.'
}

# Los EPUB de Liturgia se usan como entrada para generar los assets limpios, pero
# no deben quedar dentro del APK. En una compilacion local se guardan temporalmente
# y se restauran siempre, incluso si Gradle falla.
$epubRelatives = @(
    'app/src/main/assets/epubs/LH - 1. ADVIENTO.epub',
    'app/src/main/assets/epubs/LH - 2. NAVIDAD.epub',
    'app/src/main/assets/epubs/LH - 3. CUARESMA.epub',
    'app/src/main/assets/epubs/LH - 4. PASCUA.epub',
    'app/src/main/assets/epubs/LH - 5. TIEMPO ORDINARIO.epub',
    'app/src/main/assets/epubs/LH - 6. SANTORAL.epub',
    'app/src/main/assets/epubs/Liturgia-horarum-2026-latin.epub'
)
$backupDir = Join-Path $env:TEMP ("ministerium-epubs-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$backups = New-Object System.Collections.Generic.List[object]

try {
    Write-Section 'Excluyendo EPUB fuente del APK'
    $index = 0
    foreach ($relative in $epubRelatives) {
        $source = Join-Path $ProjectRoot $relative
        if (Test-Path $source) {
            $backup = Join-Path $backupDir ("$index-" + [IO.Path]::GetFileName($source))
            Copy-Item $source $backup -Force
            $backups.Add([PSCustomObject]@{ Source = $source; Backup = $backup })
            Remove-Item $source -Force
            Write-Host "Temporalmente fuera: $relative"
            $index++
        }
    }

    Write-Section 'Compilando APK con Gradle'
    & .\gradlew.bat --no-daemon assembleDebug
    Assert-Exit 'Gradle assembleDebug'

    $sourceApk = Join-Path $ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path $sourceApk)) {
        throw "Gradle termino sin producir el APK esperado: $sourceApk"
    }

    $gradleText = Get-Content (Join-Path $ProjectRoot 'app\build.gradle') -Raw
    $version = 'local'
    if ($gradleText -match "versionName\s+'([^']+)'") {
        $version = $Matches[1]
    }

    $outputDir = Join-Path $RepoRoot 'Ministerium-APK'
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    $destApk = Join-Path $outputDir ("Ministerium-$version-prueba.apk")
    Copy-Item $sourceApk $destApk -Force

    $hash = (Get-FileHash $destApk -Algorithm SHA256).Hash.ToLowerInvariant()
    $shaFile = "$destApk.sha256"
    "$hash  $([IO.Path]::GetFileName($destApk))" | Set-Content -Path $shaFile -Encoding ASCII

    Write-Section 'COMPILACION TERMINADA'
    Write-Host "APK:    $destApk"
    Write-Host "SHA256: $hash"
    Write-Host "Firma:  Ministerium Test estable"
}
finally {
    Write-Host ''
    Write-Host 'Restaurando EPUB fuente locales...'
    foreach ($item in $backups) {
        Copy-Item $item.Backup $item.Source -Force
    }
    if (Test-Path $backupDir) {
        Remove-Item $backupDir -Recurse -Force
    }
    Write-Host 'EPUB restaurados.'
}
