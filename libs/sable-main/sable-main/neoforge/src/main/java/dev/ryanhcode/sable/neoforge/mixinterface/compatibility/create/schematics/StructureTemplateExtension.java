package dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

public interface StructureTemplateExtension {

    List<SubLevelTemplate> sable$getSubLevels();

    record SubLevelTemplate(UUID uuid, Vector3d position, Quaterniond orientation, StructureTemplate template) {}
}
