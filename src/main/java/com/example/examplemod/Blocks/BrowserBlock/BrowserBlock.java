package com.example.examplemod.Blocks.BrowserBlock;

import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;
import com.example.examplemod.Screens.BrowserScreen.BrowserSearchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;

public class BrowserBlock extends Block implements EntityBlock {
    public BrowserBlock() {
        super(Properties.of(Material.STONE));
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        BrowserBlockEntity be = (BrowserBlockEntity) pLevel.getBlockEntity(pPos);

        if (pLevel.isClientSide) {
            Minecraft.getInstance().setScreen(new BrowserSearchScreen(be));
        }

        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BrowserBlockEntity(pPos, pState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide) return null;

        return (level, pos, state, te) -> {
            if (te instanceof BrowserBlockEntity blockEntity) blockEntity.tickServer();
        };
    }

    @Override
    public @Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity) {
        return EntityBlock.super.getListener(pLevel, pBlockEntity);
    }
}
