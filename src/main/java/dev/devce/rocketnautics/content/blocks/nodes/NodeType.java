package dev.devce.rocketnautics.content.blocks.nodes;

public enum NodeType {
    NUMBER_INPUT("Number"),
    ALTITUDE("Altitude"),
    VELOCITY("Velocity"),
    PITCH("Pitch"),
    YAW("Yaw"),
    ROLL("Roll"),
    POS_X("X Position"),
    POS_Y("Y Position"),
    POS_Z("Z Position"),
    THRUST_GET("Get Thrust"),
    THRUST_SET("Set Thrust"),
    GIMBAL_SET("Set Gimbal"),
    ENGINE_ID("Engine ID"),
    PERIPHERAL_LIST("Peripheral List"),
    COMPARE("Compare"),
    LOGIC("Logic"),
    MATH("Math"),
    ADVANCED("Advanced"),
    MEMORY("Memory"),
    LINK_INPUT("Link Input"),
    LINK_OUTPUT("Link Output"),
    COMMENT("Comment"),
    OUTPUT("Redstone Output");

    private final String displayName;

    NodeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
