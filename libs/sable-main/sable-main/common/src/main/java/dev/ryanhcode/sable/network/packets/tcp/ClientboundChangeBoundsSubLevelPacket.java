package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.util.SableBufferUtils;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Objects;

public record ClientboundChangeBoundsSubLevelPacket(long plotCoordinate,
                                                    BoundingBox3ic bounds) implements SableTCPPacket {

    public static final Type<ClientboundChangeBoundsSubLevelPacket> TYPE = new CustomPacketPayload.Type<>(Sable.sablePath("change_bounds_sublevel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundChangeBoundsSubLevelPacket> CODEC = StreamCodec.of((buf, value) -> value.write(buf), ClientboundChangeBoundsSubLevelPacket::read);

    private static ClientboundChangeBoundsSubLevelPacket read(final FriendlyByteBuf buf) {
        return new ClientboundChangeBoundsSubLevelPacket(buf.readLong(), SableBufferUtils.read(buf, new BoundingBox3i()));
    }

    private void write(final FriendlyByteBuf buf) {
        buf.writeLong(this.plotCoordinate);
        SableBufferUtils.write(buf, this.bounds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(final PacketContext context) {
        final Level level = context.level();

        final SubLevelContainer container = SubLevelContainer.getContainer(level);

        if (container == null) {
            Sable.LOGGER.error("Received a sub-level tracking packet for a level without a sub-level container");
            return;
        }

        final SubLevel subLevel = container.getSubLevel(ChunkPos.getX(this.plotCoordinate), ChunkPos.getZ(this.plotCoordinate));
        if (subLevel == null) {
            Sable.LOGGER.error("Cannot change bounds of nonexistent sub-level plot");
            return;
        }

        final LevelPlot plot = subLevel.getPlot();
        final BoundingBox3ic previousBoundingBox = new BoundingBox3i(plot.getBoundingBox());

        plot.setBoundingBox(this.bounds);

        if (!Objects.equals(previousBoundingBox, this.bounds)) {
            plot.getSubLevel().onPlotBoundsChanged();
        }
    }
}