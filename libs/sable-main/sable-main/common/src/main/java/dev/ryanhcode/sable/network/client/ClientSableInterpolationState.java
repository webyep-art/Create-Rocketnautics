package dev.ryanhcode.sable.network.client;

import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.packets.PacketReceiveMode;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * TODO: there's too large of a window that we're okay with being within for latency / delay.
 */
public class ClientSableInterpolationState {
    public static final boolean RENDER_INTERPOLATION_BOUNDS = false;

    private final Minecraft minecraft = Minecraft.getInstance();

    /**
     * The most recent tick we have received from the server
     */
    private double mostRecentTick = -1;

    /**
     * If we have received any update so far
     */
    private boolean receivedFirstUpdate;

    /**
     * The current stepping interpolation tick. This *should* be as aligned as possible with the server tick,
     * so that we can step back by the delay in ticks to use as a pointer for the snapshot interpolation.
     */
    private double interpolationTick;

    /**
     * The running estimate we have of the server tick speed, as a multiplier of the 20tps expected
     */
    private double estimatedServerTickSpeed;

    /**
     * The latest information from the server we have on the spacing between the latest server update and the previous one
     */
    private float serverMsFromLastUpdate;

    /**
     * If we should be receiving consistent updates from the server regarding the interpolation tick
     */
    private boolean stopped = true;

    /**
     * The receive mode of the most recent packet, for debug rendering
     */
    private PacketReceiveMode receivingMode = PacketReceiveMode.UNKNOWN;

    private double latestDelay;
    public double mostRecentInterpolationTick;
    public double lastInterpolationTick;

    public void tick() {
        if (!this.receivedFirstUpdate) {
            return;
        }

        final float rate = this.minecraft.level.tickRateManager().tickrate();
        final float expectedMsBetween = 1000.0f / rate;

        if (!this.stopped) {
            this.estimatedServerTickSpeed = Mth.lerp(0.05, this.estimatedServerTickSpeed, expectedMsBetween / Math.max(1, this.serverMsFromLastUpdate));
        }

        this.interpolationTick += this.estimatedServerTickSpeed;
        this.interpolationTick = Mth.clamp(this.interpolationTick, this.mostRecentTick - this.getInterpolationDelay(), this.mostRecentTick + 1.5);

        this.latestDelay = this.mostRecentTick - this.interpolationTick + this.getInterpolationDelay();

        this.lastInterpolationTick = this.mostRecentInterpolationTick;
        this.mostRecentInterpolationTick = this.getTickPointer();
    }

    /**
     * The interpolation tick at which we are sampling from the snapshot buffers at
     */
    public double getTickPointer() {
        return this.interpolationTick - this.getInterpolationDelay();
    }

    public void receiveSnapshot(final ClientSubLevel clientSubLevel, final int gameTick, final Pose3dc data, final PacketReceiveMode packetReceiveMode) {
        this.receivingMode = packetReceiveMode;
        clientSubLevel.getInterpolator().receiveSnapshot(gameTick, data);
    }

    @ApiStatus.Internal
    public void addDebugInfo(final Consumer<String> consumer) {
        consumer.accept(String.format("Delay: %.2ft", this.latestDelay));
        consumer.accept(String.format("Estimated Send-rate: %.2ft", this.estimatedServerTickSpeed));

        if (this.interpolationTick - this.getInterpolationDelay() > this.mostRecentTick) {
            consumer.accept(ChatFormatting.RED + "Past most-recent tick");
        }

        consumer.accept("Interpolation " + (this.stopped ? "stopped" : "running"));

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            consumer.accept("Networking locally");
        } else {
            consumer.accept("Networking through " + this.receivingMode.name());
        }
    }

    public double getInterpolationDelay() {
//        return 1.5f;
        return SableClientConfig.INTERPOLATION_DELAY.getAsDouble();
    }

    public void receiveInfo(final int msSinceLast, final int gameTick, final boolean stopped) {
        if (gameTick < this.mostRecentTick) return;

        if (!this.receivedFirstUpdate || this.stopped && !stopped) {
            this.interpolationTick = gameTick;
            this.estimatedServerTickSpeed = 1.0f;

            this.receivedFirstUpdate = true;
        }

        this.stopped = stopped;
        this.mostRecentTick = gameTick;
        this.serverMsFromLastUpdate = msSinceLast;
    }

    public boolean isStopped() {
        return this.stopped;
    }
}
