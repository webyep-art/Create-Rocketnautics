package dev.devce.rocketnautics.content.screens;

import dev.devce.rocketnautics.content.blocks.AstralEngineeringTableBlockEntity;
import dev.devce.rocketnautics.registry.RocketBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class AstralEngineeringTableMenu extends AbstractContainerMenu {

    private final AstralEngineeringTableBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final BlockPos blockPos;

    public AstralEngineeringTableMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level(), extraData.readBlockPos());
    }

    public AstralEngineeringTableMenu(int containerId, Inventory inv, Level level, BlockPos pos) {
        this(containerId, inv, level, pos, level.getBlockEntity(pos), new SimpleContainerData(2));
    }

    public AstralEngineeringTableMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        this(containerId, inv, inv.player.level(), entity.getBlockPos(), entity, data);
    }

    private AstralEngineeringTableMenu(int containerId, Inventory inv, Level level, BlockPos pos, @Nullable BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ASTRAL_ENGINEERING_TABLE_MENU.get(), containerId);
        this.level = level;
        this.blockPos = pos;
        this.data = data;

        if (!(entity instanceof AstralEngineeringTableBlockEntity table)) {
            throw new IllegalStateException("Astral Engineering Table menu opened without a valid block entity at " + pos);
        }

        this.blockEntity = table;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, 0, 54, 34));   // input left
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, 1, 80, 34));   // input middle
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, 2, 104, 34) {  // output right
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                blockEntity.consumeRecipeInputs();
            }
        });

        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        int vanillaSlotCount = 36;

        if (index < vanillaSlotCount) {
            if (!moveItemStackTo(sourceStack, vanillaSlotCount, vanillaSlotCount + 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < vanillaSlotCount + 3) {
            if (!moveItemStackTo(sourceStack, 0, vanillaSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockPos),
                player, RocketBlocks.ASTRAL_ENGINEERING_TABLE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}