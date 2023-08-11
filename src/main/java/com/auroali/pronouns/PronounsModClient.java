package com.auroali.pronouns;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.entity_events.api.client.ClientEntityLoadEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayConnectionEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.util.HashMap;
import java.util.UUID;

public class PronounsModClient implements ClientModInitializer {
	public static final Object2ObjectOpenHashMap<UUID, String> PRONOUNS_MAP = new Object2ObjectOpenHashMap<>();
	@Override
	public void onInitializeClient(ModContainer mod) {
		ClientPlayConnectionEvents.INIT.register((handler, client) -> {
			PRONOUNS_MAP.clear();
		});

		ClientPlayNetworking.registerGlobalReceiver(new Identifier(PronounsMod.MODID, "channel"), (client, handler, buf, responseSender) -> {
			while(buf.readableBytes() > 0) {
				PRONOUNS_MAP.put(buf.readUuid(), buf.readString());
			}
		});

		ClientEntityLoadEvents.AFTER_UNLOAD.register(((entity, world) -> {
			if(entity instanceof PlayerEntity) {
				PRONOUNS_MAP.remove(entity.getUuid());
			}
		}));
	}
}
