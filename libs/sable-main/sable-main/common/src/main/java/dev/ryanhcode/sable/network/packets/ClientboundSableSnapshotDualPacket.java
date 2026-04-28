package dev.ryanhcode.sable.network.packets;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacketType;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableBufferUtils;
import foundry.veil.api.network.handler.PacketContext;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Objects;

public final class ClientboundSableSnapshotDualPacket implements SableUDPPacket, SableTCPPacket {
    public static final Type<ClientboundSableSnapshotDualPacket> TYPE = new Type<>(Sable.sablePath("snapshot_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSableSnapshotDualPacket> CODEC = StreamCodec.of((buf, value) -> value.encode(buf), ClientboundSableSnapshotDualPacket::new);
    private final int interpolationTick;
    private final List<Entry> entries;

    public ClientboundSableSnapshotDualPacket(final int interpolationTick, final List<Entry> entries) {
        this.interpolationTick = interpolationTick;
        this.entries = entries;
    }


    @Override
    public void handle(final PacketContext context) {
        this.handleClient(context.level(), PacketReceiveMode.TCP);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(long plotCoordinate, Pose3d pose, Vector3fc linearVelocity,
                        Vector3fc angularVelocity) {

    }

    public ClientboundSableSnapshotDualPacket(final ByteBuf buf) {
        this(buf.readInt(), readList(buf));
    }

    private static List<Entry> readList(final ByteBuf byteBuf) {
        final FriendlyByteBuf buf = ((FriendlyByteBuf) byteBuf);
        final List<Entry> list = new ObjectArrayList<>();

        final int length = buf.readVarInt();

        for (int i = 0; i < length; i++) {
            list.add(new Entry(buf.readLong(), SableBufferUtils.read(buf, new Pose3d()), SableBufferUtils.read(buf, new Vector3f()), SableBufferUtils.read(buf, new Vector3f())));
        }

        return list;
    }

    public void encode(final ByteBuf byteBuf) {
        final FriendlyByteBuf buf = ((FriendlyByteBuf) byteBuf);

        buf.writeInt(this.interpolationTick);
        buf.writeVarInt(this.entries.size());
        for (final Entry entry : this.entries) {
            buf.writeLong(entry.plotCoordinate);
            SableBufferUtils.write(buf, entry.pose);
            SableBufferUtils.write(buf, entry.linearVelocity);
            SableBufferUtils.write(buf, entry.angularVelocity);
        }
    }

    @Override
    public SableUDPPacketType getType() {
        return SableUDPPacketType.SNAPSHOT;
    }

    @Override
    public void handleClient(final Level level) {
        this.handleClient(level, PacketReceiveMode.UDP);
    }

    private void handleClient(final Level level, final PacketReceiveMode packetReceiveMode) {
        final SubLevelContainer container = SubLevelContainer.getContainer(level);

        if (container == null) {
            Sable.LOGGER.error("Received a sub-level movement packet for a level without a sub-level container");
            return;
        }

        for (final Entry entry : this.entries) {
            final SubLevel subLevel = container.getSubLevel(ChunkPos.getX(entry.plotCoordinate), ChunkPos.getZ(entry.plotCoordinate));

            if (!(subLevel instanceof final ClientSubLevel clientSubLevel)) {
                Sable.LOGGER.error("Received a sub-level movement packet for a non-existent sub-level");
                continue;
            }

            ((ClientSubLevelContainer) container).getInterpolation()
                    .receiveSnapshot(clientSubLevel, this.interpolationTick, entry.pose, packetReceiveMode);
        }
    }

    public List<Entry> entries() {
        return this.entries;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final var that = (ClientboundSableSnapshotDualPacket) obj;
        return Objects.equals(this.entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entries);
    }

    @Override
    public String toString() {
        return "ClientboundSableSnapshotDualPacket[" +
                "entries=" + this.entries + ']';
    }

}
