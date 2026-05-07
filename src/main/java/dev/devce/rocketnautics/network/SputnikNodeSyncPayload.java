package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SputnikNodeSyncPayload(BlockPos pos, CompoundTag graphData) implements CustomPacketPayload {
    public static final Type<SputnikNodeSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "sputnik_node_sync"));

    public static final StreamCodec<FriendlyByteBuf, SputnikNodeSyncPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SputnikNodeSyncPayload::pos,
            ByteBufCodecs.COMPOUND_TAG,
            SputnikNodeSyncPayload::graphData,
            SputnikNodeSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
