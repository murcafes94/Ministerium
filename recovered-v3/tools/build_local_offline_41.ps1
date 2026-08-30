param(
    [switch]$Offline,
    [switch]$SkipValidation
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$RepoRoot = Split-Path -Parent $ProjectRoot
Set-Location $ProjectRoot

function Write-Section([string]$Text) {
    Write-Host ''
    Write-Host '============================================================'
    Write-Host $Text
    Write-Host '============================================================'
}

function Fail([string]$Message) {
    throw $Message
}

function Assert-Exit([string]$Label) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Label fallo con codigo $LASTEXITCODE"
    }
}

function Test-Java11([string]$JavaExe) {
    if (-not $JavaExe -or -not (Test-Path $JavaExe)) { return $false }
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $command = '"' + $JavaExe + '" -version 2>&1'
        $versionText = (& cmd.exe /d /c $command | Out-String)
        return ($versionText -match '(?im)(?:java|openjdk) version\s+"?11(?:\.|\s|")')
    } catch {
        return $false
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

function Configure-Java11 {
    $candidates = New-Object System.Collections.Generic.List[string]

    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $candidate) { $candidates.Add($candidate) }
    }

    try {
        $whereJava = & where.exe java 2>$null
        foreach ($item in $whereJava) {
            if ($item -and (Test-Path $item)) { $candidates.Add([string]$item) }
        }
    } catch {}

    $patterns = @(
        "$env:ProgramFiles\Eclipse Adoptium\jdk-11*\bin\java.exe",
        "$env:ProgramFiles\Java\jdk-11*\bin\java.exe",
        "$env:ProgramFiles\Microsoft\jdk-11*\bin\java.exe",
        "$env:ProgramFiles\Amazon Corretto\jdk11*\bin\java.exe",
        "$env:USERPROFILE\.jdks\*11*\bin\java.exe",
        'C:\portapps\android-studio-portable\app\jre\bin\java.exe',
        "$env:ProgramFiles\Android\Android Studio\jre\bin\java.exe"
    )
    foreach ($pattern in $patterns) {
        Get-Item $pattern -ErrorAction SilentlyContinue | ForEach-Object {
            $candidates.Add($_.FullName)
        }
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (Test-Java11 $candidate) {
            $bin = Split-Path -Parent $candidate
            $jdkHome = Split-Path -Parent $bin
            $env:JAVA_HOME = $jdkHome
            $env:Path = "$bin;$env:Path"
            Write-Host "JDK 11: $jdkHome"
            return
        }
    }

    Fail @'
No se encontro JDK 11.
Ministerium 4.1 usa Gradle 6.7.1 / Android Gradle Plugin 4.2.1 y necesita Java 11.
Instala Temurin/OpenJDK 11 o configura JAVA_HOME y vuelve a ejecutar.
'@
}

