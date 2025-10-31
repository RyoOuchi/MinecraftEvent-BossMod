package com.example.examplemod.Blocks.DNSServerBlock;

import com.example.examplemod.Blocks.ServerBlock.ServerBlockEntity;
import com.example.examplemod.Blocks.WifiRouterBlock.WifiRouterBlockEntity;
import com.example.examplemod.ServerData.DnsServerSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.Nullable;

public class DNSServerBlock extends Block implements EntityBlock {
    public DNSServerBlock() {
        super(Properties.of(Material.STONE));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new DNSServerBlockEntity(pPos, pState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide) return null;

        return (level, pos, state, te) -> {
            if (te instanceof DNSServerBlockEntity blockEntity) blockEntity.tickServer();
        };
    }

    @Override
    public @Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity) {
        return EntityBlock.super.getListener(pLevel, pBlockEntity);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
        if (!pLevel.isClientSide() && pLevel instanceof ServerLevel serverLevel) {
            DnsServerSavedData data = DnsServerSavedData.get(serverLevel);
            data.addDnsServer(pPos);
            System.out.println("💾 Added DNS Server at: " + pPos);
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        if (!pLevel.isClientSide() && pLevel instanceof ServerLevel serverLevel) {
            DnsServerSavedData data = DnsServerSavedData.get(serverLevel);
            data.removeDnsServer(pPos);
            System.out.println("🗑️ Removed DNS Server from data: " + pPos);

        }
    }
}
