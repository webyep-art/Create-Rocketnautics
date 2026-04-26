package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class RocketPartials {

    public static final ModelResourceLocation VECTOR_THRUSTER_NOZZLE_MODEL = 
        new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "block/vector_thruster_nozzle"), "standalone");
    
    public static BakedModel vectorThrusterNozzle;

    public static void init() {
        // init class
    }
}
