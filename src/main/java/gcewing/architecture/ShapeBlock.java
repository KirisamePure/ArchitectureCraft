package gcewing.architecture;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
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
    ShapeData data = ShapeData.fromStack(context.getItemInHand());
    Direction side = Direction.UP;
    if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
      side = context.getClickedFace();
    else if (context.getClickedFace() == Direction.DOWN || data.shape().underneath)
      side = Direction.DOWN;
    int turn = context.getHorizontalDirection().get2DDataValue();
    if (data.shape().window()) side = Direction.UP;
    return defaultBlockState()
        .setValue(FACING, side)
        .setValue(TURN, turn)
        .setValue(GLOWING, data.shape().glowing());
  }

  @Override
  public void setPlacedBy(
      Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    if (level.getBlockEntity(pos) instanceof ShapeBlockEntity be)
      be.setData(ShapeData.fromStack(stack));
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    ShapeData data =
        world.getBlockEntity(pos) instanceof ShapeBlockEntity be ? be.data() : ShapeData.defaults();
    return ShapeGeometry.collision(data, state, world, pos);
  }

  @Override
  protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
    if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ShapeBlockEntity be)
      return List.of(drop(be, state));
    return List.of(ShapeData.defaults().stack(1));
  }

  private static ItemStack drop(ShapeBlockEntity be, BlockState state) {
    ItemStack stack = be.data().stack(1);
    // Persist manual glowbrush changes independently of the shape's original glow variant.
    stack.set(
        net.minecraft.core.component.DataComponents.BLOCK_STATE,
        new net.minecraft.world.item.component.BlockItemStateProperties(
            java.util.Map.of("glowing", state.getValue(GLOWING).toString())));
    return stack;
  }

  @Override
  protected ItemStack getCloneItemStack(
      LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
    return world.getBlockEntity(pos) instanceof ShapeBlockEntity be
        ? drop(be, state)
        : ShapeData.defaults().stack(1);
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
    if (stack.is(ArchitectureCraft.CLADDING) && !be.data().shape().window())
      material = ShapeData.fromStack(stack).base();
    else if (be.data().shape().window()
        && stack.getItem() instanceof BlockItem bi
        && (bi.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock
            || bi.getBlock() instanceof net.minecraft.world.level.block.TransparentBlock))
      material = bi.getBlock().defaultBlockState();
    if (material == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
    if (!level.isClientSide() && be.data().secondary().isEmpty()) {
      be.setData(be.data().withSecondary(material));
      if (!player.getAbilities().instabuild) stack.shrink(1);
    }
    return InteractionResult.SUCCESS;
  }
}
