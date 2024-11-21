package com.auroali.pronouns.network;

import com.auroali.pronouns.PronounsMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public record ClientPronounsLoadRequestC2S(UUID requestedPronouns) implements FabricPacket {
    public static final PacketType<ClientPronounsLoadRequestC2S> ID = PacketType.create(PronounsMod.id("request_pronouns"), ClientPronounsLoadRequestC2S::new);

    public ClientPronounsLoadRequestC2S(PacketByteBuf buf) {
        this(buf.readUuid());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(requestedPronouns);
    }

    @Override
    public PacketType<?> getType() {
        return ID;
    }
}
