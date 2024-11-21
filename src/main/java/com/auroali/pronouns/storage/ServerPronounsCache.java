package com.auroali.pronouns.storage;

import com.auroali.pronouns.storage.legacy.LegacyPlayerPronouns;
import net.minecraft.entity.player.PlayerEntity;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ServerPronounsCache implements PronounsCache {
    protected static final Logger LOGGER = LoggerFactory.getLogger("Pronouns Cache | Server");
    public static final int MAX_PRONOUNS_LENGTH = 64;
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
        // todo: make the server cache drop pronouns when players log out
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
            int dataVersion = stream.readInt();
            if (dataVersion != PronounsCache.DATAVERSION) {
                LOGGER.warn("Outdated pronouns file {}", file.getName());
            }
            return switch (dataVersion) {
                // dataversion 1
                case 1 -> {
                    String pronounsString = validatePronounsString(stream.readUTF());
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
    void dropUnusedEntries() {
        // if theres no entries, theres nothing to drop
        if (pronouns.isEmpty())
            return;

        Set<UUID> playerUuids = this.server.getPlayerManager()
          .getPlayerList()
          .stream()
          .map(PlayerEntity::getUuid)
          .collect(Collectors.toSet());

        synchronized (this.pronouns) {
            // remove any entries that lack a corresponding player
            this.pronouns.keySet().removeIf(Predicate.not(playerUuids::contains));
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
        dropUnusedEntries();
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
        pronouns = validatePronounsString(pronouns);
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

    protected String validatePronounsString(String pronouns) {
        if (pronouns.length() > MAX_PRONOUNS_LENGTH) {
            LOGGER.warn("Pronoun string {} over max character limit of {} characters! This will be trimmed.", pronouns, MAX_PRONOUNS_LENGTH);
            return pronouns.substring(0, MAX_PRONOUNS_LENGTH);
        }
        return pronouns;
    }
}
