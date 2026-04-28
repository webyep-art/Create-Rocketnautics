package dev.ryanhcode.sable.api.physics.force;

import dev.ryanhcode.sable.Sable;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

/**
 * All default force groups
 */
public class ForceGroups {
    public static final ResourceKey<Registry<ForceGroup>> REGISTRY_KEY = ResourceKey.createRegistryKey(Sable.sablePath("force_groups"));
    private static final RegistrationProvider<ForceGroup> VANILLA_PROVIDER;
    public static final Registry<ForceGroup> REGISTRY;

    static {
        VANILLA_PROVIDER = RegistrationProvider.get(REGISTRY_KEY, Sable.MOD_ID);
        REGISTRY = VANILLA_PROVIDER.asVanillaRegistry();
    }

    public static final RegistryObject<ForceGroup> GRAVITY = VANILLA_PROVIDER.register(Sable.sablePath("gravity"), () -> new ForceGroup(Component.translatable("force_group.sable.gravity"), null, 0x216e55, false));
    public static final RegistryObject<ForceGroup> DRAG = VANILLA_PROVIDER.register(Sable.sablePath("drag"), () -> new ForceGroup(Component.translatable("force_group.sable.drag"), null, 0x834f31, false));
    public static final RegistryObject<ForceGroup> LEVITATION = VANILLA_PROVIDER.register(Sable.sablePath("levitation"), () -> new ForceGroup(Component.translatable("force_group.sable.levitation"), null, 0x734480, true));
    public static final RegistryObject<ForceGroup> BALLOON_LIFT = VANILLA_PROVIDER.register(Sable.sablePath("balloon_lift"), () -> new ForceGroup(Component.translatable("force_group.sable.balloon_lift"), null, 0xd2643e, true));
    public static final RegistryObject<ForceGroup> PROPULSION = VANILLA_PROVIDER.register(Sable.sablePath("propulsion"), () -> new ForceGroup(Component.translatable("force_group.sable.propulsion"), null, 0x5a7c9f, true));
    public static final RegistryObject<ForceGroup> LIFT = VANILLA_PROVIDER.register(Sable.sablePath("lift"), () -> new ForceGroup(Component.translatable("force_group.sable.lift"), null, 0x8cb6c6, true));
    public static final RegistryObject<ForceGroup> MAGNETIC_FORCE = VANILLA_PROVIDER.register(Sable.sablePath("magnetic_force"), () -> new ForceGroup(Component.translatable("force_group.sable.magnetic_force"), null, 0xe05343, false));

    public static void register() {
        // no-op
    }

    /**
     *
     * The count of registered force groups
     */
    public static int count() {
        return REGISTRY.size();
    }
}
