package dev.ryanhcode.sable.sublevel.storage.region;

import dev.ryanhcode.sable.Sable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;

/**
 * A storage file for sub-levels.
 * One SubLevelStorage file can store 1,024 sub-levels,
 * each occupying a span of sectors in the file.
 */
public class SubLevelStorageFile implements AutoCloseable {
    public static final String FILE_EXTENSION = ".slvls";
    public static final String SINGLE_FILE_EXTENSION = ".slvl";
    private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);
    public static boolean COMPRESS_DATA = true;
    public static int EXTERNAL_MASK = 16;
    /**
     * A bit set of occupied sectors. False sector indices do not contain any data,
     * and true sector indices contain current sub-level data.
     */
    protected final BitSet usedSectors = new BitSet();
    protected final BitSet usedIndices = new BitSet();
    private final int beginningSectorSize = 4096;
    private final int sectorSize;
    private final Path path;
    private final Path externalFileDir;
    private final FileChannel file;
    /**
     * The header of the file
     */
    private final ByteBuffer header;

    /**
     * A buffer containing the offsets of each sub-level in the file, stored in the header of the storage files.
     * Each span value follows the format 0xPPPPPPLL, with PPPPP being the offset of the sub-level in the file and
     * LL being the length of the sub-level in sectors.
     */
    private final IntBuffer sectorSpans;

    public SubLevelStorageFile(final Path path, final Path externalFileDir, final int sectorSize) throws IOException {
        this.path = path;
        this.externalFileDir = externalFileDir;

        this.sectorSize = sectorSize;
        this.file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);

        final int totalIndexCount = this.beginningSectorSize / Integer.BYTES;
        this.header = ByteBuffer.allocateDirect(this.beginningSectorSize);
        this.sectorSpans = this.header.asIntBuffer();
        this.sectorSpans.limit(totalIndexCount);

        this.header.position(0);
        this.usedSectors.set(0, this.beginningSectorSize / this.sectorSize, true);

        final long existingSize = Files.size(path);
        final int headerBytesRead = this.file.read(this.header, 0L);

        if (headerBytesRead != -1) {
            if (headerBytesRead != this.beginningSectorSize) {
                Sable.LOGGER.error("Sub-level storage file {} has truncated header: {}", path, headerBytesRead);
            }

            for (int spanIndex = 0; spanIndex < totalIndexCount; spanIndex++) {
                final int span = this.sectorSpans.get(spanIndex);

                this.usedIndices.set(spanIndex, span != 0);

                if (span == 0) {
                    continue;
                }

                final int spanStart = this.getSpanStart(span);
                final int spanLength = this.getSpanLength(span);

                if (((long) spanStart) * sectorSize > existingSize) {
                    // we're out of bounds of the file
                    Sable.LOGGER.warn("SubLevelStorageFile: Start of span at index {} in file {} is out of bounds (span start: {}, span length: {}, file size: {})",
                            spanIndex, path, spanStart, spanLength, existingSize);
                }

                if (spanStart < 0 || spanLength <= 0) {
                    Sable.LOGGER.warn("SubLevelStorageFile: Invalid span at index {} in file {}", spanIndex, path);
                    continue;
                }

                // if already occupied we have overlap
                for (int i = spanStart; i < spanStart + spanLength; i++) {
                    if (this.usedSectors.get(i)) {
                        Sable.LOGGER.warn("SubLevelStorageFile: Overlapping span at index {} in file {}", spanIndex, path);
                    }
                }

                this.usedSectors.set(spanStart, spanStart + spanLength, true);
            }
        }
    }

    public SubLevelStorageFile(final Path path, final Path externalFileDir) throws IOException {
        this(path, externalFileDir, 4096);
    }

    private static boolean isExternalStreamChunk(final byte b) {
        return (b & EXTERNAL_MASK) != 0;
    }

    public int findFreeIndex() {
        return this.usedIndices.nextClearBit(0);
    }

    public int getTotalIndexCapacity() {
        return this.beginningSectorSize / Integer.BYTES;
    }

    private int sizeToSectors(final int sizeBytes) {
        return (sizeBytes + this.sectorSize - 1) / this.sectorSize;
    }

    private Path getExternalFilePath(final int index) {
        final String string = index + SINGLE_FILE_EXTENSION;
        return this.externalFileDir.resolve(string);
    }

    @Nullable
    private InputStream createExternalSubLevelInputStream(final int index) throws IOException {
        final Path path = this.getExternalFilePath(index);
        if (!Files.isRegularFile(path)) {
            Sable.LOGGER.error("External sub-level path {} is not file", path);
            return null;
        } else {
            return Files.newInputStream(path);
        }
    }

    @Nullable
    public DataInputStream getSubLevelDataInputStream(final int index) throws IOException {
        final int span = this.sectorSpans.get(index);

        final int spanStart = this.getSpanStart(span);
        final int spanLength = this.getSpanLength(span);

        if (spanStart == 0) {
            return null;
        }

        if (spanLength <= 0 || spanStart + spanLength > this.usedSectors.length()) {
            Sable.LOGGER.error("SubLevelStorageFile: Invalid span at index {} in file {}", index, this.path);
            return null;
        }

        final int bufferSize = spanLength * this.sectorSize;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        this.file.read(byteBuffer, (long) spanStart * this.sectorSize);
        byteBuffer.flip();

        if (byteBuffer.remaining() < Integer.BYTES + 1) {
            Sable.LOGGER.error("SubLevelStorageFile: Not enough data to read sector data header at index {} in file {}", index, this.path);
            return null;
        }

        final int subLevelRemainingBytes = byteBuffer.getInt();
        final byte dataType = byteBuffer.get();

        if (subLevelRemainingBytes == 0) {
            Sable.LOGGER.warn("SubLevelStorageFile: Invalid sector data size at index {} in file {}: {}", index, this.path, subLevelRemainingBytes);
            return null;
        }

        final int actualRemainingBytes = subLevelRemainingBytes - 1;

        if (isExternalStreamChunk(dataType)) {
            if (actualRemainingBytes != 0) {
                Sable.LOGGER.warn("Sub-level has both internal and external streams");
            }

            return new DataInputStream(this.createExternalSubLevelInputStream(index));
        } else if (actualRemainingBytes > byteBuffer.remaining()) {
            Sable.LOGGER.error("Sub-level {} stream is truncated: expected {} but read {}", index, actualRemainingBytes, byteBuffer.remaining());
            return null;
        } else if (actualRemainingBytes < 0) {
            Sable.LOGGER.error("Declared size {} of sub-level {} is negative", subLevelRemainingBytes, index);
            return null;
        }

        return new DataInputStream(new ByteArrayInputStream(byteBuffer.array(), byteBuffer.position(), actualRemainingBytes));
    }

    private void writeHeader() throws IOException {
        this.header.position(0);
        this.file.write(this.header, 0L);
    }

    private ByteBuffer createExternalStub() {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(5);
        byteBuffer.putInt(1);
        byteBuffer.put((byte) EXTERNAL_MASK);
        byteBuffer.flip();
        return byteBuffer;
    }

    /**
     * Writes a sub-levels data to disk
     *
     * @param index      the index of the sub-level to write
     * @param byteBuffer the byte buffer containing the sub-level data
     * @throws IOException if an I/O error occurs while writing to the file
     */
    protected void write(final int index, final ByteBuffer byteBuffer) throws IOException {
        // get the previous span so we can clear it if it was used
        final int oldSpan = this.sectorSpans.get(index);
        final int oldSectorStart = this.getSpanStart(oldSpan);
        final int oldSpanLength = this.getSpanLength(oldSpan);

        // allocate space according to the size of the byte buffer
        final int remaining = byteBuffer.remaining();
        int sectorsNeeded = this.sizeToSectors(remaining);

        boolean savingToExternalFile = false;
        Path temporaryExternalFile = null;

        if (sectorsNeeded > 255) {
            // Too large for one span! Let's store to an external file.
            savingToExternalFile = true;
            // We only need 1 sector to store the stub that points to the external file
            sectorsNeeded = 1;
        }

        final int sectorWriteStart = this.allocateSpace(sectorsNeeded);

        if (savingToExternalFile) {
            temporaryExternalFile = this.writeToExternalFile(byteBuffer);

            final ByteBuffer stub = this.createExternalStub();
            this.file.write(stub, (long) sectorWriteStart * this.sectorSize);
        } else {
            // write into that space
            this.file.write(byteBuffer, (long) sectorWriteStart * this.sectorSize);
        }

        this.sectorSpans.put(index, this.packSpan(sectorWriteStart, sectorsNeeded));
        this.usedIndices.set(index, true);
        this.writeHeader();

        if (savingToExternalFile) {
            Files.move(temporaryExternalFile, this.getExternalFilePath(index), StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(this.getExternalFilePath(index));
        }

        // clear the previous span of sectors if we used to store data there for this sub-level index
        if (oldSectorStart != 0) {
            this.usedSectors.clear(oldSectorStart, oldSectorStart + oldSpanLength);
        }
    }

    private Path writeToExternalFile(final ByteBuffer byteBuffer) throws IOException {
        final Path tempFile = Files.createTempFile(this.externalFileDir, "tmp", null);
        final FileChannel fileChannel = FileChannel.open(tempFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        try {
            byteBuffer.position(5);
            fileChannel.write(byteBuffer);
        } catch (final Throwable throwable1) {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (final Throwable throwable) {
                    throwable1.addSuppressed(throwable);
                }
            }

            throw throwable1;
        }

        if (fileChannel != null) {
            fileChannel.close();
        }

        return tempFile;
    }

    /**
     * Allocates sectors for a sub-level.
     * Finds an open space `spaceNeeded` sectors long in the used sectors bitset,
     * and marks that space as used, returning the start index.
     *
     * @param spaceNeeded the number of sectors needed for the sub-level data
     * @return the starting index of the allocated space in the used sectors bitset
     */
    public int allocateSpace(final int spaceNeeded) {
        int j = 0;

        while (true) {
            final int start = this.usedSectors.nextClearBit(j);
            final int nextSetBit = this.usedSectors.nextSetBit(start);
            if (nextSetBit == -1 || nextSetBit - start >= spaceNeeded) {
                this.usedSectors.set(start, start + spaceNeeded);
                return start;
            }

            j = nextSetBit;
        }
    }

    /**
     * Writes a sub-level tag to the storage file.
     *
     * @param index       the index of the sub-level to write
     * @param compoundTag the NBT tag containing the sub-level data, or null to clear the sub-level
     * @throws IOException if an I/O error occurs while writing to the file
     */
    public void write(final int index, @Nullable final CompoundTag compoundTag) throws IOException {
        if (compoundTag == null) {
            this.clear(index);
        } else {
            final DataOutputStream dataOutputStream = this.getSubLevelDataOutputStream(index);

            try {
                if (COMPRESS_DATA) {
                    NbtIo.writeCompressed(compoundTag, dataOutputStream);
                } else {
                    NbtIo.write(compoundTag, dataOutputStream);
                }
            } catch (final Throwable exception) {
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (final Throwable anotherException) {
                        exception.addSuppressed(anotherException);
                    }
                }

                throw exception;
            }

            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
        }
    }

    /**
     * Reads a sub-level tag from the storage file.
     *
     * @param index the index of the sub-level to read
     * @throws IOException
     */
    public CompoundTag read(final int index) throws IOException {
        final DataInputStream dataInputStream = this.getSubLevelDataInputStream(index);
        if (dataInputStream == null) {
            return null;
        }

        try {
            if (COMPRESS_DATA) {
                return NbtIo.readCompressed(dataInputStream, NbtAccounter.unlimitedHeap());
            } else {
                return NbtIo.read(dataInputStream);
            }
        } catch (final Throwable exception) {
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (final Throwable anotherException) {
                    exception.addSuppressed(anotherException);
                }
            }

            throw exception;
        } finally {
            if (dataInputStream != null) {
                dataInputStream.close();
            }
        }
    }

    private void clear(final int index) throws IOException {
        final int span = this.sectorSpans.get(index);

        if (span != 0) {
            this.sectorSpans.put(index, 0);
            this.usedIndices.clear(index);

            final int spanStart = this.getSpanStart(span);
            this.usedSectors.clear(spanStart, spanStart + this.getSpanLength(span));

            this.writeHeader();
        }
    }

    public DataOutputStream getSubLevelDataOutputStream(final int index) {
        return new DataOutputStream(new SectorSpanDataBuffer(index));
    }

    public Path getPath() {
        return this.path;
    }

    public int getSpanStart(final int span) {
        return span >> 8 & 0xFFFFFF; // Extract the offset
    }

    public int getSpanLength(final int span) {
        return span & 0xFF; // Extract the length
    }

    public int packSpan(final int start, final int length) {
        if (start < 0 || length <= 0 || length > 255) {
            throw new IllegalArgumentException("Invalid span: start=" + start + ", length=" + length);
        }
        return (start << 8) | length; // Pack the offset and length into a single integer
    }

    /**
     * Frees any native resources held by this object.
     */
    @Override
    public void close() throws IOException {
        try {
            this.padOrTruncateToFullSector();
        } finally {
            try {
                this.file.force(true);
            } finally {
                this.file.close();
            }
        }
    }

    public void flush() throws IOException {
        this.file.force(true);
    }

    private void padOrTruncateToFullSector() throws IOException {
        // how many sectors of data are we using?
        final int bytesNeededForFile = this.usedSectors.length() * this.sectorSize;
        final int currentFileSize = (int) this.file.size();

        if (currentFileSize > bytesNeededForFile) {
            this.file.truncate(bytesNeededForFile);
        } else {
            final int desiredSize = bytesNeededForFile;

            if (currentFileSize < desiredSize) {
                final ByteBuffer byteBuffer = PADDING_BUFFER.duplicate();
                byteBuffer.position(0);
                this.file.write(byteBuffer, desiredSize - 1);
            }
        }
    }

    class SectorSpanDataBuffer extends ByteArrayOutputStream {
        private final int subLevelIndex;

        public SectorSpanDataBuffer(final int subLevelIndex) {
            super(SubLevelStorageFile.this.sectorSize);
            super.write(0);
            super.write(0);
            super.write(0);
            super.write(0);
            super.write(0);
            this.subLevelIndex = subLevelIndex;
        }

        public void close() throws IOException {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(this.buf, 0, this.count);

            final int start = this.count - 4;
            byteBuffer.putInt(0, start);
            SubLevelStorageFile.this.write(this.subLevelIndex, byteBuffer);
        }
    }
}
