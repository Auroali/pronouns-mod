package com.auroali.pronouns.network;

import com.auroali.pronouns.PronounsMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public record SendPronounsS2C(UUID player, String pronouns) implements FabricPacket {
    public static final PacketType<SendPronounsS2C> ID = PacketType.create(PronounsMod.id("send_pronouns"), SendPronounsS2C::new);

    public SendPronounsS2C(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readString());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(player);
        buf.writeString(pronouns);
    }

    @Override
    public PacketType<?> getType() {
        return ID;
    }
}
