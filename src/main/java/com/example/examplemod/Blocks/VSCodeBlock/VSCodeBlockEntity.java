package com.example.examplemod.Blocks.VSCodeBlock;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.Items.CodeFileItem.CodeFileItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VSCodeBlockEntity extends BlockEntity {

    private final ItemStackHandler itemStackHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof CodeFileItem;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (!(stack.getItem() instanceof CodeFileItem)) {
                return stack;
            }

            return super.insertItem(slot, stack, simulate);
        }
    };

    private final LazyOptional<IItemHandler> itemHander = LazyOptional.of(() -> itemStackHandler);


    public VSCodeBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ExampleMod.VSCODE_BLOCK_ENTITY, pWorldPosition, pBlockState);
    }

    public ItemStack getItemInSlot() {
        return itemStackHandler.getStackInSlot(0);
    }

    @Override
    public void load(CompoundTag pTag) {
        if (pTag.contains("Inventory")) {
            itemStackHandler.deserializeNBT(pTag.getCompound("Inventory"));
        }
        super.load(pTag);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("Inventory", itemStackHandler.serializeNBT());
        super.saveAdditional(pTag);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return itemHander.cast();
        }
        return super.getCapability(capability, side);
    }
}
