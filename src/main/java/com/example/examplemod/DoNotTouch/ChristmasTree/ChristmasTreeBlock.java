package com.example.examplemod.DoNotTouch.ChristmasTree;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class ChristmasTreeBlock extends Block {
    public ChristmasTreeBlock() {
        super(Properties.of(Material.STONE));
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pLevel.isClientSide) {
            ChristmasTree christmasTree = new ChristmasTree(ExampleMod.CHRISTMAS_TREE_ENTITY, pLevel);
            christmasTree.setPos(pPos.getX() + 0.5, pPos.getY(), pPos.getZ() + 0.5);
            pLevel.addFreshEntity(christmasTree);
        }
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    }
}
