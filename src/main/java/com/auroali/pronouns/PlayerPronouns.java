package com.auroali.pronouns;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PlayerPronouns {
	private static final int HEADER_VALIDATION_MARKER = 0x50524e53;
	protected final File file;
	protected final Object2ObjectOpenHashMap<UUID, String> pronounsMap;
	protected boolean isDirty;

	public PlayerPronouns(File file) {
		pronounsMap = new Object2ObjectOpenHashMap<>();
		this.file = file;
		load();

	}

	public void load() {
		if (!file.exists()) {
			save();
			return;
		}
		try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
			int header = stream.readInt();
			if(header != HEADER_VALIDATION_MARKER) {
				throw new IOException("Invalid file header start. Expected: %s, got: %s".formatted(Integer.toHexString(HEADER_VALIDATION_MARKER), Integer.toHexString(header)));
			}
			int numEntries = stream.readInt();
			for(int i = 0; i < numEntries; i++) {
				long leastSig = stream.readLong();
				long mostSig = stream.readLong();
				UUID playerUUID = new UUID(mostSig, leastSig);
				int strLen = stream.readInt();
				byte[] str = new byte[strLen];
				int len = stream.read(str, 0, strLen);
				if(len != strLen) {
					throw new IOException("String length mismatch! Expected %d, got %d".formatted(len, strLen));
				}
				pronounsMap.put(playerUUID, new String(str, StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			PronounsMod.LOGGER.error("An error occurred whilst loading the pronouns file!");
			PronounsMod.LOGGER.error(String.valueOf(e));
			PronounsMod.LOGGER.warn("Old pronouns file will be overwritten.");
			save();
		}
	}

	public void save() {
		try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))))) {
			stream.writeInt(HEADER_VALIDATION_MARKER);
			stream.writeInt(pronounsMap.size());
			pronounsMap.object2ObjectEntrySet().fastForEach(e -> {
				long least = e.getKey().getLeastSignificantBits();
				long most = e.getKey().getMostSignificantBits();
				try {
					stream.writeLong(least);
					stream.writeLong(most);
					byte[] str = e.getValue().getBytes(StandardCharsets.UTF_8);
					stream.writeInt(str.length);
					stream.write(str);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			});
		} catch (Exception e) {
			PronounsMod.LOGGER.error("An error occurred whilst saving the pronouns file!");
			PronounsMod.LOGGER.error(String.valueOf(e));
		} finally {
			setDirty(false);
		}
	}

	public void setDirty() {
		setDirty(true);
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean dirty) {
		this.isDirty = dirty;
	}

	public String getPronouns(UUID uuid) {
		return pronounsMap.get(uuid);
	}

	public synchronized void setPronouns(UUID uuid, String pronouns) {
		pronounsMap.put(uuid, pronouns);
		setDirty();
	}
}
