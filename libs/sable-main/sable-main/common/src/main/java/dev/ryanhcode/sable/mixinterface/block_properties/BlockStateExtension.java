package dev.ryanhcode.sable.mixinterface.block_properties;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinition;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public interface BlockStateExtension {
    void sable$loadProperties(final StateDefinition<Block, BlockState> stateDefinition,  PhysicsBlockPropertiesDefinition definition);

    <T> T sable$getProperty(PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<T> type);
}
