package com.auroali.pronouns.mixin;

import com.auroali.pronouns.PronounsMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MessageType.class)
public class MessageTypeMixin {
	@Redirect(method = "createParameters(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/entity/Entity;)Lnet/minecraft/network/message/MessageType$Parameters;", at = @At(
		value = "INVOKE",
		target = "Lnet/minecraft/entity/Entity;getDisplayName()Lnet/minecraft/text/Text;"
	))
	private static Text pronouns$m_cjdjzedr(Entity instance) {
		if(instance instanceof PlayerEntity entity) {
			String pronouns = PronounsMod.pronouns.getPronouns(entity.getUuid());
			if(pronouns != null)
				return instance.getDisplayName().copy().append(Text.literal(" %s".formatted(pronouns)).formatted(Formatting.ITALIC));
		}
		return instance.getDisplayName();
	}
}
