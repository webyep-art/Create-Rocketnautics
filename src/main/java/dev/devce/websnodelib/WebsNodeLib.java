package dev.devce.websnodelib;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class WebsNodeLib {
    public static final String MOD_ID = "websnodelib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public WebsNodeLib(net.neoforged.bus.api.IEventBus modBus) {
        LOGGER.info("Web's Node Lib initialized!");
        dev.devce.websnodelib.internal.InternalNodes.register();
        
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onClientCommands);
    }

    private void onClientCommands(net.neoforged.neoforge.client.event.RegisterClientCommandsEvent event) {
        dev.devce.websnodelib.internal.WebsNodeCommands.register(event.getDispatcher());
    }
}
