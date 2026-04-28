package dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

public interface SchematicLevelExtension {
    List<SchematicSubLevel> sable$getSubLevels();

    record SchematicSubLevel(UUID uuid, Vector3d position, Quaterniond orientation, SchematicLevel level) { }
}
