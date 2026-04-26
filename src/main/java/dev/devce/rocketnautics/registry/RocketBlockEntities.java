package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.blocks.BoosterThrusterBlockEntity;
import dev.devce.rocketnautics.content.blocks.RCSThrusterBlockEntity;
import dev.devce.rocketnautics.content.blocks.RocketThrusterBlockEntity;
import dev.devce.rocketnautics.content.blocks.VectorThrusterBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RocketBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RocketNautics.MODID);

    public static final Supplier<BlockEntityType<RocketThrusterBlockEntity>> ROCKET_THRUSTER = 
            BLOCK_ENTITIES.register("rocket_thruster", 
                    () -> BlockEntityType.Builder.of(RocketThrusterBlockEntity::new, RocketBlocks.ROCKET_THRUSTER.get()).build(null));

    public static final Supplier<BlockEntityType<BoosterThrusterBlockEntity>> BOOSTER_THRUSTER = 
            BLOCK_ENTITIES.register("booster_thruster", 
                    () -> BlockEntityType.Builder.of(BoosterThrusterBlockEntity::new, RocketBlocks.BOOSTER_THRUSTER.get()).build(null));

    public static final Supplier<BlockEntityType<VectorThrusterBlockEntity>> VECTOR_THRUSTER = 
            BLOCK_ENTITIES.register("vector_thruster", 
                    () -> BlockEntityType.Builder.of(VectorThrusterBlockEntity::new, RocketBlocks.VECTOR_THRUSTER.get()).build(null));

    public static final Supplier<BlockEntityType<RCSThrusterBlockEntity>> RCS_THRUSTER =
            BLOCK_ENTITIES.register("rcs_thruster", 
                    () -> BlockEntityType.Builder.of(RCSThrusterBlockEntity::new, RocketBlocks.RCS_THRUSTER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(RocketBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ROCKET_THRUSTER.get(), (be, side) -> {
            if (side == null || side == be.getThrustDirection().getOpposite()) {
                return be.fuelTank;
            }
            return null;
        });
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, VECTOR_THRUSTER.get(), (be, side) -> {
            if (side == null || side == be.getThrustDirection().getOpposite()) {
                return be.fuelTank;
            }
            return null;
        });
    }
}
