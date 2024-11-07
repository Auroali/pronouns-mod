package com.auroali.pronouns.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface PronounsCache {
    Optional<String> get(UUID uuid);

    void loadAsync(UUID uuid, Consumer<Optional<String>> consumer);

    void set(UUID uuid, String pronouns);

    static PronounsCache getCache(Object object) {
        if (object instanceof PronounsCacheGetter getter) {
            return getter.pronouns$get();
        }
        throw new IllegalArgumentException("Class %s does not implement PronounsCacheGetter!".formatted(object.getClass().getName()));
    }
}
