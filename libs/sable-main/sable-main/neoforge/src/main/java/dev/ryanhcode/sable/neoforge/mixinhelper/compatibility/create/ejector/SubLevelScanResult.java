package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.ejector;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.BlockHitResult;

public record SubLevelScanResult(BlockHitResult result, ServerSubLevel serverSubLevel) {
}
