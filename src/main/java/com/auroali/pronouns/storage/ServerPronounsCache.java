package com.auroali.pronouns.storage;

import com.auroali.pronouns.storage.legacy.LegacyPlayerPronouns;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class ServerPronounsCache implements PronounsCache {
    protected static Logger LOGGER = LoggerFactory.getLogger("Pronouns Cache | Server");
    private final Map<UUID, String> pronouns = new HashMap<>();
    private final Set<UUID> removed = new HashSet<>();
    private final Map<UUID, CompletableFuture<Optional<String>>> pending = new HashMap<>();
    private final MinecraftServer server;
    private final Executor executor;
    private final File pronounsDir;
    public long lastModified;
    public long lastSaved;

    public ServerPronounsCache(MinecraftServer server, Executor executor, File pronounsDir) {
        this.server = server;
        this.executor = executor;
        this.pronounsDir = pronounsDir;
        updateOldPronouns(server);
    }

    @Override
    public Optional<String> get(UUID uuid) {
        return pronouns.containsKey(uuid) ? Optional.of(pronouns.get(uuid)) : Optional.empty();
    }

    @Override
    public void loadAsync(UUID uuid, Consumer<Optional<String>> consumer) {
        if (this.pending.containsKey(uuid)) {
            this.pending.put(
              uuid,
              this.pending
                .get(uuid)
                .whenCompleteAsync((pronouns, throwable) -> consumer.accept(pronouns), executor)
            );
            return;
        }
        CompletableFuture<Optional<String>> future = CompletableFuture.supplyAsync(() -> load(uuid), Util.getMainWorkerExecutor())
          .whenCompleteAsync((pronouns, throwable) -> this.pending.remove(uuid), executor)
          .whenCompleteAsync((pronouns, throwable) -> consumer.accept(pronouns));
        this.pending.put(uuid, future);
    }

    Optional<String> load(UUID uuid) {
        java.io.File file = getPronounsFile(uuid);
        return pronounsDir.isDirectory() && file.exists() ? readPronounsFile(file) : Optional.empty();
    }

    Optional<String> readPronounsFile(File file) {
        try (DataInputStream stream = new DataInputStream(new FileInputStream(file))) {
            stream.readInt();
            return Optional.of(stream.readUTF());
        } catch (IOException e) {
            LOGGER.error("Failed to read pronouns file!", e);
            return Optional.empty();
        }
    }

    void writePronounsFile(File file, String pronouns) {
        try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(file))) {
            // store the data version, in case there's any format changes down the line
            stream.writeInt(PronounsCache.DATAVERSION);
            stream.writeUTF(pronouns);
        } catch (IOException e) {
            LOGGER.error("Failed to write pronouns file!", e);
        }
    }

    public void save() {
        for (UUID uuid : this.removed) {
            File file = getPronounsFile(uuid);
            if (file.exists() && file.delete())
                LOGGER.info("Successfully removed pronouns file for {}", uuid);
        }
        this.removed.clear();
        this.pronouns.forEach((uuid, pronouns) -> {
            File file = getPronounsFile(uuid);
            writePronounsFile(file, pronouns);
        });
        this.lastSaved = System.currentTimeMillis();
    }

    public void updateOldPronouns(MinecraftServer server) {
        File file = server.getSavePath(WorldSavePath.ROOT).resolve("pronouns.dat").toFile();
        if (!file.exists())
            return;
        LegacyPlayerPronouns oldPronouns = new LegacyPlayerPronouns(file);
        oldPronouns.pronounsMap.forEach(this::set);
        if (file.delete())
            LOGGER.info("Successfully updated old pronouns files");
        save();
    }

    File getPronounsFile(UUID uuid) {
        return new File(pronounsDir, uuid.toString() + ".pronouns");
    }

    @Override
    public void set(UUID uuid, String pronouns) {
        synchronized (this.pronouns) {
            lastModified = System.currentTimeMillis();
            if (pronouns == null) {
                this.pronouns.remove(uuid);
                this.removed.add(uuid);
                return;
            }
            this.pronouns.put(uuid, pronouns);
            removed.remove(uuid);
        }
    }
}
