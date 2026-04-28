package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinition;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinitionLoader;
import foundry.veil.api.network.handler.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundPhysicsPropertyPacket(PhysicsBlockPropertiesDefinition definition) implements SableTCPPacket {
    public static final Type<ClientboundPhysicsPropertyPacket> TYPE = new CustomPacketPayload.Type<>(Sable.sablePath("physics_property"));

    public static final StreamCodec<ByteBuf, ClientboundPhysicsPropertyPacket> CODEC = StreamCodec.composite(
            PhysicsBlockPropertiesDefinition.STREAM_CODEC, ClientboundPhysicsPropertyPacket::definition,
            ClientboundPhysicsPropertyPacket::new
    );

    @Override
    public void handle(final PacketContext context) {
        Minecraft.getInstance().execute(() -> {
            PhysicsBlockPropertiesDefinitionLoader.applyToBlocks(this.definition);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
