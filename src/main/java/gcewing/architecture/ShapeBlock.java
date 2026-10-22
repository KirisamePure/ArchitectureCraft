package gcewing.architecture;

import com.mojang.serialization.MapCodec;
import gcewing.architecture.shape.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class ShapeBlock extends BaseEntityBlock {
  public static final MapCodec<ShapeBlock> CODEC = simpleCodec(ShapeBlock::new);
  public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
  public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 3);
  public static final BooleanProperty GLOWING = BooleanProperty.create("glowing");

  public ShapeBlock(Properties properties) {
    super(properties);
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(FACING, Direction.UP)
            .setValue(TURN, 0)
            .setValue(GLOWING, false));
  }

  @Override
  public MapCodec<ShapeBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, TURN, GLOWING);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new ShapeBlockEntity(pos, state);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return ShapePlacement.plan(context).state();
  }

  private static ShapeData data(BlockGetter world, BlockPos pos) {
    return world.getBlockEntity(pos) instanceof ShapeBlockEntity be
        ? be.data()
        : ShapeData.defaults();
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return ShapeGeometry.collision(data(world, pos), state, world, pos, true);
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return ShapeGeometry.collision(data(world, pos), state, world, pos, false);
  }

  @Override
  protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
    ShapeData data = data(world, pos);
    if (data.shape().occlusionMask == 0 || (data.shape().occlusionMask & 0xff00) != 0)
      return Shapes.empty();
    return ShapeGeometry.voxel(
        new Mesh(List.of(), CollisionMask.boxes(data.shape()))
            .transformed(ShapeGeometry.orientation(state, data.withOffset(0)))
            .boxes());
  }

  @Override
  protected float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
    BlockState base = data(world, pos).base();
    return base.is(this) ? 1 : base.getShadeBrightness(world, pos);
  }

  @Override
  protected float getDestroyProgress(
      BlockState state, Player player, BlockGetter world, BlockPos pos) {
    BlockState base = data(world, pos).base();
    return base.is(this) ? 0 : base.getDestroyProgress(player, world, pos);
  }

  private static ItemStack shapeItem(ShapeData data, BlockState state, boolean pick) {
    ItemStack stack =
        new ShapeData(
                data.shapeId(), data.base(), Optional.empty(), pick ? data.disconnected() : 0, 0)
            .stack(1);
    stack.set(
        net.minecraft.core.component.DataComponents.BLOCK_STATE,
        new net.minecraft.world.item.component.BlockItemStateProperties(
            Map.of("glowing", state.getValue(GLOWING).toString())));
    return stack;
  }

  @Override
  protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
    if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
        instanceof ShapeBlockEntity be) {
      List<ItemStack> drops = new ArrayList<>();
      drops.add(shapeItem(be.data(), state, false));
      ItemStack secondary = ArchitectTool.secondaryItem(be.data());
      if (!secondary.isEmpty()) drops.add(secondary);
      return drops;
    }
    return List.of(ShapeData.defaults().stack(1));
  }

  @Override
  protected ItemStack getCloneItemStack(
      LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
    return shapeItem(data(world, pos), state, true);
  }

  @Override
  protected InteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    if (!(level.getBlockEntity(pos) instanceof ShapeBlockEntity be)
        || stack.getItem() instanceof ArchitectTool) return InteractionResult.TRY_WITH_EMPTY_HAND;
    BlockState material = null;
    if (stack.is(ArchitectureCraft.CLADDING)
        && stack.has(ArchitectureCraft.SHAPE_DATA)
        && be.data().shape().acceptsCladding()) material = ShapeData.fromStack(stack).base();
    else if (be.data().shape().window()
        && stack.getItem() instanceof BlockItem bi
        && (bi.getBlock() == Blocks.GLASS_PANE || bi.getBlock() instanceof StainedGlassPaneBlock))
      material = SawbenchMenu.materialState(stack);
    if (material == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
    if (!level.isClientSide() && be.data().secondary().isEmpty()) {
      be.setData(be.data().withSecondary(material));
      if (!player.getAbilities().instabuild) stack.shrink(1);
    }
    return InteractionResult.SUCCESS;
  }
}