function Test-ConfiguredSdk([string]$LocalProperties) {
    if (-not (Test-Path $LocalProperties)) { return $false }
    try {
        $line = Get-Content $LocalProperties | Where-Object { $_ -match '^\s*sdk\.dir\s*=' } | Select-Object -First 1
        if (-not $line) { return $false }
        $value = ($line -replace '^\s*sdk\.dir\s*=\s*', '').Trim()
        $value = $value.Replace('\:', ':').Replace('\\', '\')
        return (Test-Path $value)
    } catch {
        return $false
    }
}

function Configure-AndroidSdk {
    $localProperties = Join-Path $ProjectRoot 'local.properties'
    if (Test-ConfiguredSdk $localProperties) {
        Write-Host 'Android SDK: local.properties OK'
        return
    }

    # Fuerza siempre un array real. En Windows PowerShell 5.1, si el
    # pipeline devuelve un solo SDK, la asignacion produce un String y, con
    # Set-StrictMode, acceder a .Count provoca PropertyNotFoundStrict.
    $sdkCandidates = @(
        @(
            $env:ANDROID_SDK_ROOT,
            $env:ANDROID_HOME,
            (Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
            'C:\portapps\android-studio-portable\data\sdk'
        ) | Where-Object { $_ -and (Test-Path $_) }
    )

    if ($sdkCandidates.Count -eq 0) {
        Fail 'No se encontro Android SDK. Configura Android SDK Platform 30 / Build Tools 30.0.3 y vuelve a ejecutar.'
    }

    $sdk = ($sdkCandidates | Select-Object -First 1).Replace('\','/')
    "sdk.dir=$sdk" | Set-Content -Path $localProperties -Encoding ASCII
    Write-Host "local.properties configurado para: $sdk"
}

function Assert-GeneratedAssets {
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
            Fail "Falta un recurso local ya generado: $required`nDescarga nuevamente el ZIP completo de Ministerium 4.1; la compilacion normal no necesita regenerarlo por Internet."
        }
    }
}

function Prepare-TestSigning {
    $b64Key = Join-Path $ProjectRoot 'test-signing\ministerium-test.keystore.b64'
    $keyFile = Join-Path $ProjectRoot 'test-signing\ministerium-test.keystore'
    if (-not (Test-Path $b64Key)) { Fail "No existe la clave codificada: $b64Key" }
    $keyText = (Get-Content $b64Key -Raw) -replace '\s',''
    [IO.File]::WriteAllBytes($keyFile, [Convert]::FromBase64String($keyText))
    if ((Get-Item $keyFile).Length -le 0) { Fail 'La clave de firma generada esta vacia.' }
}

Write-Section 'Ministerium 4.1 - compilacion local SIN regenerar contenido'
Write-Host 'Este modo no ejecuta pip, no consulta calendarios web y no descarga contenido liturgico.'
if ($Offline) {
    Write-Host 'Gradle: modo OFFLINE activado.'
} else {
    Write-Host 'Gradle: usara cache local y podra consultar repositorios solo si falta una dependencia.'
}

Configure-Java11
Configure-AndroidSdk
Assert-GeneratedAssets

if (-not $SkipValidation) {
    Write-Section 'Validaciones locales 4.1'
    if (Get-Command node -ErrorAction SilentlyContinue) {
        & node tools/validate_stabilization_40.mjs
        Assert-Exit 'Validacion base 4.1'
        & node tools/validate_calendar_31.mjs
        Assert-Exit 'Validacion de calendario local'
        & node tools/validate_lectionary_40.mjs
        Assert-Exit 'Validacion OLM del Leccionario'
        & node tools/validate_magisterium_40.mjs
        Assert-Exit 'Validacion de Magisterio'
        & node tools/validate_prayer_experience_40.mjs
        Assert-Exit 'Validacion de oracion y lectores'
        & node tools/validate_release_40.mjs
        Assert-Exit 'Contrato de release 4.1'
    } else {
        Write-Warning 'Node.js no esta instalado. Se omiten validaciones .mjs; Gradle seguira compilando.'
    }
}

Write-Section 'Preparando firma estable de pruebas'
Prepare-TestSigning

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
    $gradleArgs = New-Object System.Collections.Generic.List[string]
    $gradleArgs.Add('--no-daemon')
    if ($Offline) { $gradleArgs.Add('--offline') }
    $gradleArgs.Add('assembleDebug')

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & .\gradlew.bat @gradleArgs
        $gradleExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }

    if ($gradleExit -ne 0) {
        if ($Offline) {
            Fail @'
Gradle no pudo compilar en modo offline.
El BAT NO intentara conectarse a Internet. Si el error indica que falta una dependencia de Gradle en cache,
ejecuta una sola vez COMPILAR-MINISTERIUM-CON-RED.bat cuando tengas conexion; despues podras volver a usar el BAT normal offline.
'@
        }
        Fail "Gradle assembleDebug fallo con codigo $gradleExit"
    }

    $sourceApk = Join-Path $ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path $sourceApk)) {
        Fail "Gradle termino sin producir el APK esperado: $sourceApk"
    }

    $gradleText = Get-Content (Join-Path $ProjectRoot 'app\build.gradle') -Raw
    $version = 'local'
    if ($gradleText -match "versionName\s+'([^']+)'") { $version = $Matches[1] }

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
    Write-Host 'Firma:  Ministerium Test estable'
}
finally {
    Write-Host ''
    Write-Host 'Restaurando EPUB fuente locales...'
    foreach ($item in $backups) { Copy-Item $item.Backup $item.Source -Force }
    if (Test-Path $backupDir) { Remove-Item $backupDir -Recurse -Force }
    Write-Host 'EPUB restaurados.'
}
