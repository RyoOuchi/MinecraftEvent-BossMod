package com.example.examplemod.Blocks.CableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.Nullable;

public class CableBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public CableBlock() {
        super(Properties.of(Material.STONE).noOcclusion().lightLevel(light -> 7));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
        boolean connected = canConnectTo(pNeighborState);
        return pState.setValue(getPropertyFor(pDirection), connected);
    }

    private boolean canConnectTo(BlockState neighborState) {
        return neighborState.getBlock() instanceof CableBlock;
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        updateConnections(pLevel, pPos, pState);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        updateConnections(level, pos, state);
    }

    private void updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            boolean connected = canConnectTo(neighborState);
            state = state.setValue(getPropertyFor(dir), connected);
        }
        level.setBlock(pos, state, 3);
    }

    private BooleanProperty getPropertyFor(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getNearestLookingDirection().getOpposite();
        BlockState state = this.defaultBlockState();

        for (Direction d : Direction.values()) {
            state = state.setValue(getPropertyFor(d), d == dir);
        }

        return state;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new CableBlockEntity(pPos, pState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide) return null;

        return (level, pos, state, te) -> {
            if (te instanceof CableBlockEntity blockEntity) blockEntity.tickServer(level, pos);
        };
    }

    @Override
    public @Nullable <T extends BlockEntity> GameEventListener getListener(Level pLevel, T pBlockEntity) {
        return EntityBlock.super.getListener(pLevel, pBlockEntity);
    }
}

