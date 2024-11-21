package com.auroali.pronouns.storage.legacy;

import com.auroali.pronouns.PronounsMod;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LegacyPlayerPronouns {
    private static final int HEADER_VALIDATION_MARKER = 0x50524e53;
    protected final File file;
    public final Object2ObjectOpenHashMap<UUID, String> pronounsMap;

    public LegacyPlayerPronouns(File file) {
        pronounsMap = new Object2ObjectOpenHashMap<>();
        this.file = file;
        load();
    }

    public void load() {
        if (!file.exists())
            return;

        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
            int header = stream.readInt();
            if (header != HEADER_VALIDATION_MARKER) {
                throw new IOException("Invalid file header start. Expected: %s, got: %s".formatted(Integer.toHexString(HEADER_VALIDATION_MARKER), Integer.toHexString(header)));
            }
            int numEntries = stream.readInt();
            for (int i = 0; i < numEntries; i++) {
                long leastSig = stream.readLong();
                long mostSig = stream.readLong();
                UUID playerUUID = new UUID(mostSig, leastSig);
                int strLen = stream.readInt();
                byte[] str = new byte[strLen];
                int len = stream.read(str, 0, strLen);
                if (len != strLen) {
                    throw new IOException("String length mismatch! Expected %d, got %d".formatted(len, strLen));
                }
                pronounsMap.put(playerUUID, new String(str, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            PronounsMod.LOGGER.error("An error occurred whilst loading the pronouns file!");
            PronounsMod.LOGGER.error(String.valueOf(e));
        }
    }
}
