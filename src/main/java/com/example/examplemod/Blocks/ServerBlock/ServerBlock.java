package com.example.examplemod.Blocks.ServerBlock;

import com.example.examplemod.Screens.ServerDomainScreen.ServerDomainScreen;
import com.example.examplemod.ServerData.ServerSavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

import java.util.Map;

public class ServerBlock extends Block implements EntityBlock {
    public ServerBlock() {
        super(Properties.of(Material.STONE));
    }


    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        if (!pLevel.isClientSide() && pLevel instanceof ServerLevel serverLevel) {
            ServerSavedData data = ServerSavedData.get(serverLevel);
            final Map<String, BlockPos> servers = data.getServers();

            servers.forEach((serverName, serverPos) -> {
                System.out.println("🖥️ Server Name: " + serverName + " at Position: " + serverPos);
            });

            if (data.removeServer(pPos)) {
                System.out.println("💾 Removed Server at: " + pPos);
            } else {
                System.out.println("⚠️ No server init at: " + pPos);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide) {
            Minecraft.getInstance().setScreen(new ServerDomainScreen(pPos));

        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ServerBlockEntity(pPos, pState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide) return null;

        return (level, pos, state, te) -> {
            if (te instanceof ServerBlockEntity blockEntity) blockEntity.tickServer();
        };
    }

    @Override
    public @Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity) {
        return EntityBlock.super.getListener(pLevel, pBlockEntity);
    }
}
