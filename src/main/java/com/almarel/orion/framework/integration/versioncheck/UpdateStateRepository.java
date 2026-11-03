// TYPE B: FULL FILE
package com.almarel.orion.framework.integration.versioncheck;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.integration.versioncheck.data.UpdateCheckState;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Owns the single canonical in-memory UpdateCheckState instance and its
 * persistence to disk. All access is serialized through one ReentrantLock,
 * matching the ioLock pattern already used by PresetFileIO in this codebase.
 */
@Singleton
public class UpdateStateRepository {

    private static final String STATE_FILE_PATH = "globals/versioncheck/update-check-state.json";
    private static final String TEMP_FILE_EXTENSION = ".tmp";

    private final JavaPlugin plugin;
    private final Gson gson;
    private final File stateFile;
    private final ReentrantLock lock = new ReentrantLock();

    private UpdateCheckState state = new UpdateCheckState();

    @Inject
    public UpdateStateRepository(JavaPlugin plugin, Gson gson) {
        this.plugin = plugin;
        this.gson = gson;
        this.stateFile = new File(plugin.getDataFolder(), STATE_FILE_PATH);
    }

    /**
     * Reads the persisted state from disk into memory.
     * Must be called from an async thread — performs blocking file I/O.
     */
    public void loadFromDisk() {
        lock.lock();
        try {
            if (!stateFile.exists()) {
                return;
            }

            UpdateCheckState loaded = readStateFromFile();
            if (loaded != null) {
                normalizeState(loaded);
                this.state = loaded;
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[1b1v] Failed to load update-check-state.json: " + ex.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private UpdateCheckState readStateFromFile() throws Exception {
        try (Reader reader = new FileReader(stateFile, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, UpdateCheckState.class);
        }
    }

    private void normalizeState(UpdateCheckState loaded) {
        if (loaded.getLastNotifiedPerKey() == null) {
            loaded.setLastNotifiedPerKey(new ConcurrentHashMap<>());
        }
    }

    /**
     * Returns the current in-memory state. Safe to call from the main thread —
     * this only acquires an uncontended in-memory lock, no disk access.
     */
    public UpdateCheckState snapshot() {
        lock.lock();
        try {
            return state;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies a mutation to the shared state and persists the result to disk.
     * Performs blocking file I/O — callers must invoke this from an async thread.
     */
    public void update(Consumer<UpdateCheckState> mutator) {
        lock.lock();
        try {
            mutator.accept(state);
            persistToDisk(state);
        } finally {
            lock.unlock();
        }
    }

    private void persistToDisk(UpdateCheckState toSave) {
        try {
            ensureParentDirectory(stateFile);
            String json = gson.toJson(toSave);
            writeAtomic(stateFile, json);
        } catch (Exception ex) {
            plugin.getLogger().severe("[1b1v] Failed to save update-check-state.json: " + ex.getMessage());
        }
    }

    private void ensureParentDirectory(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    private void writeAtomic(File targetFile, String content) throws Exception {
        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + TEMP_FILE_EXTENSION);
        Files.writeString(tempFile.toPath(), content, StandardCharsets.UTF_8);
        Files.move(
                tempFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );
    }
}