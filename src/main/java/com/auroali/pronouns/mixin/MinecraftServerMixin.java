package com.auroali.pronouns.mixin;

import com.auroali.pronouns.PronounsMod;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllWorlds(ZZZ)Z", shift = At.Shift.AFTER))
	public void pronouns$save(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
		if(PronounsMod.pronouns != null && (force || PronounsMod.pronouns.isDirty())) {
			if(!suppressLogs)
				PronounsMod.LOGGER.info("Saving Player Pronouns...");
			PronounsMod.pronouns.save();

		}
	}
}
