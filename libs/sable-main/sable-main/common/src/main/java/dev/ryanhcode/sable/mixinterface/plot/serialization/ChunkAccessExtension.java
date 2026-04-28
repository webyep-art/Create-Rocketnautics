package dev.ryanhcode.sable.mixinterface.plot.serialization;

import java.util.Collection;

public interface ChunkAccessExtension {

    /**
     * Sets the file names of the sub-levels that this chunk holder contains.
     */
    void sable$setContainingSubLevels(final Collection<String> subLevels);

    /**
     * @return the file names of the sub-levels that this chunk holder contains
     */
    Collection<String> sable$getContainingSubLevels();

}
