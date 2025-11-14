package com.example.examplemod.Blocks.WifiRouterBlock;

import com.example.examplemod.Blocks.CableBlock.CableBlockEntity;
import com.example.examplemod.Blocks.DNSServerBlock.DNSServerBlockEntity;
import com.example.examplemod.Networking.Graph;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WifiRouterBlock extends Block implements EntityBlock {
    public WifiRouterBlock() {
        super(Properties.of(Material.STONE));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new WifiRouterBlockEntity(pPos, pState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide) return null;

        return (level, pos, state, te) -> {
            if (te instanceof WifiRouterBlockEntity blockEntity) blockEntity.tickServer(level);
        };
    }

    @Override
    public @Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity) {
        return EntityBlock.super.getListener(pLevel, pBlockEntity);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        WifiRouterBlockEntity be = (WifiRouterBlockEntity) pLevel.getBlockEntity(pPos);
        if (be == null) return InteractionResult.FAIL;
        be.loadDNSServers();

        System.out.println("📶 WifiRouterBlock used! Current Router Map:");
        Graph routerMap = be.getRouterMap();
        routerMap.visualizeNetwork(pLevel, Blocks.AMETHYST_BLOCK);

        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }


    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (pLevel.isClientSide()) return;
        final WifiRouterBlockEntity be = (WifiRouterBlockEntity) pLevel.getBlockEntity(pPos);

        if (be == null) {
            System.err.println("⚠️ WifiRouterBlockEntity is null at position: " + pPos);
            return;
        }

        if (pLevel.getBlockEntity(pPos.above()) instanceof DNSServerBlockEntity) {
            System.out.println("🔗 Connected WifiRouter at " + pPos + " to DNS Server above it.");
        }

        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    }
}
