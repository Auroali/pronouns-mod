package com.auroali.pronouns.client;

import com.auroali.pronouns.PlayerPronouns;

import java.io.File;
import java.util.UUID;

public class ClientPlayerPronouns extends PlayerPronouns {
	public ClientPlayerPronouns() {
		super(null);
	}

	@Override
	public void load() {
	}

	@Override
	public void save() {
	}

	@Override
	public void setDirty() {
	}

	public void removeEntry(UUID uuid) {
		this.pronounsMap.remove(uuid);
	}

	@Override
	public boolean isDirty() {
		return super.isDirty();
	}
}
