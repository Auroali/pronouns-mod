package com.auroali.pronouns;

import com.auroali.pronouns.network.RequestPronounsC2S;
import com.auroali.pronouns.network.SendPronounsS2C;
import com.auroali.pronouns.network.UpdatePronounsS2C;
import com.auroali.pronouns.storage.PronounsCache;
import com.auroali.pronouns.storage.legacy.LegacyPlayerPronouns;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

public class PronounsMod implements ModInitializer {
    public static final String MODID = "pronouns";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID.substring(0, 1).toUpperCase() + MODID.substring(1));

    public static Identifier id(String value) {
        return new Identifier(MODID, value);
    }

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.INIT.register((playNetworkHandler, server) -> {
            PronounsCache cache = PronounsCache.getCache(server);
            cache.loadAsync(playNetworkHandler.getPlayer().getUuid(), optional ->
              optional.ifPresent(pronouns -> cache.set(playNetworkHandler.getPlayer().getUuid(), pronouns))
            );
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestPronounsC2S.ID, (packet, player, responseSender) -> {
            MinecraftServer server = player.getServer();
            PronounsCache cache = PronounsCache.getCache(server);
            cache.getAfterLoad(packet.requestedPronouns()).ifPresentOrElse(
              pronouns -> responseSender.sendPacket(new SendPronounsS2C(packet.requestedPronouns(), pronouns)),
              () -> responseSender.sendPacket(new SendPronounsS2C(packet.requestedPronouns(), null))
            );
        });


        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> {
            dispatcher.register(CommandManager.literal("pronouns")
              .then(CommandManager.literal("set")
                .then(CommandManager.argument("pronouns", StringArgumentType.greedyString())
                  .executes(ctx -> {
                      if (!ctx.getSource().isExecutedByPlayer())
                          return 1;

                      String pronouns = StringArgumentType.getString(ctx, "pronouns");
                      ServerPlayerEntity entity = ctx.getSource().getPlayer();

                      PronounsCache cache = PronounsCache.getCache(ctx.getSource().getServer());
                      cache.set(entity.getUuid(), pronouns);

                      UpdatePronounsS2C packet = new UpdatePronounsS2C(entity.getUuid(), pronouns);
                      PlayerLookup.tracking(entity).forEach(p -> {
                          ServerPlayNetworking.send(p, packet);
                      });
                      ServerPlayNetworking.send(entity, packet);

                      ctx.getSource().sendFeedback(() -> Text.translatable("pronouns.set", Text.literal(pronouns).setStyle(Style.EMPTY.withColor(Formatting.GREEN))), true);
                      return 0;
                  })))
              .then(CommandManager.literal("get")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                  .executes(ctx -> {
                      PlayerEntity entity = EntityArgumentType.getPlayer(ctx, "player");
                      if (entity == null)
                          return -1;

                      PronounsCache cache = PronounsCache.getCache(ctx.getSource().getServer());
                      Optional<String> pronounsOptional = cache.get(entity.getUuid());
                      pronounsOptional.ifPresentOrElse(
                        pronouns -> ctx.getSource().sendFeedback(() -> Text.translatable("pronouns.query", entity.getName(), pronouns), false),
                        () -> ctx.getSource().sendFeedback(() -> Text.translatable("pronouns.query.none", entity.getName()), false)
                      );
                      return 0;
                  }))

              )
            );
        });
    }
}
