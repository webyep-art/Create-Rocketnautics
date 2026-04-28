package dev.ryanhcode.sable.debug;

import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundGizmoMoveSubLevelPacket;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.UUID;

public class GizmoScreen extends Screen {
    private boolean dragging;
    private @Nullable GizmoSelection activeSelection;

    protected GizmoScreen() {
        super(Component.literal("Gizmo Mode"));
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int i, final int j, final float f) {
//        super.render(guiGraphics, i, j, f);
    }

    @Override
    public boolean mouseClicked(final double d, final double e, final int i) {
        final SableClientGizmoHandler gizmoHandler = SableClient.GIZMO_HANDLER;
        if (gizmoHandler.getSelection() != null) {
            this.activeSelection = gizmoHandler.getSelection();
            this.dragging = true;
        }

        return super.mouseClicked(d, e, i);
    }

    @Override
    public boolean mouseReleased(final double d, final double e, final int i) {
        this.dragging = false;
        this.activeSelection = null;

        return super.mouseReleased(d, e, i);
    }

    @Override
    public boolean mouseDragged(final double x, final double y, final int i, final double f, final double g) {
        final SableClientGizmoHandler gizmoHandler = SableClient.GIZMO_HANDLER;

        if (this.dragging) {
            final Minecraft minecraft = Minecraft.getInstance();
            final ClientLevel level = minecraft.level;
            final SubLevelContainer container = SubLevelContainer.getContainer(level);
            assert container != null;

            final UUID subLevelID = this.activeSelection.subLevel();
            final SubLevel subLevel = container.getSubLevel(subLevelID);
            if (subLevel == null) {
                this.cancel();
                return super.mouseDragged(x, y, i, f, g);
            }

            final int ordinal = (this.activeSelection.axis().ordinal() + 1) % 3;
            final Direction.Axis axis = Direction.Axis.VALUES[ordinal];

            final Vector3d dragNormal = JOMLConversion.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE, this.activeSelection.axis()).getNormal());

            final Vector3d pos = JOMLConversion.toJOML(minecraft.player.getEyePosition());
            final Vector3d relativePos = new Vector3d(pos).sub(subLevel.logicalPose().position());

            final Vector3d planeNormal = JOMLConversion.atLowerCornerOf(Direction.get(Direction.AxisDirection.POSITIVE, axis).getNormal());
            if (relativePos.dot(planeNormal) < 0.0) {
                planeNormal.negate();
            }

            final Vector3d dir = JOMLConversion.toJOML(gizmoHandler.getMouseDir());

            final boolean hitsPlane = dir.dot(planeNormal) < 0.0;

            if (hitsPlane) {
                final Vector3d negatedPlaneNormal = planeNormal.negate(new Vector3d());
                final double d = planeNormal.dot(relativePos);

                final double rayLength = d / dir.dot(negatedPlaneNormal);

                final Vector3d hitPos = new Vector3d(pos).fma(rayLength, dir);

                final Vector3d subLevelPos = new Vector3d(subLevel.logicalPose().position());
                subLevelPos.fma(-subLevelPos.dot(dragNormal), dragNormal, subLevelPos);
                subLevelPos.fma(hitPos.dot(dragNormal), dragNormal, subLevelPos);

                VeilPacketManager.server().sendPacket(new ServerboundGizmoMoveSubLevelPacket(this.activeSelection.subLevel(), subLevelPos));
            }
        }

        return super.mouseDragged(x, y, i, f, g);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void cancel() {
        this.dragging = false;
        this.activeSelection = null;
    }

    @Override
    public void onClose() {
        SableClient.GIZMO_HANDLER.stop();
    }
}
