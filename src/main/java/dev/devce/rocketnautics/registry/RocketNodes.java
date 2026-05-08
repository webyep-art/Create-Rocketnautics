package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.api.nodes.NodeHandler;
import dev.devce.rocketnautics.api.nodes.NodeRegistry;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.SensorNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.ConstantNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.MathNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.ThrusterNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.VectorNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.BoosterNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.RCSNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.GraphNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.AttitudeNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.LinkedInputNodeHandler;
import dev.devce.rocketnautics.content.blocks.nodes.handlers.LinkedOutputNodeHandler;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RocketNodes {
    public static final DeferredHolder<NodeHandler, SensorNodeHandler> ALTITUDE = NodeRegistry.NODE_HANDLERS.register("altitude",
        () -> new SensorNodeHandler("Altitude", "Returns current ship altitude in blocks relative to sea level (y=64).", context -> context.getAltitude(), 0xFF00AAFF));
    
    public static final DeferredHolder<NodeHandler, SensorNodeHandler> VELOCITY = NodeRegistry.NODE_HANDLERS.register("velocity",
        () -> new SensorNodeHandler("Velocity", "Returns current ship vertical velocity (m/s). Positive is UP.", context -> context.getVelocity(), 0xFF00AAFF));

    public static final DeferredHolder<NodeHandler, SensorNodeHandler> PITCH = NodeRegistry.NODE_HANDLERS.register("pitch",
        () -> new SensorNodeHandler("Pitch", "Returns ship rotation on X axis (Degrees).", context -> context.getPitch(), 0xFF00AAFF));

    public static final DeferredHolder<NodeHandler, SensorNodeHandler> YAW = NodeRegistry.NODE_HANDLERS.register("yaw",
        () -> new SensorNodeHandler("Yaw", "Returns ship rotation on Y axis (Degrees).", context -> context.getYaw(), 0xFF00AAFF));

    public static final DeferredHolder<NodeHandler, SensorNodeHandler> ROLL = NodeRegistry.NODE_HANDLERS.register("roll",
        () -> new SensorNodeHandler("Roll", "Returns ship rotation on Z axis (Degrees).", context -> context.getRoll(), 0xFF00AAFF));

    public static final DeferredHolder<NodeHandler, ConstantNodeHandler> CONSTANT = NodeRegistry.NODE_HANDLERS.register("constant",
        ConstantNodeHandler::new);

    public static final DeferredHolder<NodeHandler, MathNodeHandler> MATH = NodeRegistry.NODE_HANDLERS.register("math",
        MathNodeHandler::new);
    
    // Actuators (Peripherals)
    public static final DeferredHolder<NodeHandler, ThrusterNodeHandler> THRUSTER = NodeRegistry.NODE_HANDLERS.register("thruster",
        ThrusterNodeHandler::new);

    public static final DeferredHolder<NodeHandler, BoosterNodeHandler> BOOSTER = NodeRegistry.NODE_HANDLERS.register("booster",
        BoosterNodeHandler::new);

    public static final DeferredHolder<NodeHandler, VectorNodeHandler> VECTOR_CONTROL = NodeRegistry.NODE_HANDLERS.register("vector_control",
        VectorNodeHandler::new);

    public static final DeferredHolder<NodeHandler, RCSNodeHandler> RCS_CONTROL = NodeRegistry.NODE_HANDLERS.register("rcs_control",
        RCSNodeHandler::new);

    // Wireless
    public static final DeferredHolder<NodeHandler, LinkedInputNodeHandler> LINK_INPUT = NodeRegistry.NODE_HANDLERS.register("link_input",
        LinkedInputNodeHandler::new);

    public static final DeferredHolder<NodeHandler, LinkedOutputNodeHandler> LINK_OUTPUT = NodeRegistry.NODE_HANDLERS.register("link_output",
        LinkedOutputNodeHandler::new);

    // Utility
    public static final DeferredHolder<NodeHandler, GraphNodeHandler> OSCILLOSCOPE = NodeRegistry.NODE_HANDLERS.register("oscilloscope",
        GraphNodeHandler::new);

    public static final DeferredHolder<NodeHandler, AttitudeNodeHandler> ATTITUDE = NodeRegistry.NODE_HANDLERS.register("attitude",
        AttitudeNodeHandler::new);
}
