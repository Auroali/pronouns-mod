package com.auroali.pronouns.storage;

import com.auroali.pronouns.network.RequestPronounsC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.*;
import java.util.function.Consumer;

public class ClientPronounsCache implements PronounsCache {
    private final Map<UUID, String> pronouns = new HashMap<>();
    private final Map<UUID, List<Consumer<Optional<String>>>> pending = new HashMap<>();

    public ClientPronounsCache() {

    }

    @Override
    public Optional<String> get(UUID uuid) {
        return this.pronouns.containsKey(uuid) ? Optional.of(this.pronouns.get(uuid)) : Optional.empty();
    }

    @Override
    public void loadAsync(UUID uuid, Consumer<Optional<String>> consumer) {
        List<Consumer<Optional<String>>> consumers = pending.computeIfAbsent(uuid, key -> new ArrayList<>());
        consumers.add(consumer);
        if (consumers.size() == 1)
            ClientPlayNetworking.send(new RequestPronounsC2S(uuid));
    }

    @Override
    public void set(UUID uuid, String pronouns) {
        if (pronouns == null) {
            this.pronouns.remove(uuid);
            return;
        }
        this.pronouns.put(uuid, pronouns);
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void processPendingConsumers(UUID uuid, Optional<String> value) {
        if (pending.containsKey(uuid))
            pending.get(uuid).forEach(consumer -> consumer.accept(value));
        pending.remove(uuid);
    }

    public void clearPendingConsumers() {
        this.pronouns.clear();
    }

    public void clearCachedValues() {
        this.pronouns.clear();
    }
}
