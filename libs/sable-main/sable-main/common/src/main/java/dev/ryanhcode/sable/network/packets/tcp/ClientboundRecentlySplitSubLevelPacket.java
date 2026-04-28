package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableBufferUtils;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ClientboundRecentlySplitSubLevelPacket(UUID splitSubLevelID, UUID splitFromID, Pose3d pose) implements SableTCPPacket {
    public static Type<ClientboundRecentlySplitSubLevelPacket> TYPE = new Type<>(Sable.sablePath("recently_split_sub_level"));
    public static StreamCodec<RegistryFriendlyByteBuf, ClientboundRecentlySplitSubLevelPacket> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            ClientboundRecentlySplitSubLevelPacket::splitSubLevelID,
            UUIDUtil.STREAM_CODEC,
            ClientboundRecentlySplitSubLevelPacket::splitFromID,
            SableBufferUtils.POSE3D_STREAM_CODEC,
            ClientboundRecentlySplitSubLevelPacket::pose,
            ClientboundRecentlySplitSubLevelPacket::new);

    public void handle(final PacketContext context) {
        final SubLevelContainer container = SubLevelContainer.getContainer(context.level());
        if (container instanceof final ClientSubLevelContainer clientContainer) {
            final SubLevel subLevel = container.getSubLevel(this.splitSubLevelID);
            final SubLevel splitFrom = container.getSubLevel(this.splitFromID);

            if (subLevel != null && splitFrom != null) {
                ((ClientSubLevel) subLevel).wasSplitFrom(clientContainer.getInterpolation(), (ClientSubLevel) splitFrom, this.pose);
            } else {
                Sable.LOGGER.error("Attempted to handle a recently split sub-level packet for a sub-level (or origin sub-level) that does not exist!");
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}