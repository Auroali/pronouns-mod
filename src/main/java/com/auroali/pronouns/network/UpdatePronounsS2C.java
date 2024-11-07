package com.auroali.pronouns.network;

import com.auroali.pronouns.PronounsMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public record UpdatePronounsS2C(UUID player, String pronouns) implements FabricPacket {
    public static final PacketType<UpdatePronounsS2C> ID = PacketType.create(PronounsMod.id("update_pronouns"), UpdatePronounsS2C::new);

    public UpdatePronounsS2C(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readBoolean() ? buf.readString() : null);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(player);
        buf.writeBoolean(pronouns != null);
        if (pronouns != null)
            buf.writeString(pronouns);
    }

    @Override
    public PacketType<?> getType() {
        return ID;
    }
}
