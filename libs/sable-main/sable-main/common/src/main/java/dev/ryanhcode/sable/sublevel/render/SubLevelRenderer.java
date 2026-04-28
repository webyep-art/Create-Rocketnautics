package dev.ryanhcode.sable.sublevel.render;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.ReachAroundSubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import foundry.veil.api.compat.SodiumCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Renders sub-levels in the world.
 */
public final class SubLevelRenderer {

    public static final SelectedRenderer DEFAULT;

    static {
        SelectedRenderer impl = null;
        for (final SelectedRenderer render : SelectedRenderer.values()) {
            if (render.isSupported()) {
                impl = render;
                break;
            }
        }

        if (impl == null) {
            throw new RuntimeException("Failed to find a supported sub-level renderer");
        }

        DEFAULT = impl;
    }

    private static SubLevelRenderDispatcher dispatcher;
    private static SelectedRenderer selected = DEFAULT;

    public static void setImpl(final SelectedRenderer impl) {
        final SelectedRenderer newImpl = !impl.isSupported() ? DEFAULT : impl;
        if (selected.equals(newImpl)) {
            return;
        }

        selected = newImpl;

        if (dispatcher != null) {
            dispatcher.free();
            dispatcher = null;

            final ClientLevel level = Minecraft.getInstance().level;

            if (level != null) {
                final Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer) ((SubLevelContainerHolder) level).sable$getPlotContainer()).getAllSubLevels();

                for (final ClientSubLevel sublevel : sublevels) {
                    sublevel.updateRenderData();
                }
            }
        }
    }

    public static void free() {
        if (dispatcher != null) {
            dispatcher.free();
            dispatcher = null;
        }
    }

    public static SubLevelRenderDispatcher getDispatcher() {
        if (dispatcher == null) {
            dispatcher = selected.create();
        }
        return dispatcher;
    }

    public enum SelectedRenderer {
        VANILLA {
            @Override
            public boolean isSupported() {
                return !SodiumCompat.isLoaded();
            }

            @Override
            public SubLevelRenderDispatcher create() {
                return new VanillaSubLevelRenderDispatcher();
            }
        },
        SODIUM_REACHAROUND {
            @Override
            public boolean isSupported() {
                return SodiumCompat.isLoaded();
            }

            @Override
            public SubLevelRenderDispatcher create() {
                return new ReachAroundSubLevelRenderDispatcher();
            }
        };

        public abstract boolean isSupported();

        public abstract SubLevelRenderDispatcher create();
    }
}
