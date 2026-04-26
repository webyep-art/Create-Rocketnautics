package dev.devce.rocketnautics;

import dev.devce.rocketnautics.content.blocks.VectorThrusterRenderer;
import dev.devce.rocketnautics.content.particles.RocketExhaustParticle;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketParticles;
import dev.devce.rocketnautics.registry.RocketPartials;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = RocketNautics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RocketNauticsClient {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RocketParticles.PLASMA.get(), RocketExhaustParticle.FlameProvider::new);
        event.registerSpriteSet(RocketParticles.PLUME.get(), RocketExhaustParticle.FlameProvider::new);
        event.registerSpriteSet(RocketParticles.JET_SMOKE.get(), RocketExhaustParticle.SmokeProvider::new);
        event.registerSpriteSet(RocketParticles.BLUE_FLAME.get(), RocketExhaustParticle.FlameProvider::new);
        event.registerSpriteSet(RocketParticles.RCS_GAS.get(), RocketExhaustParticle.RCSGasProvider::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RocketPartials.init();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RocketBlockEntities.VECTOR_THRUSTER.get(), VectorThrusterRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(RocketPartials.VECTOR_THRUSTER_NOZZLE_MODEL);
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.BakingCompleted event) {
        RocketPartials.vectorThrusterNozzle = event.getModels().get(RocketPartials.VECTOR_THRUSTER_NOZZLE_MODEL);
    }
}
