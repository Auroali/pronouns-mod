package com.auroali.pronouns;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;
import org.quiltmc.qsl.lifecycle.api.event.ServerWorldLoadEvents;
import org.quiltmc.qsl.networking.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;

public class PronounsMod implements ModInitializer {
	public static final String MODID = "pronouns";

	public static PlayerPronouns pronouns;
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID.substring(0, 1).toUpperCase() + MODID.substring(1));

	@Override
	public void onInitialize(ModContainer mod) {
		ServerLifecycleEvents.STARTING.register(server -> {
			File pronounsFile = server.getSavePath(WorldSavePath.ROOT).resolve("pronouns.dat").toFile();
			pronouns = new PlayerPronouns(pronounsFile);
		});

		ServerPlayConnectionEvents.JOIN.register((playNetworkHandler, packetSender, server) -> {
			if(pronouns.getPronouns(playNetworkHandler.player.getUuid()) != null)
				sendPronouns(playNetworkHandler.player, playNetworkHandler.player.getUuid());
		});

		EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
			if(trackedEntity instanceof PlayerEntity entity && pronouns.getPronouns(entity.getUuid()) != null) {
				sendPronouns(player, entity.getUuid());
			}
		});



		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> {
			dispatcher.register(CommandManager.literal("pronouns")
				.then(CommandManager.literal("set")
					.then(CommandManager.argument("pronouns", StringArgumentType.string())
					.executes(ctx -> {
						String pronouns = StringArgumentType.getString(ctx, "pronouns");
						ServerPlayerEntity entity = ctx.getSource().getPlayer();
						PronounsMod.pronouns.setPronouns(entity.getUuid(), pronouns);
						sendPronouns(entity, entity.getUuid());
						sendPronounsNear(entity, entity.getUuid());
						ctx.getSource().sendFeedback(Text.translatable("pronouns.set", Text.literal(pronouns).setStyle(Style.EMPTY.withColor(Formatting.GREEN))), true);
						return 0;
					})))
				.then(CommandManager.literal("get")
					.then(CommandManager.argument("player", EntityArgumentType.player())
						.executes(ctx -> {
							PlayerEntity entity = EntityArgumentType.getPlayer(ctx, "player");
							if(entity == null)
								return -1;
							String pronouns = PronounsMod.pronouns.getPronouns(entity.getUuid());
							if(pronouns != null)
								ctx.getSource().sendFeedback(Text.translatable("pronouns.query", entity.getName(), pronouns), false);
							else {
								ctx.getSource().sendFeedback(Text.translatable("pronouns.query.none", entity.getName()), false);
							}
							return 0;
						}))

				)
			);
		});

		ServerLifecycleEvents.STOPPED.register(server -> pronouns = null);

	}

	public void sendPronouns(ServerPlayerEntity entity, UUID uuid) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeUuid(uuid);
		buf.writeString(pronouns.getPronouns(uuid));
		ServerPlayNetworking.send(entity, new Identifier(MODID, "channel"), buf);
	}

	public void sendPronounsNear(ServerPlayerEntity entity, UUID uuid) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeUuid(uuid);
		buf.writeString(pronouns.getPronouns(uuid));
		PlayerLookup.tracking(entity).forEach(e ->  ServerPlayNetworking.send(e, new Identifier(MODID, "channel"), buf));
	}
}
