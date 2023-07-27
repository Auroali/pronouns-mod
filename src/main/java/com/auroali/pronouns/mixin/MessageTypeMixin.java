package com.auroali.pronouns.mixin;

import com.auroali.pronouns.PronounsMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MessageType.class)
public class MessageTypeMixin {
	@Redirect(method = "m_cjdjzedr", at = @At(
		value = "INVOKE",
		target = "Lnet/minecraft/entity/Entity;getDisplayName()Lnet/minecraft/text/Text;"
	))
	private static Text pronouns$m_cjdjzedr(Entity instance) {
		Text text;
		if((text = instance.getDisplayName()) instanceof MutableText mutableText && instance instanceof PlayerEntity entity) {
			String pronouns = PronounsMod.pronouns.getPronouns(entity.getUuid());
			if(pronouns != null)
				text = mutableText.append(Text.literal(" (%s)".formatted(pronouns)));
		}
		return text;
	}
}
