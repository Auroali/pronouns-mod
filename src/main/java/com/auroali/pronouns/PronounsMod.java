package com.auroali.pronouns;

import com.auroali.pronouns.network.ClientPronounsLoadRequestC2S;
import com.auroali.pronouns.network.ClientPronounsLoadRequestS2C;
import com.auroali.pronouns.network.UpdatePronounsS2C;
import com.auroali.pronouns.storage.PronounsCache;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
            ServerPlayerEntity playerEntity = playNetworkHandler.getPlayer();
            cache.loadAsync(playerEntity.getUuid(), optional ->
              optional.ifPresent(pronouns -> {
                  cache.set(playerEntity.getUuid(), pronouns);
                  // sync the loaded pronouns to the client
                  // this has to be done after setting pronouns, as sometimes clients can
                  // load a player before pronouns have finished loading
                  UpdatePronounsS2C packet = new UpdatePronounsS2C(playerEntity.getUuid(), optional);
                  PlayerLookup.tracking(playerEntity).forEach(p -> ServerPlayNetworking.send(p, packet));
                  ServerPlayNetworking.send(playerEntity, packet);
              })
            );
        });

        ServerPlayNetworking.registerGlobalReceiver(ClientPronounsLoadRequestC2S.ID, (packet, player, responseSender) -> {
            MinecraftServer server = player.getServer();
            PronounsCache cache = PronounsCache.getCache(server);
            Optional<String> pronouns = cache.get(packet.requestedPronouns());
            responseSender.sendPacket(new ClientPronounsLoadRequestS2C(packet.requestedPronouns(), pronouns));
        });


        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) ->
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

                    UpdatePronounsS2C packet = new UpdatePronounsS2C(entity.getUuid(), Optional.of(pronouns));
                    PlayerLookup.tracking(entity).forEach(p ->
                      ServerPlayNetworking.send(p, packet)
                    );
                    ServerPlayNetworking.send(entity, packet);

                    ctx.getSource().sendFeedback(() -> Text.translatable("pronouns.set", Text.literal(pronouns).formatted(Formatting.GREEN)), false);
                    return 0;
                })))
            .then(CommandManager.literal("clear")
              .executes(ctx -> {
                  if (!ctx.getSource().isExecutedByPlayer())
                      return 1;

                  ServerPlayerEntity entity = ctx.getSource().getPlayer();
                  PronounsCache cache = PronounsCache.getCache(ctx.getSource().getServer());
                  cache.set(entity.getUuid(), null);

                  UpdatePronounsS2C packet = new UpdatePronounsS2C(entity.getUuid(), Optional.empty());
                  PlayerLookup.tracking(entity).forEach(p ->
                    ServerPlayNetworking.send(p, packet)
                  );
                  ServerPlayNetworking.send(entity, packet);
                  ctx.getSource().sendFeedback(() -> Text.translatable("pronouns.clear").formatted(Formatting.GREEN), false);
                  return 0;
              })
            )
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
          )
        );
    }
}
