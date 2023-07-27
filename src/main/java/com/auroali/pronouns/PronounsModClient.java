package com.auroali.pronouns;

import com.auroali.pronouns.client.ClientPlayerPronouns;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.entity_events.api.client.ClientEntityLoadEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayConnectionEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

public class PronounsModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(ModContainer mod) {
		ClientPlayConnectionEvents.INIT.register((handler, client) -> {
			if(PronounsMod.pronouns == null)
				PronounsMod.pronouns = new ClientPlayerPronouns();
		});

		ClientPlayNetworking.registerGlobalReceiver(new Identifier(PronounsMod.MODID, "channel"), (client, handler, buf, responseSender) -> {
			while(buf.readableBytes() > 0) {
				PronounsMod.pronouns.setPronouns(buf.readUuid(), buf.readString());
			}
		});

		ClientEntityLoadEvents.AFTER_UNLOAD.register(((entity, world) -> {
			if(entity instanceof PlayerEntity && PronounsMod.pronouns instanceof ClientPlayerPronouns pronouns) {
				pronouns.removeEntry(entity.getUuid());
			}
		}));
	}
}
