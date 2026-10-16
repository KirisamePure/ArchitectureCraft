package gcewing.architecture;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ArchitectTool extends Item {
  public enum Kind {
    CHISEL,
    HAMMER,
    GLOWBRUSH
  }

  private final Kind kind;

  public ArchitectTool(Properties properties, Kind kind) {
    super(properties);
    this.kind = kind;
  }

  /**
   * Directional chisel zones are in block-local hit coordinates, exactly as in ShapeKind.zoneHit.
   */
  public static Direction zone(boolean window, Direction face, Vec3 hit) {
    double r = window ? .375 : .25;
    if (hit.x <= -r && face != Direction.WEST) return Direction.WEST;
    if (hit.x >= r && face != Direction.EAST) return Direction.EAST;
    if (hit.y <= -r && face != Direction.DOWN) return Direction.DOWN;
    if (hit.y >= r && face != Direction.UP) return Direction.UP;
    if (hit.z <= -r && face != Direction.NORTH) return Direction.NORTH;
    if (hit.z >= r && face != Direction.SOUTH) return Direction.SOUTH;
    return null;
  }

  public static ItemStack secondaryItem(ShapeData data) {
    if (data.secondary().isEmpty()) return ItemStack.EMPTY;
    if (data.shape().window()) return new ItemStack(data.secondary().get().getBlock());
    return new ShapeData(60, data.secondary().get(), Optional.empty(), 0, 0).stack(1);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    var level = context.getLevel();
    var pos = context.getClickedPos();
    var player = context.getPlayer();
    if (player == null || !player.mayBuild() || !level.mayInteract(player, pos))
      return InteractionResult.PASS;
    if (!(level.getBlockEntity(pos) instanceof ShapeBlockEntity be)) {
      Block block = level.getBlockState(pos).getBlock();
      if (kind != Kind.CHISEL
          || (block != Blocks.GLASS
              && block != Blocks.GLASS_PANE
              && block != Blocks.GLOWSTONE
              && block != Blocks.ICE)) return InteractionResult.PASS;
      if (!level.isClientSide()) {
        level.destroyBlock(pos, false, player);
        Block.popResource(level, pos, new ItemStack(block));
      }
      return InteractionResult.SUCCESS;
    }
    if (level.isClientSide()) return InteractionResult.SUCCESS;
    BlockState state = be.getBlockState();
    ShapeData data = be.data();
    switch (kind) {
      case HAMMER -> {
        if (player.isShiftKeyDown()) {
          int mounting = state.getValue(ShapeBlock.FACING).get3DDataValue() ^ 1;
          state =
              state.setValue(
                  ShapeBlock.FACING, Direction.from3DDataValue(((mounting + 1) % 6) ^ 1));
        } else {
          if (data.offset() != 0) data = data.withOffset(-data.offset());
          if (data.offset() >= 0) state = state.cycle(ShapeBlock.TURN);
        }
        level.setBlock(pos, state, 3);
        be.setData(data);
      }
      case GLOWBRUSH -> {
        level.setBlock(pos, state.setValue(ShapeBlock.GLOWING, !player.isShiftKeyDown()), 3);
        be.setChanged();
      }
      case CHISEL -> {
        Direction face =
            zone(
                data.shape().window(),
                context.getClickedFace(),
                context.getClickLocation().subtract(pos.getCenter()));
        if (face != null) {
          int mask = data.disconnected() ^ (1 << face.get3DDataValue());
          be.setData(data.withConnections(mask));
          if (level.getBlockEntity(pos.relative(face)) instanceof ShapeBlockEntity other) {
            int bit = 1 << face.getOpposite().get3DDataValue();
            other.setData(
                other
                    .data()
                    .withConnections(
                        (mask & (1 << face.get3DDataValue())) != 0
                            ? other.data().disconnected() | bit
                            : other.data().disconnected() & ~bit));
          }
        } else if (data.secondary().isPresent()) {
          if (!player.getAbilities().instabuild) Block.popResource(level, pos, secondaryItem(data));
          be.setData(data.withSecondary(null));
        }
      }
    }
    return InteractionResult.SUCCESS;
  }
}
