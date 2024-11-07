package com.auroali.pronouns;

import com.auroali.pronouns.network.SendPronounsS2C;
import com.auroali.pronouns.network.UpdatePronounsS2C;
import com.auroali.pronouns.storage.ClientPronounsCache;
import com.auroali.pronouns.storage.PronounsCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Optional;

public class PronounsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientEntityEvents.ENTITY_LOAD.register(((entity, world) -> {
            if (entity instanceof PlayerEntity) {
                MinecraftClient client = MinecraftClient.getInstance();
                PronounsCache cache = PronounsCache.getCache(client);
                // load pronouns for this entity
                cache.loadAsync(entity.getUuid(), optional ->
                  optional.ifPresentOrElse(
                    pronouns -> cache.set(entity.getUuid(), pronouns),
                    () -> cache.set(entity.getUuid(), null)
                  )
                );
            }
        }));

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PronounsCache cache = PronounsCache.getCache(client);
            if(entity instanceof PlayerEntity) {
                cache.set(entity.getUuid(), null);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PronounsCache cache = PronounsCache.getCache(client);
            if(cache instanceof ClientPronounsCache clientCache) {
                clientCache.clearPendingConsumers();
                clientCache.clearCachedValues();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(SendPronounsS2C.ID, (packet, player, responseSender) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PronounsCache cache = PronounsCache.getCache(client);
            if (cache instanceof ClientPronounsCache clientCache) {
                Optional<String> pronounsOptional = packet.pronouns() != null ? Optional.of(packet.pronouns()) : Optional.empty();
                clientCache.processPendingConsumers(packet.player(), pronounsOptional);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(UpdatePronounsS2C.ID, (packet, player, responseSender) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PronounsCache cache = PronounsCache.getCache(client);
            cache.set(packet.player(), packet.pronouns());
        });
    }
}
