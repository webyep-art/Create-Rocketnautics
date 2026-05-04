package dev.devce.rocketnautics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.devce.rocketnautics.content.items.JetpackItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class JetpackLayer<T extends AbstractClientPlayer, M extends PlayerModel<T>> extends RenderLayer<T, M> {
    public JetpackLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof JetpackItem)) return;

        poseStack.pushPose();
        
        
        this.getParentModel().body.translateAndRotate(poseStack);
        
        
        poseStack.translate(0.0, 0.4, 0.25);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.8F, 0.8F, 0.8F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
            chestStack,
            ItemDisplayContext.FIXED,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            player.level(),
            player.getId()
        );

        poseStack.popPose();
    }
}
