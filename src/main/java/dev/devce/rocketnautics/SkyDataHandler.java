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

package dev.devce.rocketnautics;

import dev.devce.rocketnautics.content.physics.SpaceTransitionHandler;
import dev.devce.rocketnautics.network.PlanetMapPayload;
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SkyDataHandler {

    public static final int MAX_POWER_SIZE = 24;
    public static final int MAX_TRUE_SIZE = 2 << MAX_POWER_SIZE; // approximately 30 million
    public static final int MIN_POWER_SIZE = 14;

    public static final int SCALE_FACTOR = 3;

    public static final Map<ServerLevel, SkyDataHandler> HANDLERS = new HashMap<>();

    // level -> height offset, level to pull from
    public static final Map<ResourceKey<Level>, DoubleObjectPair<ResourceKey<Level>>> OVERRIDES = new HashMap<>();

    static {
        OVERRIDES.put(SpaceTransitionHandler.SPACE_DIM, DoubleObjectPair.of(SpaceTransitionHandler.OVERWORLD_SPACE_Y, Level.OVERWORLD));
    }

    public final ServerLevel level;
    protected final RecursiveDataSquare root;

    public SkyDataHandler(ServerLevel level) {
        this.level = level;
        // root is never rendered, only its four children may be rendered at once.
        this.root = new RecursiveDataSquare(null, MAX_POWER_SIZE + 1, -MAX_TRUE_SIZE, -MAX_TRUE_SIZE);
    }

    // client and server (used on client)
    public static double getHeightOffsetForLevel(ResourceKey<Level> level) {
        DoubleObjectPair<ResourceKey<Level>> pair = OVERRIDES.get(level);
        if (pair == null) return 0;
        return pair.firstDouble();
    }

    // server only
    public static SkyDataHandler getHandlerForLevel(ServerLevel level) {
        if (OVERRIDES.containsKey(level.dimension())) {
            level = level.getServer().getLevel(OVERRIDES.get(level.dimension()).right());
        }
        return HANDLERS.computeIfAbsent(level, SkyDataHandler::new);
    }

    public byte[] getRenderDataForDeepSpace(int powerSizeClamp) {
        // we can render the root at this point
        DataSquare square = root;
        while (powerSizeClamp < square.powerSize) {
            if (square instanceof RecursiveDataSquare r) {
                square = r.getChildAtTruePosition(0, 0);
            } else {
                break;
            }
        }
        return square.getRenderData();
    }

    public PlanetMapPayload getRenderDataAtScaleAndPosition(int powerSize, int trueX, int trueZ) {
        // get the square we are on
        DataSquare square = getSquareAtPosition(powerSize, trueX, trueZ);
        // get the nearest four squares
        boolean posX = square.isPosX(trueX);
        boolean posZ = square.isPosZ(trueZ);
        DataSquare posXPosZ;
        DataSquare posXNegZ;
        DataSquare negXPosZ;
        DataSquare negXNegZ;
        int shift = toTrueSize(square.powerSize);
        if (posX) {
            if (posZ) {
                negXNegZ = square;

                posXPosZ = getSquareAtPosition(powerSize, trueX + shift, trueZ + shift);
                posXNegZ = getSquareAtPosition(powerSize, trueX + shift, trueZ);
                negXPosZ = getSquareAtPosition(powerSize, trueX, trueZ + shift);
            } else {
                negXPosZ = square;

                posXPosZ = getSquareAtPosition(powerSize, trueX + shift, trueZ);
                posXNegZ = getSquareAtPosition(powerSize, trueX + shift, trueZ - shift);
                negXNegZ = getSquareAtPosition(powerSize, trueX, trueZ - shift);
            }
        } else {
            if (posZ) {
                posXNegZ = square;

                posXPosZ = getSquareAtPosition(powerSize, trueX, trueZ + shift);
                negXPosZ = getSquareAtPosition(powerSize, trueX - shift, trueZ + shift);
                negXNegZ = getSquareAtPosition(powerSize, trueX - shift, trueZ);
            } else {
                posXPosZ = square;

                posXNegZ = getSquareAtPosition(powerSize, trueX, trueZ - shift);
                negXPosZ = getSquareAtPosition(powerSize, trueX - shift, trueZ);
                negXNegZ = getSquareAtPosition(powerSize, trueX - shift, trueZ - shift);
            }
        }


        return new PlanetMapPayload(square.powerSize, posXPosZ.trueNegXCorner, posXPosZ.trueNegZCorner, posXPosZ.getRenderData(), posXNegZ.getRenderData(), negXPosZ.getRenderData(), negXNegZ.getRenderData());
    }

    protected DataSquare getSquareAtPosition(int powerSize, int trueX, int trueZ) {
        DataSquare square = root;
        do { // we never render the root, so always descend one step before checking powerSize.
            if (square instanceof RecursiveDataSquare r) {
                square = r.getChildAtTruePosition(trueX, trueZ);
            } else {
                break;
            }
        } while (powerSize < square.powerSize);
        return square;
    }

    public static int targetSizeForHeight(double y) {
        int log2Height = (int) (Math.log(y) / Math.log(2));
        return Math.clamp(log2Height + SCALE_FACTOR, MIN_POWER_SIZE, MAX_POWER_SIZE);
    }

    public static double targetSizeForHeightContinuous(double y) {
        double log2Height = Math.log(y) / Math.log(2);
        return Math.clamp(log2Height + SCALE_FACTOR, MIN_POWER_SIZE, MAX_POWER_SIZE);
    }

    public static int toTrueSize(int powerSize) {
        return 2 << powerSize;
    }

    protected class RecursiveDataSquare extends DataSquare {
        private DataSquare childPosXPosZ;
        private DataSquare childPosXNegZ;
        private DataSquare childNegXPosZ;
        private DataSquare childNegXNegZ;

        public RecursiveDataSquare(@Nullable RecursiveDataSquare parent, int powerSize, int trueNegXCorner, int trueNegZCorner) {
            super(parent, powerSize, trueNegXCorner, trueNegZCorner);
        }

        public DataSquare getChildAtTruePosition(int trueX, int trueZ) {
            boolean posX = isPosX(trueX);
            boolean posZ = isPosZ(trueZ);
            if (posX) {
                if (posZ) {
                    return getChildPosXPosZ();
                } else {
                    return getChildPosXNegZ();
                }
            } else {
                if (posZ) {
                    return getChildNegXPosZ();
                } else {
                    return getChildNegXNegZ();
                }
            }
        }

        private DataSquare createChild(int trueNegXCorner, int trueNegZCorner) {
            if (powerSize > MIN_POWER_SIZE + 1) {
                return new RecursiveDataSquare(this, powerSize - 1, trueNegXCorner, trueNegZCorner);
            }
            return new DataSquare(this, powerSize - 1, trueNegXCorner, trueNegZCorner);
        }

        public DataSquare getChildNegXNegZ() {
            if (childNegXNegZ == null) {
                childNegXNegZ = createChild(trueNegXCorner, trueNegZCorner);
            }
            return childNegXNegZ;
        }

        public DataSquare getChildNegXPosZ() {
            if (childNegXPosZ == null) {
                childNegXPosZ = createChild(trueNegXCorner, trueNegZCorner + toTrueSize(powerSize - 1));
            }
            return childNegXPosZ;
        }

        public DataSquare getChildPosXNegZ() {
            if (childPosXNegZ == null) {
                childPosXNegZ = createChild(trueNegXCorner + toTrueSize(powerSize - 1), trueNegZCorner);
            }
            return childPosXNegZ;
        }

        public DataSquare getChildPosXPosZ() {
            if (childPosXPosZ == null) {
                childPosXPosZ = createChild(trueNegXCorner + toTrueSize(powerSize - 1), trueNegZCorner + toTrueSize(powerSize - 1));
            }
            return childPosXPosZ;
        }
    }

    protected class DataSquare {
        public final @Nullable RecursiveDataSquare parent;
        public final int powerSize;
        public final int trueNegXCorner;
        public final int trueNegZCorner;

        protected byte[] renderData;

        public DataSquare(@Nullable RecursiveDataSquare parent, int powerSize, int trueNegXCorner, int trueNegZCorner) {
            this.parent = parent;
            this.powerSize = powerSize;
            this.trueNegXCorner = trueNegXCorner;
            this.trueNegZCorner = trueNegZCorner;
        }

        public byte[] getRenderData() {
            if (renderData == null) {
                buildRenderData();
            }
            return renderData;
        }

        public boolean isPosX(int trueX) {
            return trueX - trueNegXCorner >= toTrueSize(powerSize - 1);
        }

        public boolean isPosZ(int trueZ) {
            return trueZ - trueNegZCorner >= toTrueSize(powerSize - 1);
        }

        protected byte biomeToData(Holder<Biome> biome) {
            byte colorIdx = 4; // Default plains/grass
            if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)) colorIdx = 0;
            else if (biome.is(BiomeTags.IS_RIVER)) colorIdx = 1;
            else if (biome.is(BiomeTags.IS_BEACH)) colorIdx = 2;
            else if (biome.is(BiomeTags.HAS_DESERT_PYRAMID)) colorIdx = 3;
            else if (biome.is(BiomeTags.IS_FOREST)) colorIdx = 5;
            else if (biome.is(BiomeTags.IS_JUNGLE)) colorIdx = 6;
            else if (biome.is(BiomeTags.IS_TAIGA)) colorIdx = 7;
            else if (biome.is(BiomeTags.HAS_VILLAGE_SNOWY)) colorIdx = 8;
            else if (biome.is(BiomeTags.IS_BADLANDS)) colorIdx = 9;
            else if (biome.is(BiomeTags.IS_MOUNTAIN)) colorIdx = 10;
            return colorIdx;
        }

        protected void buildRenderData() {
            renderData = new byte[256 * 256];
            try {
                BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
                Climate.Sampler sampler = level.getChunkSource().randomState().sampler();

                int step = toTrueSize(powerSize - 8); // -8 is equivalent to /256

                for (int x = 0; x < 256; x++) {
                    for (int z = 0; z < 256; z++) {
                        int worldX = trueNegXCorner + x * step;
                        int worldZ = trueNegZCorner + z * step;

                        Holder<Biome> biome = source.getNoiseBiome(worldX >> 2, 64 >> 2, worldZ >> 2, sampler);
                        renderData[x + z * 256] = biomeToData(biome);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
