package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableBufferUtils;
import foundry.veil.api.network.handler.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

import java.util.UUID;

/**
 * Gizmo movement packet
 *
 * @param subLevel sub-level id
 * @param position position
 */
public record ServerboundGizmoMoveSubLevelPacket(UUID subLevel, Vector3d position) implements SableTCPPacket {

    public static final Type<ServerboundGizmoMoveSubLevelPacket> TYPE = new Type<>(Sable.sablePath("gizmo_move_sub_level"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundGizmoMoveSubLevelPacket> CODEC = StreamCodec.of((buf, value) -> value.write(buf), ServerboundGizmoMoveSubLevelPacket::read);

    private static ServerboundGizmoMoveSubLevelPacket read(final FriendlyByteBuf buf) {
        return new ServerboundGizmoMoveSubLevelPacket(
                buf.readUUID(),
                SableBufferUtils.read(buf, new Vector3d())
        );
    }

    private void write(final FriendlyByteBuf buf) {
        buf.writeUUID(this.subLevel);
        SableBufferUtils.write(buf, this.position);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(final PacketContext context) {
        final ServerLevel level = (ServerLevel) context.level();

        final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);

        if (!context.player().hasPermissions(1)) {
            Sable.LOGGER.warn("Player {} tried to move a sub-level with gizmo without permission", context.player().getGameProfile().getName());
            return;
        }

        if (container == null) {
            Sable.LOGGER.error("Received a gizmo movement packet for a level without a sub-level container");
            return;
        }

        final SubLevel subLevel = container.getSubLevel(this.subLevel);
        container.physicsSystem().getPipeline().teleport((ServerSubLevel) subLevel, this.position, subLevel.logicalPose().orientation());
    }
}