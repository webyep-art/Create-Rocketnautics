package dev.devce.rocketnautics.content.blocks;

import dev.devce.rocketnautics.content.screens.AstralEngineeringTableMenu;
import dev.devce.rocketnautics.registry.RocketBlockEntities;
import dev.devce.rocketnautics.registry.RocketItems;
import dev.devce.rocketnautics.util.JetpackData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class AstralEngineeringTableBlockEntity extends BlockEntity implements MenuProvider {

    public final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static final int INPUT_SLOT1 = 0;
    private static final int INPUT_SLOT2 = 1;
    private static final int OUTPUT_SLOT = 2;

    protected final ContainerData data;

    public AstralEngineeringTableBlockEntity(BlockPos pos, BlockState state) {
        super(RocketBlockEntities.ASTRAL_ENGINEERING_TABLE_BLOCK_ENTITY.get(), pos, state);

        this.data = new ContainerData() {
            public int get(int i) { return 0; }
            public void set(int i, int v) {}
            public int getCount() { return 0; }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        ItemStack result = getRecipeResult();
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (!result.isEmpty()) {
            if (output.isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, result);
                setChanged(level, pos, state);
            }
        }
    }

    private ItemStack getRecipeResult() {
        ItemStack slot1 = itemHandler.getStackInSlot(INPUT_SLOT1);
        ItemStack slot2 = itemHandler.getStackInSlot(INPUT_SLOT2);

        if (slot1.is(RocketItems.SPACE_CHESTPLATE.get()) &&
                slot2.is(RocketItems.JETPACK_UPGRADE.get())) {
            return createJetpackChestplate(slot1);
        }

        if (slot2.is(RocketItems.SPACE_CHESTPLATE.get()) &&
                slot1.is(RocketItems.JETPACK_UPGRADE.get())) {
            return createJetpackChestplate(slot2);
        }

        return ItemStack.EMPTY;
    }

    private ItemStack createJetpackChestplate(ItemStack input) {
        ItemStack result = input.copy();
        result.setCount(1);

        JetpackData.enable(result);

        return result;
    }

    public void consumeRecipeInputs() {
        if (!level.isClientSide && !getRecipeResult().isEmpty()) {
            itemHandler.extractItem(INPUT_SLOT1, 1, false);
            itemHandler.extractItem(INPUT_SLOT2, 1, false);
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rocketnautics.astral_engineering_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AstralEngineeringTableMenu(id, inv, this, this.data);
    }

    // =========================
    // INVENTORY DROP
    // =========================
    public void drops() {
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    // =========================
    // NBT SAVE/LOAD
    // =========================
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("inventory", itemHandler.serializeNBT(provider));
        super.saveAdditional(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        itemHandler.deserializeNBT(provider, tag.getCompound("inventory"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}