package com.auroali.pronouns.network;

import com.auroali.pronouns.PronounsMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

/**
 * Sends pronouns for the specified uuid
 * <br> This differs from UpdatePronounsS2C in how it is handled,
 * this packet is used as the response for loadAsync in the client cache while
 * UpdatePronounsS2C directly sets the pronouns
 * @param player the uuid of the target player
 * @param pronouns the pronouns to use
 */
public record SendPronounsS2C(UUID player, String pronouns) implements FabricPacket {
    public static final PacketType<SendPronounsS2C> ID = PacketType.create(PronounsMod.id("send_pronouns"), SendPronounsS2C::new);

    public SendPronounsS2C(PacketByteBuf buf) {
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
