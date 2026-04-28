package dev.ryanhcode.sable.render.region;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Constructs a voxel-based shadow mesh for chunks.
 * Voxels are added with {@link #add(int, int, int)} and inserted into "grids".</p>
 * <p>After inserting all voxels, {@link #build()} will calculate the
 * optimal merging of all voxels into a set of cubes.</p>
 * <p>Finally, {@link #render(Matrix4f, VertexConsumer)} will put all calculated shapes
 * into a buffer for later rendering.</p>
 *
 * @author Ocelot
 */
public class SimpleCulledRenderRegionBuilder {

    protected static final Comparator<Cube> Z_SORTER = Comparator.comparingInt(cube -> cube.x + cube.sizeX);
    protected static final Comparator<Cube> Y_SORTER = Z_SORTER.thenComparingInt(cube -> cube.z + cube.sizeZ);

    protected final int gridSize;
    protected final List<Cube> cubes;
    protected final BitSet grid;

    /**
     * Creates a new mesh builder with the specified grid size.
     *
     * @param gridSize The number of voxels in each axis
     */
    public SimpleCulledRenderRegionBuilder(final int gridSize) {
        this.gridSize = gridSize;
        this.cubes = new LinkedList<>();
        this.grid = new BitSet(this.gridSize * this.gridSize * this.gridSize);
    }

    private int getGridIndex(final int x, final int y, final int z) {
        return (z * this.gridSize + y) * this.gridSize + x;
    }

    private boolean hasVoxel(final int x, final int y, final int z) {
        for (int i = 0; i < 1; i++) {

            final int vx = x >> i;
            final int vy = y >> i;
            final int vz = z >> i;

            final int sideLength = this.gridSize >> i;
            if (vx < 0 || vx >= sideLength ||
                    vy < 0 || vy >= sideLength ||
                    vz < 0 || vz >= sideLength) {
                return false;
            }

            if (this.grid.get(this.getGridIndex(vx, vy, vz))) {
                return true;
            }
        }

        return false;
    }

    protected boolean shouldFaceRender(@NotNull final Cube cube, @NotNull final Direction direction) {
        final int x0 = cube.x;
        final int y0 = cube.y;
        final int z0 = cube.z;
        final int x1 = cube.x + cube.sizeX;
        final int y1 = cube.y + cube.sizeY;
        final int z1 = cube.z + cube.sizeZ;

        return switch (direction) {
            case DOWN -> {
                for (int x = x0; x < x1; x++) {
                    for (int z = z0; z < z1; z++) {
                        if (!this.hasVoxel(x, y0 - 1, z)) {
                            yield true;
                        }
                    }
                }

                yield false;
            }
            case UP -> {
                for (int x = x0; x < x1; x++) {
                    for (int z = z0; z < z1; z++) {
                        if (!this.hasVoxel(x, y1, z)) {
                            yield true;
                        }
                    }
                }

                yield false;
            }
            case NORTH -> {
                for (int x = x0; x < x1; x++) {
                    for (int y = y0; y < y1; y++) {
                        if (!this.hasVoxel(x, y, z0 - 1)) {
                            yield true;
                        }
                    }
                }

                yield false;
            }
            case SOUTH -> {
                for (int x = x0; x < x1; x++) {
                    for (int y = y0; y < y1; y++) {
                        if (!this.hasVoxel(x, y, z1)) {
                            yield true;
                        }
                    }
                }

                yield false;
            }
            case WEST -> {
                for (int z = z0; z < z1; z++) {
                    for (int y = y0; y < y1; y++) {
                        if (!this.hasVoxel(x0 - 1, y, z)) {
                            yield true;
                        }
                    }
                }

                yield false;
            }
            case EAST -> {
                for (int z = z0; z < z1; z++) {
                    for (int y = y0; y < y1; y++) {
                        if (!this.hasVoxel(x1, y, z)) {
                            yield true;
                        }
                    }
                }

                yield false;
            }
        };
    }

    private void mergeX() {
        for (int y = 0; y < this.gridSize; y++) {
            for (int z = 0; z < this.gridSize; z++) {

                int startX = -1;
                for (int x = 0; x < this.gridSize; x++) {
                    final boolean set = this.grid.get(this.getGridIndex(x, y, z));

                    if (startX == -1) {
                        if (set) {
                            startX = x;
                        }
                        continue;
                    }

                    if (set) {
                        continue;
                    }

                    this.cubes.add(new Cube(startX, y, z, x - startX, 1, 1));
                    startX = -1;
                }

                if (startX != -1) {
                    this.cubes.add(new Cube(startX, y, z, this.gridSize - startX, 1, 1));
                }
            }
        }
    }

    private void mergeZ() {
        this.cubes.sort(Z_SORTER);

        int startIndex = -1;
        int x = 0;
        int y = 0;
        int sizeX = 0;
        int sizeZ = 0;
        int nextZ = 0;
        for (int i = 0; i < this.cubes.size(); i++) {
            final Cube cube = this.cubes.get(i);
            if (startIndex == -1) {
                startIndex = i;
                x = cube.x;
                y = cube.y;
                sizeX = cube.sizeX;
                sizeZ = cube.sizeZ;
                nextZ = cube.z + sizeZ;
                continue;
            }

            if (cube.sizeX == sizeX &&
                    cube.sizeZ == sizeZ &&
                    cube.x == x &&
                    cube.y == y &&
                    cube.z == nextZ) {

                // If there are more cubes, try to merge
                if (i < this.cubes.size() - 1) {
                    nextZ += sizeZ;
                    continue;
                }
                i++;
            }

            final int length = i - startIndex - 1;
            if (length > 0) {
                final Cube start = this.cubes.get(startIndex);
                final Cube end = this.cubes.get(startIndex + length);
                for (int j = 0; j <= length; j++) {
                    this.cubes.remove(startIndex);
                }
                this.cubes.add(startIndex, new Cube(start.x,
                        start.y,
                        start.z,
                        sizeX,
                        start.sizeY,
                        end.z - start.z + end.sizeZ));
            }

            startIndex = -1;
            i -= length + 1;
        }
    }

    private void mergeY() {
        this.cubes.sort(Y_SORTER);

        int startIndex = -1;
        int x = 0;
        int z = 0;
        int sizeX = 0;
        int sizeY = 0;
        int sizeZ = 0;
        int nextY = 0;
        for (int i = 0; i < this.cubes.size(); i++) {
            final Cube cube = this.cubes.get(i);
            if (startIndex == -1) {
                startIndex = i;
                x = cube.x;
                z = cube.z;
                sizeX = cube.sizeX;
                sizeY = cube.sizeY;
                sizeZ = cube.sizeZ;
                nextY = cube.y + sizeY;
                continue;
            }

            if (cube.sizeX == sizeX &&
                    cube.sizeZ == sizeZ &&
                    cube.x == x &&
                    cube.y == nextY &&
                    cube.z == z) {

                // If there are more cubes, try to merge
                if (i < this.cubes.size() - 1) {
                    nextY += sizeY;
                    continue;
                }
                i++;
            }

            final int length = i - startIndex - 1;
            if (length > 0) {
                final Cube start = this.cubes.get(startIndex);
                final Cube end = this.cubes.get(startIndex + length);
                for (int j = 0; j <= length; j++) {
                    this.cubes.remove(startIndex);
                }
                this.cubes.add(startIndex, new Cube(start.x,
                        start.y,
                        start.z,
                        sizeX,
                        end.y - start.y + end.sizeY,
                        sizeZ));
            }

            startIndex = -1;
            i -= length + 1;
        }
    }

    /**
     * Adds a voxel at the specified position and size.
     *
     * @param x The single x position
     * @param y The single y position
     * @param z The single z position
     */
    public void add(final int x, final int y, final int z) {
        this.grid.set(this.getGridIndex(x, y, z));
    }

    /**
     * Converts the generated voxel grids into renderable shapes.
     */
    public void build() {
        this.cubes.clear();

        this.mergeX();
        this.mergeZ();
        this.mergeY();
    }

    /**
     * Converts the generated voxel grids into renderable shapes.
     */
    public void buildNoGreedy() {
        this.cubes.clear();

        for (int y = 0; y < this.gridSize; y++) {
            for (int z = 0; z < this.gridSize; z++) {
                for (int x = 0; x < this.gridSize; x++) {
                    if (this.grid.get(this.getGridIndex(x, y, z))) {
                        this.cubes.add(new Cube(x, y, z, 1, 1, 1));
                    }
                }
            }
        }
    }

    /**
     * Renders all cubes into the specified consumer.
     *
     * @param consumer The consumer to draw cubes into
     */
    public void render(@NotNull final Matrix4f matrix4f, @NotNull final VertexConsumer consumer) {
        for (final Cube cube : this.cubes) {
            final int x0 = cube.x;
            final int y0 = cube.y;
            final int z0 = cube.z;
            final int x1 = cube.x + cube.sizeX;
            final int y1 = cube.y + cube.sizeY;
            final int z1 = cube.z + cube.sizeZ;

            if (this.shouldFaceRender(cube, Direction.NORTH)) {
                consumer.addVertex(matrix4f, x0, y0, z0);
                consumer.addVertex(matrix4f, x0, y1, z0);
                consumer.addVertex(matrix4f, x1, y1, z0);
                consumer.addVertex(matrix4f, x1, y0, z0);
            }

            if (this.shouldFaceRender(cube, Direction.EAST)) {
                consumer.addVertex(matrix4f, x1, y0, z0);
                consumer.addVertex(matrix4f, x1, y1, z0);
                consumer.addVertex(matrix4f, x1, y1, z1);
                consumer.addVertex(matrix4f, x1, y0, z1);
            }

            if (this.shouldFaceRender(cube, Direction.SOUTH)) {
                consumer.addVertex(matrix4f, x1, y0, z1);
                consumer.addVertex(matrix4f, x1, y1, z1);
                consumer.addVertex(matrix4f, x0, y1, z1);
                consumer.addVertex(matrix4f, x0, y0, z1);
            }

            if (this.shouldFaceRender(cube, Direction.WEST)) {
                consumer.addVertex(matrix4f, x0, y0, z1);
                consumer.addVertex(matrix4f, x0, y1, z1);
                consumer.addVertex(matrix4f, x0, y1, z0);
                consumer.addVertex(matrix4f, x0, y0, z0);
            }

            if (this.shouldFaceRender(cube, Direction.DOWN)) {
                consumer.addVertex(matrix4f, x0, y0, z0);
                consumer.addVertex(matrix4f, x1, y0, z0);
                consumer.addVertex(matrix4f, x1, y0, z1);
                consumer.addVertex(matrix4f, x0, y0, z1);
            }

            if (this.shouldFaceRender(cube, Direction.UP)) {
                consumer.addVertex(matrix4f, x0, y1, z1);
                consumer.addVertex(matrix4f, x1, y1, z1);
                consumer.addVertex(matrix4f, x1, y1, z0);
                consumer.addVertex(matrix4f, x0, y1, z0);
            }
        }
    }

    /**
     * @return All cubes in this mesh
     */
    public @NotNull List<Cube> getCubes() {
        return this.cubes;
    }

    /**
     * A single cube segment of a mesh.
     *
     * @param x     The x-coordinate of the cube
     * @param y     The y-coordinate of the cube
     * @param z     The z-coordinate of the cube
     * @param sizeX The size in the x of the cube
     * @param sizeY The size in the y of the cube
     * @param sizeZ The size in the z of the cube
     */
    public record Cube(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
    }
}