/*
 * This file is part of Cosmonautics.
 * Cosmonautics is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cosmonautics is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cosmonautics.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SeamlessTransitionPayload(boolean active) implements CustomPacketPayload {
    public static final Type<SeamlessTransitionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "seamless_transition"));
    public static final StreamCodec<FriendlyByteBuf, SeamlessTransitionPayload> CODEC = StreamCodec.of(
        (buf, val) -> buf.writeBoolean(val.active),
        buf -> new SeamlessTransitionPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
