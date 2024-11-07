package com.auroali.pronouns.mixin;

import com.auroali.pronouns.PronounsMod;
import com.auroali.pronouns.storage.PronounsCache;
import com.auroali.pronouns.storage.PronounsCacheGetter;
import com.auroali.pronouns.storage.ServerPronounsCache;
import com.mojang.datafixers.DataFixer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.ApiServices;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements PronounsCacheGetter {
    @Shadow
    public abstract Path getSavePath(WorldSavePath worldSavePath);

    @Unique
    protected ServerPronounsCache pronouns$cache;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void pronouns$initCache(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci) {
        File pronounsDir = this.getSavePath(WorldSavePath.ROOT).resolve("pronouns").toFile();
        if (!pronounsDir.exists())
            pronounsDir.mkdir();
        this.pronouns$cache = new ServerPronounsCache((MinecraftServer) (Object) this, (MinecraftServer) (Object) this, pronounsDir);
    }

    @Inject(method = "saveAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;save(ZZZ)Z", shift = At.Shift.AFTER))
    public void pronouns$savePronouns(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (this.pronouns$cache.lastSaved <= this.pronouns$cache.lastModified) {
            if (!suppressLogs)
                PronounsMod.LOGGER.info("Saving Player Pronouns...");
            this.pronouns$cache.save();
        }
    }

    @Override
    public PronounsCache pronouns$get() {
        return this.pronouns$cache;
    }
}
