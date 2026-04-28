package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.stock_ticker;

import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StockTickerInteractionHandler.class)
public class StockTickerInteractionHandlerMixin {

    @Redirect(method = "getStockTickerPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos sable$getStockTickerPosition(final Entity instance) {
        final Entity vehicle = instance.getRootVehicle();

        if (Sable.HELPER.getContaining(vehicle) != null) {
            return vehicle.blockPosition();
        }

        return instance.blockPosition();

    }
}
