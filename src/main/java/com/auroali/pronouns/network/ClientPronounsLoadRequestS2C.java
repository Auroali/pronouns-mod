package com.auroali.pronouns.network;

import com.auroali.pronouns.PronounsMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

import java.util.Optional;
import java.util.UUID;

/**
 * Sends pronouns for the specified uuid
 * <br> This differs from UpdatePronounsS2C in how it is handled,
 * this packet is used as the response for loadAsync in the client cache while
 * UpdatePronounsS2C directly sets the pronouns
 *
 * @param player   the uuid of the target player
 * @param pronouns the pronouns to use
 */
public record ClientPronounsLoadRequestS2C(UUID player, Optional<String> pronouns) implements FabricPacket {
    public static final PacketType<ClientPronounsLoadRequestS2C> ID = PacketType.create(PronounsMod.id("send_pronouns"), ClientPronounsLoadRequestS2C::new);

    public ClientPronounsLoadRequestS2C(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readOptional(PacketByteBuf::readString));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(player);
        buf.writeOptional(pronouns, PacketByteBuf::writeString);
    }

    @Override
    public PacketType<?> getType() {
        return ID;
    }
}
