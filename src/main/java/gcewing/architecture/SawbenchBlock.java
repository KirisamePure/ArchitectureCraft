package gcewing.architecture;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class SawbenchBlock extends BaseEntityBlock {
  public static final MapCodec<SawbenchBlock> CODEC = simpleCodec(SawbenchBlock::new);
  public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

  public SawbenchBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  @Override
  public MapCodec<SawbenchBlock> codec() {
    return CODEC;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new SawbenchBlockEntity(pos, state);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (player.isShiftKeyDown()) return InteractionResult.PASS;
    if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SawbenchBlockEntity be)
      player.openMenu(be);
    return InteractionResult.SUCCESS;
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return ShapeGeometry.voxel(
        gcewing.architecture.shape.Mesh.model("block/sawbench")
            .transformed(
                new gcewing.architecture.shape.Orientation(
                    1, Math.floorMod(state.getValue(FACING).get2DDataValue() + 2, 4), 0))
            .boxes());
  }

  @Override
  protected void affectNeighborsAfterRemoval(
      BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
    Containers.updateNeighboursAfterDestroy(state, level, pos);
  }
}
