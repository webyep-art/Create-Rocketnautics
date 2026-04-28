package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.nozzle;

import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NozzleBlockEntity.class)
public interface NozzleBlockEntityAccessor {

    @Accessor
    float getRange();

}
