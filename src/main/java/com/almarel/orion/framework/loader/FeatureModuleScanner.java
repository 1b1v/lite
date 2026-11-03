package com.almarel.orion.framework.loader;

import com.google.inject.Module;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис сканирования JAR-архива плагина для динамического обнаружения Guice-модулей.
 * Не зависит от конкретных имен классов и поддерживает поиск после обфускации.
 */
public final class FeatureModuleScanner {

    private FeatureModuleScanner() {}

    /**
     * Сканирует JAR-файл плагина на наличие классов, реализующих {@link Module}.
     *
     * @param plugin        Экземпляр плагина.
     * @param logger        Логгер для некритичных диагностических сообщений.
     * @param targetPackage Базовый пакет для поиска (например, "com.almarel.orion.brush.custom").
     * @return Список созданных экземпляров модулей.
     */
    public static List<Module> scan(Plugin plugin, Logger logger, String targetPackage) {
        Optional<File> jarFileOpt = resolvePluginJar(plugin, logger);
        if (jarFileOpt.isEmpty()) {
            return List.of();
        }

        String packagePath = targetPackage.replace('.', '/') + "/";
        ClassLoader classLoader = plugin.getClass().getClassLoader();

        return scanJarForModules(jarFileOpt.get(), packagePath, classLoader, logger);
    }

    private static Optional<File> resolvePluginJar(Plugin plugin, Logger logger) {
        try {
            URL location = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (!"file".equalsIgnoreCase(location.getProtocol())) {
                logger.warning("[Scanner] Plugin code source is not a file protocol. Scanning aborted.");
                return Optional.empty();
            }

            File jarFile = new File(location.toURI());
            if (!jarFile.exists() || !jarFile.isFile()) {
                logger.warning("[Scanner] Cannot locate plugin JAR file at: " + jarFile.getAbsolutePath());
                return Optional.empty();
            }

            return Optional.of(jarFile);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Scanner] Failed to resolve plugin JAR file location", e);
            return Optional.empty();
        }
    }

    private static List<Module> scanJarForModules(File jarFile, String packagePath, ClassLoader loader, Logger logger) {
        List<Module> modules = new ArrayList<>();

        try (JarFile file = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = file.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!isCandidateClass(entry, packagePath)) {
                    continue;
                }

                String fqcn = resolveFqcn(entry.getName());
                tryInstantiateModule(fqcn, loader, logger).ifPresent(modules::add);
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "[Scanner] Error reading plugin JAR file: " + jarFile.getName(), t);
        }

        return modules;
    }

    private static boolean isCandidateClass(JarEntry entry, String packagePath) {
        if (entry.isDirectory()) {
            return false;
        }
        String name = entry.getName();
        return name.startsWith(packagePath) && name.endsWith(".class");
    }

    private static String resolveFqcn(String entryPath) {
        return entryPath.substring(0, entryPath.length() - 6).replace('/', '.');
    }

    private static Optional<Module> tryInstantiateModule(String fqcn, ClassLoader loader, Logger logger) {
        try {
            Class<?> clazz = Class.forName(fqcn, false, loader);
            if (!isInstantiableModule(clazz)) {
                return Optional.empty();
            }

            Module module = clazz.asSubclass(Module.class)
                    .getDeclaredConstructor()
                    .newInstance();

            return Optional.of(module);
        } catch (ClassNotFoundException | NoClassDefFoundError | NoSuchMethodException ignored) {
            // Класс не является публичным модулем или не имеет конструктора без параметров
            return Optional.empty();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "[Scanner] Failed to initialize module instance for: " + fqcn, t);
            return Optional.empty();
        }
    }

    private static boolean isInstantiableModule(Class<?> clazz) {
        if (!Module.class.isAssignableFrom(clazz)) {
            return false;
        }
        int modifiers = clazz.getModifiers();
        return !Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers);
    }
}