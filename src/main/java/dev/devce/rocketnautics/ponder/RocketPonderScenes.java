package dev.devce.rocketnautics.ponder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.foundation.element.InputWindowElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import dev.devce.rocketnautics.content.blocks.RocketThrusterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class RocketPonderScenes {

    public static void thrusterIntro(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("rocket_thruster", "Rocket Thruster: Power and Fueling"); 
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos thrusterPos = util.grid().at(2, 1, 2);
        BlockPos tankPos = util.grid().at(2, 1, 1); 

        
        scene.world().showSection(util.select().everywhere(), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(60)
            .text("rocketnautics.ponder.rocket_thruster.text_1")
            .pointAt(util.vector().topOf(thrusterPos))
            .placeNearTarget();
        scene.idle(70);

        
        scene.overlay().showText(70)
            .text("rocketnautics.ponder.rocket_thruster.text_5") 
            .pointAt(util.vector().topOf(tankPos))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
            .text("rocketnautics.ponder.rocket_thruster.text_6") 
            .pointAt(util.vector().centerOf(tankPos))
            .placeNearTarget();
        scene.idle(80);

        
        scene.overlay().showControls(util.vector().topOf(thrusterPos), Pointing.DOWN, 40).rightClick();
        scene.idle(10);
        scene.overlay().showText(60)
            .text("rocketnautics.ponder.rocket_thruster.text_2")
            .pointAt(util.vector().topOf(thrusterPos))
            .placeNearTarget();
        scene.idle(70);

        
        
        scene.effects().emitParticles(util.vector().centerOf(thrusterPos).add(0, -0.7, 0), scene.effects().particleEmitterWithinBlockSpace(net.minecraft.core.particles.ParticleTypes.FLAME, new Vec3(0, -1, 0)), 10, 5);
        
        scene.overlay().showText(60)
            .text("rocketnautics.ponder.rocket_thruster.text_3")
            .pointAt(util.vector().centerOf(thrusterPos))
            .placeNearTarget();
        scene.idle(80);

        
        scene.overlay().showText(80)
            .colored(net.createmod.ponder.api.PonderPalette.RED)
            .text("rocketnautics.ponder.rocket_thruster.text_4")
            .pointAt(util.vector().blockSurface(thrusterPos, Direction.DOWN))
            .placeNearTarget();
        scene.idle(90);
    }
}
