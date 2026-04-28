package dev.devce.rocketnautics.content.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Handles applying force to moving contraptions in the Sable physics engine.
 * This behavior is attached to thrusters when they are part of a Create contraption.
 */
public class RocketThrusterActorBehaviour {
    
    // Constant for the default thrust magnitude
    private static final double DEFAULT_THRUST_MAGNITUDE = 50.0;
    
    private final AbstractThrusterBlockEntity thruster;

    public RocketThrusterActorBehaviour(AbstractThrusterBlockEntity thruster) {
        this.thruster = thruster;
    }

    /**
     * Ticks the actor behavior, applying force to the contraption's force group.
     * 
     * @param forceGroup The Sable ForceGroup to apply force to.
     * @param contraptionWorldPos The world position of the thruster on the contraption.
     * @param orientation The orientation of the contraption.
     */
    public void tick(Object forceGroup, Vec3 contraptionWorldPos, Vec3 orientation) {
        if (!thruster.isActive()) {
            return;
        }

        // Apply thrust in the opposite direction of the nozzle
        Direction facing = thruster.getThrustDirection();
        Direction pushDirection = facing.getOpposite();
        
        // Calculate thrust vector based on current engine power
        double thrustMagnitude = thruster.getCurrentPower() > 0 ? 
                thruster.getCurrentPower() * 10.0 : DEFAULT_THRUST_MAGNITUDE;
        
        Vec3 thrustVector = new Vec3(
                pushDirection.getStepX() * thrustMagnitude,
                pushDirection.getStepY() * thrustMagnitude,
                pushDirection.getStepZ() * thrustMagnitude
        );

        // Note: In a complete implementation, this would interact with the Sable/Aeronautics API
        // applyForce(forceGroup, thrustVector, contraptionWorldPos);
    }
}
