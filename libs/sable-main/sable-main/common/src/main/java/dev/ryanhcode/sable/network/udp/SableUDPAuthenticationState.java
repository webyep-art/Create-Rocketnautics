package dev.ryanhcode.sable.network.udp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * The authentication state for a player's connection to the server through UDP
 */
public class SableUDPAuthenticationState {

    private State state;

    private @Nullable UUID outgoingToken;
    private long tokenAssignmentTime;

    private @Nullable InetSocketAddress activeAddress;
    private int lastAlivePingIndex;

    public SableUDPAuthenticationState(final UUID token) {
        this.assignToken(token);
    }

    /**
     * @return The current state of authentication for the player
     */
    public State getState() {
        return this.state;
    }

    /**
     * @return the active address of the player
     */
    public @Nullable InetSocketAddress getActiveAddress() {
        return this.activeAddress;
    }

    /**
     * @param token the token to test
     * @return Whether the token is the expected token for the player
     */
    public boolean isExpectedToken(final UUID token) {
        return this.outgoingToken != null && this.outgoingToken.equals(token);
    }

    /**
     * Assigns a token to the player
     *
     * @param token the token to assign
     */
    public void assignToken(final UUID token) {
        this.outgoingToken = token;
        this.tokenAssignmentTime = System.currentTimeMillis();
        this.state = State.AWAITING_AUTH;
    }

    /**
     * Assigns an address after authentication is complete
     *
     * @param address the address to assign
     */
    public void assignAddress(@NotNull final InetSocketAddress address) {
        this.activeAddress = address;
        this.state = State.AUTHENTICATED;

        this.tokenAssignmentTime = -1;
        this.outgoingToken = null;
    }

    public void setLastAlivePingIndex(final int pingIndex) {
        this.lastAlivePingIndex = pingIndex;
    }

    public int getLastAlivePingIndex() {
        return this.lastAlivePingIndex;
    }

    public enum State {
        AWAITING_AUTH,
        AWAITING_CHALLENGE,
        AUTHENTICATED,
    }
}
