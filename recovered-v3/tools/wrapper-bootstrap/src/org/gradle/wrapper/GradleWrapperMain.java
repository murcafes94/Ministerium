package org.gradle.wrapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Arranque mínimo de Gradle para este proyecto. Descarga la distribución
 * indicada en gradle-wrapper.properties y ejecuta Gradle. Si se proporciona
 * distributionSha256Sum también comprueba su SHA-256. El código fuente se
 * incluye para auditoría.
 */
public final class GradleWrapperMain {
    private static final int MAX_REDIRECTS = 8;

    private GradleWrapperMain() {}

    public static void main(String[] args) {
        try {
            Path project = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Properties properties = loadProperties(project);
            URI distributionUri = URI.create(required(properties, "distributionUrl"));
            String checksum = properties.getProperty("distributionSha256Sum", "")
                    .trim().toLowerCase();
            Path gradleHome = installDistribution(distributionUri, checksum);
            int result = runGradle(gradleHome, project, args);
            System.exit(result);
        } catch (Exception error) {
            System.err.println("No se pudo preparar Gradle: " + error.getMessage());
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static Properties loadProperties(Path project) throws IOException {
        Path file = project.resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }

    private static String required(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta la propiedad " + name);
        }
        return value.trim();
    }

    private static Path installDistribution(URI distributionUri, String checksum) throws Exception {
        String zipName = Path.of(distributionUri.getPath()).getFileName().toString();
        String distributionName = zipName.substring(0, zipName.length() - ".zip".length());
        String folderName = distributionName.endsWith("-bin")
                ? distributionName.substring(0, distributionName.length() - 4)
                : distributionName.replace("-all", "");

        String configuredHome = System.getenv("GRADLE_USER_HOME");
        Path userHome = configuredHome == null || configuredHome.isBlank()
                ? Path.of(System.getProperty("user.home"), ".gradle")
                : Path.of(configuredHome);
        Path base = userHome.resolve("wrapper/dists").resolve(distributionName);
        Path install = base.resolve("ministerium");
        Path executable = install.resolve(folderName).resolve("bin")
                .resolve(isWindows() ? "gradle.bat" : "gradle");

        Files.createDirectories(base);
        Path lockPath = base.resolve("ministerium.lock");
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            if (Files.isRegularFile(executable)) return install.resolve(folderName);

            deleteRecursively(install);
            Files.createDirectories(install);
            Path temporaryZip = base.resolve("ministerium-download.zip.part");
            Files.deleteIfExists(temporaryZip);
            download(distributionUri.toURL(), temporaryZip);

            if (!checksum.isEmpty()) {
                String actual = sha256(temporaryZip);
                if (!actual.equalsIgnoreCase(checksum)) {
                    Files.deleteIfExists(temporaryZip);
                    throw new IOException(
                            "La descarga de Gradle no superó la comprobación SHA-256");
                }
            }

            unzip(temporaryZip, install);
            Files.deleteIfExists(temporaryZip);
            if (!Files.isRegularFile(executable)) {
                throw new IOException("La distribución de Gradle quedó incompleta");
            }
            executable.toFile().setExecutable(true);
        }
        return install.resolve(folderName);
    }

    private static void download(URL initial, Path destination) throws IOException {
        URL current = initial;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            HttpURLConnection connection = (HttpURLConnection) current.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(60_000);
            connection.setReadTimeout(300_000);
            connection.setRequestProperty("User-Agent", "Ministerium-Gradle-Bootstrap/1.0");
            int status = connection.getResponseCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null) throw new IOException("Redirección sin destino");
                current = new URL(current, location);
                continue;
            }
            if (status < 200 || status >= 300) {
                connection.disconnect();
                throw new IOException("El servidor respondió HTTP " + status);
            }
            long total = connection.getContentLengthLong();
            System.out.println("Descargando Gradle por primera vez"
                    + (total > 0 ? " (" + (total / 1024 / 1024) + " MB)…" : "…"));
            try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                connection.disconnect();
            }
            return;
        }
        throw new IOException("Demasiadas redirecciones al descargar Gradle");
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder value = new StringBuilder();
        for (byte item : digest.digest()) value.append(String.format("%02x", item));
        return value.toString();
    }

    private static void unzip(Path zip, Path destination) throws IOException {
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) {
                    throw new IOException("Entrada insegura dentro del ZIP de Gradle");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
                input.closeEntry();
            }
        }
    }

    private static int runGradle(Path gradleHome, Path project, String[] args) throws Exception {
        Path executable = gradleHome.resolve("bin").resolve(isWindows() ? "gradle.bat" : "gradle");
        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.add("cmd.exe");
            command.add("/d");
            command.add("/c");
        } else {
            command.add("sh");
        }
        command.add(executable.toString());
        for (String argument : args) command.add(argument);
        Process process = new ProcessBuilder(command)
                .directory(project.toFile())
                .inheritIO()
                .start();
        return process.waitFor();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException error) {
                    throw new RuntimeException(error);
                }
            });
        } catch (RuntimeException error) {
            if (error.getCause() instanceof IOException) throw (IOException) error.getCause();
            throw error;
        }
    }
}
