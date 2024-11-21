package com.auroali.pronouns.mixin.client;

import com.auroali.pronouns.storage.ClientPronounsCache;
import com.auroali.pronouns.storage.PronounsCache;
import com.auroali.pronouns.storage.PronounsCacheGetter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements PronounsCacheGetter {
    @Unique
    ClientPronounsCache pronouns$cache;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void pronouns$initClientCache(RunArgs args, CallbackInfo ci) {
        this.pronouns$cache = new ClientPronounsCache();
    }


    @Override
    public PronounsCache pronouns$get() {
        return this.pronouns$cache;
    }
}
