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
    protected static final Logger LOGGER = LoggerFactory.getLogger("Pronouns Cache | Server");
    public static final int MAX_PRONOUNS_LENGTH = 64;
    private final Map<UUID, String> pronouns = new HashMap<>();
    private final Set<UUID> removed = new HashSet<>();
    private final Set<UUID> dropQueue = new HashSet<>();
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
        this.updateOldPronouns(server);
    }

    @Override
    public Optional<String> get(UUID uuid) {
        return this.pronouns.containsKey(uuid) ? Optional.of(this.pronouns.get(uuid)) : Optional.empty();
    }

    @Override
    public void loadAsync(UUID uuid, Consumer<Optional<String>> consumer) {
        if (this.pending.containsKey(uuid)) {
            this.pending.put(
              uuid,
              this.pending
                .get(uuid)
                .whenCompleteAsync((pronouns, throwable) -> consumer.accept(pronouns), this.executor)
            );
            return;
        }
        CompletableFuture<Optional<String>> future = CompletableFuture.supplyAsync(() -> this.load(uuid), Util.getMainWorkerExecutor())
          .whenCompleteAsync((pronouns, throwable) -> this.pending.remove(uuid), this.executor)
          .whenCompleteAsync((pronouns, throwable) -> consumer.accept(pronouns));
        this.pending.put(uuid, future);
    }

    protected Optional<String> load(UUID uuid) {
        File file = this.getPronounsFile(uuid);
        return this.pronounsDir.isDirectory() && file.exists() ? this.readPronounsFile(file) : Optional.empty();
    }

    protected Optional<String> readPronounsFile(File file) {
        try (DataInputStream stream = new DataInputStream(new FileInputStream(file))) {
            int dataVersion = stream.readInt();
            if (dataVersion != PronounsCache.DATAVERSION) {
                LOGGER.warn("Outdated pronouns file {}", file.getName());
            }
            return switch (dataVersion) {
                // dataversion 1
                case 1 -> {
                    String pronounsString = this.validatePronounsString(stream.readUTF());
                    yield Optional.of(pronounsString);
                }
                default -> {
                    LOGGER.error("Unknown dataversion {} in pronouns file {}", dataVersion, file.getName());
                    yield Optional.empty();
                }
            };
            // make sure the loaded string fits within the character limit
        } catch (IOException e) {
            LOGGER.error("Failed to read pronouns file!", e);
            return Optional.empty();
        }
    }

    /**
     * Drops any unused cache entries
     * <br> Should only be called from save, internal use only
     */
    private void dropUnusedEntries() {
        // if theres no entries, theres nothing to drop
        if (this.pronouns.isEmpty())
            return;

        synchronized (this.pronouns) {
            for (UUID uuid : this.dropQueue) {
                if (this.server.getPlayerManager().getPlayer(uuid) == null) {
                    this.pronouns.remove(uuid);
                }
            }

            this.dropQueue.clear();
        }
    }

    /**
     * Deletes any files corresponding to removed pronouns
     */
    private void removeUnsetFiles() {
        synchronized (this.pronouns) {
            for (UUID uuid : this.removed) {
                File file = this.getPronounsFile(uuid);
                if (file.exists() && file.delete())
                    LOGGER.info("Successfully removed pronouns file for {}", uuid);
            }
            this.removed.clear();
        }
    }

    /**
     * Writes the pronouns data to disk
     *
     * @param file     the file to write to
     * @param pronouns the pronouns to write
     */
    protected void writePronounsFile(File file, String pronouns) {
        try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(file))) {
            // store the data version, in case there's any format changes down the line
            stream.writeInt(PronounsCache.DATAVERSION);
            stream.writeUTF(pronouns);
            stream.writeUTF(pronouns);
        } catch (IOException e) {
            LOGGER.error("Failed to write pronouns file!", e);
        }
    }

    /**
     * Writes all pronouns to disk, deleting files for unset pronouns, and dropping any pronouns that are no longer
     * in use from the cache
     */
    public void save() {
        this.removeUnsetFiles();
        this.pronouns.forEach((uuid, pronouns) -> {
            File file = this.getPronounsFile(uuid);
            this.writePronounsFile(file, pronouns);
        });
        this.lastSaved = System.currentTimeMillis();
        this.dropUnusedEntries();
    }

    /**
     * Converts the previous pronouns format to the current one
     *
     * @param server the server instance
     */
    public void updateOldPronouns(MinecraftServer server) {
        File file = server.getSavePath(WorldSavePath.ROOT).resolve("pronouns.dat").toFile();
        if (!file.exists())
            return;
        LegacyPlayerPronouns oldPronouns = new LegacyPlayerPronouns(file);
        oldPronouns.pronounsMap.forEach(this::set);
        if (file.delete())
            LOGGER.info("Successfully updated old pronouns files");
        this.save();
    }

    File getPronounsFile(UUID uuid) {
        return new File(this.pronounsDir, uuid.toString() + ".pronouns");
    }

    @Override
    public void set(UUID uuid, String pronouns) {
        synchronized (this.pronouns) {
            this.lastModified = System.currentTimeMillis();
            if (pronouns == null) {
                this.pronouns.remove(uuid);
                this.removed.add(uuid);
                return;
            }
            pronouns = this.validatePronounsString(pronouns);
            this.pronouns.put(uuid, pronouns);
            this.removed.remove(uuid);
        }
    }

    protected String validatePronounsString(String pronouns) {
        if (pronouns.length() > MAX_PRONOUNS_LENGTH) {
            LOGGER.warn("Pronoun string {} over max character limit of {} characters! This will be trimmed.", pronouns, MAX_PRONOUNS_LENGTH);
            return pronouns.substring(0, MAX_PRONOUNS_LENGTH);
        }
        return pronouns;
    }

    public void markForRemoval(UUID uuid) {
        synchronized (this.pronouns) {
            this.dropQueue.add(uuid);
        }
    }
}
