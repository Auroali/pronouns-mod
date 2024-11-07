package com.auroali.pronouns.mixin;

import com.auroali.pronouns.storage.PronounsCache;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(MessageType.class)
public class MessageTypeMixin {
    @WrapOperation(method = "params(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/entity/Entity;)Lnet/minecraft/network/message/MessageType$Parameters;", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/entity/Entity;getDisplayName()Lnet/minecraft/text/Text;"
    ))
    private static Text pronouns$injectIntoChatMessage(Entity instance, Operation<Text> original) {
        AtomicReference<Text> text = new AtomicReference<>(original.call(instance));
        if (text.get() instanceof MutableText mutableText && instance instanceof PlayerEntity) {
            MinecraftClient client = MinecraftClient.getInstance();
            PronounsCache.getCache(client).get(instance.getUuid())
              .ifPresent(pronouns -> text.set(mutableText.copy().append(Text.literal(" %s".formatted(pronouns)).formatted(Formatting.ITALIC))));
        }
        return text.get();
    }
}
