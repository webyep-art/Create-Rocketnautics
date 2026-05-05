package dev.devce.rocketnautics.content.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class RocketThrusterActorBehaviour {
    
    
    private final RocketThrusterBlockEntity tileEntity;

    public RocketThrusterActorBehaviour(RocketThrusterBlockEntity te) {
        this.tileEntity = te;
    }

    public void tick(Object forceGroup, Vec3 worldPosition, Vec3 orientation) {
        if (!tileEntity.isActive()) return;

        
        Direction facing = tileEntity.getThrustDirection();
        
        
        double thrustMagnitude = 50.0; 
        Vec3 thrustVector = Vec3.atLowerCornerOf(facing.getNormal()).scale(thrustMagnitude);

        
        
        
        
    }
}
