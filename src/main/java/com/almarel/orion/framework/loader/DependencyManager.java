package com.almarel.orion.framework.loader;

import com.google.inject.Inject;
import com.almarel.orion.Orion;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.logging.LogLevel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Отвечает за динамическую загрузку библиотек (SQLite, HikariCP, Guice) при старте сервера.
 * Это позволяет не вшивать их в JAR, уменьшая размер плагина.
 *
 * ВНИМАНИЕ: Версии HikariCP, sqlite-jdbc, guice, jakarta.inject-api здесь
 * должны СОВПАДАТЬ с версиями в pom.xml (provided scope).
 * При обновлении версии в pom.xml — обнови и здесь, иначе compile-time ≠ runtime.
 * aopalliance 1.0 — единственная версия (spec заморожен с 2003 года), не нуждается в синхронизации.
 */
public class DependencyManager {

    private final Orion plugin;

    @Inject
    public DependencyManager(Orion plugin) {
        this.plugin = plugin;
    }

    public void load() {

        // Инициализируем менеджер библиотек Libby (AlessioDP fork)
        BukkitLibraryManager libraryManager = new BukkitLibraryManager(plugin);

        // Подавляем штатный INFO-спам Libby ("Downloading library...", "Downloaded library...", "Loaded library...")
        // Уровень WARN оставит видимыми реальные ошибки сети и сбои загрузки
        libraryManager.setLogLevel(LogLevel.WARN);

        // Добавляем Maven Central
        libraryManager.addMavenCentral();

        // 1. SQLite JDBC
        Library sqlite = Library.builder()
                .groupId("org.xerial")
                .artifactId("sqlite-jdbc")
                .version("3.53.4.0")
                .build();

        // 2. HikariCP
        Library hikari = Library.builder()
                .groupId("com.zaxxer")
                .artifactId("HikariCP")
                .version("7.1.0")
                .build();

        // 3. Google Guice (Ядро)
        Library guice = Library.builder()
                .groupId("com.google.inject")
                .artifactId("guice")
                .version("7.0.0")
                .build();

        // 3.1 Транзитивная зависимость Guice: Jakarta Inject API (Аннотации @Inject, Provider)
        Library jakartaInject = Library.builder()
                .groupId("jakarta.inject")
                .artifactId("jakarta.inject-api")
                .version("2.0.1.MR")
                .build();

        // 3.2 Транзитивная зависимость Guice: AOP Alliance (Интерцепторы)
        Library aopAlliance = Library.builder()
                .groupId("aopalliance")
                .artifactId("aopalliance")
                .version("1.0")
                .build();

        // Загружаем библиотеки
        try {
            // Обход бага Libby 1.3.2: жестко заданный таймаут 5 секунд на чтение потока.
            // На сервере 1.21.8 скачивание SQLite-драйвера занимает больше 5 секунд,
            // из-за чего Libby обрывает соединение с ошибкой "Download timed out".
            // Мы предварительно скачиваем файлы своим HTTP-клиентом с адекватным таймаутом (60с).
            // Libby увидит файлы в папке lib и пропустит стадию скачивания, сразу загрузив их в classpath.
            preDownloadLibrary(sqlite);
            preDownloadLibrary(hikari);
            preDownloadLibrary(jakartaInject);
            preDownloadLibrary(aopAlliance);
            preDownloadLibrary(guice);

            libraryManager.loadLibrary(sqlite);
            libraryManager.loadLibrary(hikari);
            // Сначала грузим зависимости Guice, потом сам Guice
            libraryManager.loadLibrary(jakartaInject);
            libraryManager.loadLibrary(aopAlliance);
            libraryManager.loadLibrary(guice);

        } catch (Exception e) {
            plugin.getLogger().severe("CRITICAL ERROR: Failed to load dependencies!");
            plugin.getLogger().severe("Error: " + e.getMessage());
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Предварительное скачивание JAR-файла в папку lib.
     * Использует увеличенные таймауты (30s connect, 60s read) для стабильной загрузки
     * крупных артефактов, таких как sqlite-jdbc.
     *
     * @param library Описание библиотеки
     * @throws IOException При ошибке сети или записи на диск
     */
    private void preDownloadLibrary(Library library) throws IOException {
        Path libDirectory = plugin.getDataFolder().toPath().resolve("lib");
        Path targetFile = libDirectory.resolve(library.getPath());

        // Если файл уже существует (и это не снапшот), пропускаем скачивание
        if (Files.exists(targetFile) && !library.isSnapshot()) {
            return;
        }

        Files.createDirectories(targetFile.getParent());
        Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
        Files.deleteIfExists(tempFile);

        String mavenUrl = "https://repo1.maven.org/maven2/" + library.getPath();

        URLConnection connection = new URL(mavenUrl).openConnection();
        connection.setConnectTimeout(30000); // 30 секунд на подключение
        connection.setReadTimeout(60000);    // 60 секунд на чтение данных
        connection.setRequestProperty("User-Agent", "Orion-DependencyManager");

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Оставляем только лог ошибок, как и просил
            plugin.getLogger().severe("Failed to download " + library.getArtifactId() + ": " + e.getMessage());
            Files.deleteIfExists(tempFile);
            throw e;
        }

        Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }
}