package com.example.examplemod.Blocks.VSCodeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class VSCodeBlock extends Block implements EntityBlock {
    public VSCodeBlock() {
        super(Properties.of(Material.STONE).noOcclusion().destroyTime(10));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new VSCodeBlockEntity(pPos, pState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return EntityBlock.super.getTicker(pLevel, pState, pBlockEntityType);
    }

    @Override
    public @Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity) {
        return EntityBlock.super.getListener(pLevel, pBlockEntity);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide) {
            return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
        }
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (!(blockEntity instanceof VSCodeBlockEntity)) {
            return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
        }
        MenuProvider containerProvider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return new TextComponent("Input File");
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
                return new VSCodeBlockContainer(pContainerId, pPos, pInventory, pPlayer);
            }
        };

        NetworkHooks.openGui((ServerPlayer) pPlayer, containerProvider, blockEntity.getBlockPos());
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }
}
